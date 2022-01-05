package com.lehaine.littlekt.graphics.font.internal

import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.graphics.font.internal.VGridAtlas.writeVGridCellToBuffer
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.clamp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Represents a grid that is "overlayed" on top of a glyph that stores properties of each grid cell.
 * The grid's origin is the bottom-left and is stored in row-major order.
 * @author Colton Daily
 * @date 12/10/2021
 */
internal data class VGrid(
    val beziers: List<Bezier>,
    val glyphWidth: Int,
    val glyphHeight: Int,
    val width: Int,
    val height: Int
) {
    /**
     * For each cell, a set of bezier curves (indices referring to the input bezier array) that passes through that cell.
     */
    val cellBeziers = findCellsIntersections(beziers, glyphWidth, glyphHeight, width, height)

    /**
     * For each cell, a boolean indicating whether the cell's midpoint is inside the glyph (true) or outside (false).
     */
    val cellMids = findCellsMidsInside(beziers, glyphWidth, glyphHeight, width, height)

    private fun findCellsIntersections(
        beziers: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridWidth: Int,
        gridHeight: Int
    ): List<Set<Int>> {
        val cellBeziers = MutableList(gridWidth * gridHeight) { mutableSetOf<Int>() }

        fun setGrid(x: Float, y: Float, bezierIndex: Int) {
            val tx = floor(x).toInt().clamp(0, gridWidth - 1)
            val ty = floor(y).toInt().clamp(0, gridHeight - 1)
            cellBeziers[ty * gridWidth + tx] += bezierIndex
        }

        beziers.forEachIndexed { i, bezier ->
            var anyIntersections = false

            // Every vertical grid line including edges
            for (x in 0..gridWidth) {
                val intY = FloatArray(2) { 0f }
                val numInt = bezier.intersectVertical(x.toFloat() * glyphWidth / gridWidth, intY)
                for (j in 0 until numInt) {
                    val y = intY[j] * gridHeight / glyphHeight
                    setGrid(x.toFloat(), y, i) // right
                    setGrid(x - 1f, y, i) // left
                    anyIntersections = true
                }
            }

            for (y in 0..gridHeight) {
                val intX = FloatArray(2) { 0f }
                val numInt = bezier.intersectHorizontal(y.toFloat() * glyphHeight / gridHeight, intX)
                for (j in 0 until numInt) {
                    val x = intX[j] * gridWidth / glyphWidth
                    setGrid(x, y.toFloat(), i) // up
                    setGrid(x, y - 1f, i) // down
                    anyIntersections = true
                }
            }

            // If no grid line intersections, bezier is fully contained in
            // one cell. Mark this bezier as intersecting that cell.
            if (!anyIntersections) {
                val x = bezier.p0.x * gridWidth / glyphWidth
                val y = bezier.p0.y * gridHeight / glyphHeight
                setGrid(x, y, i)
            }
        }
        return cellBeziers
    }

    private fun findCellsMidsInside(
        beziers: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridWidth: Int,
        gridHeight: Int
    ): List<Boolean> {
        val cellMids = MutableList(gridWidth * gridHeight) { false }

        // Find whether the center of each cell is inside the glyph
        for (y in 0 until gridHeight) {
            // Find all intersections with cells horizontal midpoint line
            // and store them sorted from left to right
            val intersections = mutableSetOf<Float>()
            val yMid = y + 0.5f
            beziers.forEachIndexed { i, bezier ->
                val intX = FloatArray(2) { 0f }
                val numInt = bezier.intersectHorizontal(yMid * glyphHeight / gridHeight, intX)
                for (j in 0 until numInt) {
                    val x = intX[j] * gridWidth / glyphWidth
                    intersections += x
                }
            }

            // Traverse intersections (whole grid row, left to right).
            // Every 2nd crossing represents exiting an "inside" region.
            // All properly formed glyphs should have an even number of
            // crossings.
            var outside = false
            var start = 0f

            intersections.sortedBy { it }.forEach {
                val end = it
                if (outside) {
                    val startCell = start.roundToInt().clamp(0, gridWidth)
                    val endCell = end.roundToInt().clamp(0, gridWidth)
                    for (x in startCell until endCell) {
                        cellMids[y * gridWidth + x] = true
                    }
                }
                outside = !outside
                start = end
            }
        }
        return cellMids
    }
}

