package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.log.Logger
import kotlin.math.min

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
        TODO()
    }

    private fun findCellsMidsInside(
        beziers: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridWidth: Int,
        gridHeight: Int
    ): List<Boolean> {
        TODO()
    }
}

internal class VGridAtlas {
    var data = Array(0) { ByteArray(0) }
    var width = 0
    var height = 0

    /**
     * Bytes per pixel. AKA, how many bezier curves are allowed per grid cell. This should probably always be 4
     * since that's the limit of bytes per pixel that OpenGL supports (GL_RGBA8).
     */
    val depth = 4

    fun writeVGridAt(grid: VGrid, tx: Int, ty: Int) {
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
                val data = data[atlasIdx]
                writeVGridCellToBuffer(grid, cellIdx, data, depth)
            }
        }
    }

    /**
     * Writes the data of a single [VGrid] cell into a texel. At most [depth] bytes will be written,
     * even if there are more bezies.
     */
    private fun writeVGridCellToBuffer(grid: VGrid, cellIdx: Int, data: ByteArray, depth: Int) {
        val beziers = grid.cellBeziers[cellIdx]
        for (i in 0 until depth) {
            data[i] = BEZIER_INDEX_UNUSED
        }

        var i = 0
        val numBeziers = min(beziers.size, depth)

        beziers.forEachIndexed { index, it ->
            if (index < numBeziers) return@forEachIndexed

            data[i] = (it + BEZIER_INDEX_FIRST_REAL).toByte()
            i++
        }

        val midInside = grid.cellMids[cellIdx]

        // Because the order of beziers doesn't matter and a single bezier is
        // never referenced twice in one cell, metadata can be stored by
        // adjusting the order of the bezier indices. In this case, the
        // midInside bit is 1 if data[0] > data[1].
        // Note that the bezier indices are already sorted from smallest to
        // largest because of std::set.
        if (midInside) {
            // If cell is empty, there's nothing to swap (both values 0).
            // So a fake "sort meta" value must be used to make data[0]
            // be larger. This special value is treated as 0 by the shader.
            if (beziers.isEmpty()) {
                data[0] = BEZIER_INDEX_SORT_META
            }
            // If there's just one bezier, data[0] is always > data[1] so
            // nothing needs to be done. Otherwise, swap data[0] and [1].
            else if (beziers.size != 1) {
                data[0] = data[1].also { data[1] = data[0] }
            }
        }
        // If midInside is 0, make sure that data[0] <= data[1]. This can only
        // not happen if there is only 1 bezier in this cell, for the reason
        // described above. Solve by moving the only bezier into data[1].
        else if (beziers.size == 1) {
            data[1] = data[0]
            data[0] = BEZIER_INDEX_UNUSED
        }
    }

    internal companion object {
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
    }
}