internal object VGridAtlas {

    private val logger = Logger<VGridAtlas>()

    // Converts X,Y to index in a row-major 2D array
    private fun xy2i(x: Int, y: Int, w: Int) = (y * w) + x

    /**
     * Each bezier index is represented as one byte in the grid cell,
     * and values 0 and 1 are reserved for special meaning.
     * This leaves a limit of 254 beziers per grid/glyph.
     * More on the meaning of values 1 and 0 in the VGridAtlas struct
     * definition and in [writeVGridCellToBuffer].
     */
    private const val BEZIER_INDEX_UNUSED = 0.toByte()
    private const val BEZIER_INDEX_SORT_META = 1.toByte()
    private const val BEZIER_INDEX_FIRST_REAL = 2.toByte()

    fun writeVGridAt(
        grid: VGrid,
        data: ByteBuffer,
        tx: Int,
        ty: Int,
        width: Int,
        height: Int,
        /**
         * Bytes per pixel. AKA, how many bezier curves are allowed per grid cell. This should probably always be 4
         * since that's the limit of bytes per pixel that OpenGL supports (GL_RGBA8).
         */
        depth: Int,
    ) {
        check(tx + grid.width <= width) { "VGrid to wide to fit on atlas" }
        check(ty + grid.height <= height) { "VGrid to long to fit on atlas" }

        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val cellIdx = xy2i(x, y, grid.width)
                val atlasIdx = xy2i(tx + x, ty + y, width) * depth

                val beziers = grid.cellBeziers[cellIdx]
                if (beziers.size > depth) {
                    logger.error { "Too many beziers in one grid cell (max: $depth, need: ${beziers.size}, x: $x, y: $y)" }
                }
                writeVGridCellToBuffer(grid, cellIdx, data, atlasIdx, depth)
            }
        }
    }

    /**
     * Writes the data of a single [VGrid] cell into a texel. At most [depth] bytes will be written,
     * even if there are more bezies.
     */
    private fun writeVGridCellToBuffer(grid: VGrid, cellIdx: Int, data: ByteBuffer, offset: Int, depth: Int) {
        val beziers = grid.cellBeziers[cellIdx].sorted()
        for (i in 0 until depth) {
            data[offset + i] = BEZIER_INDEX_UNUSED
        }

        val numBeziers = min(beziers.size, depth)
        for(i in 0 until numBeziers) {
            val it = beziers[i]
            data[offset + i] = (it + BEZIER_INDEX_FIRST_REAL).toByte()
        }

        val midInside = grid.cellMids[cellIdx]

        // Because the order of beziers doesn't matter and a single bezier is
        // never referenced twice in one cell, metadata can be stored by
        // adjusting the order of the bezier indices. In this case, the
        // midInside bit is 1 if data[0] > data[1].
        // Note: that bezier indices are already sorted in ascending order.
        if (midInside) {
            // If cell is empty, there's nothing to swap (both values 0).
            // So a fake "sort meta" value must be used to make data[0]
            // be larger. This special value is treated as 0 by the shader.
            if (beziers.isEmpty()) {
                data[offset] = BEZIER_INDEX_SORT_META
            }
            // If there's just one bezier, data[0] is always > data[1] so
            // nothing needs to be done. Otherwise, swap data[0] and [1].
            else if (beziers.size != 1) {
                data[offset] = data.getByte(offset + 1).also { data[offset + 1] = data.getByte(offset) }
            }
        }
        // If midInside is 0, make sure that data[0] <= data[1]. This can only
        // not happen if there is only 1 bezier in this cell, for the reason
        // described above. Solve by moving the only bezier into data[1].
        else if (beziers.size == 1) {
            data[offset + 1] = data.getByte(offset)
            data[offset] = BEZIER_INDEX_UNUSED
        }
    }
}