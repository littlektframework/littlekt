package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.max

/**
 * Creates a ninepatch by slicing up the [TextureSlice] into nine patches which produces clean panels of any size,
 * based on a small texture by splitting it into a 3x3 grid. The drawn, the texture tiles the textures sides horizontally
 * or vertically and the center on both axes but doesn't scale or tile the corners.
 * @param slice the slice to convert into a nine patch
 * @param left amount of pixels from the left edge
 * @param right amount of pixels from the right edge
 * @param bottom amount of pixels from the top edge
 * @param top amount of pixels from the bottom edge
 * @author Colton Daily
 * @date 1/18/2022
 */
class NinePatch(private val slice: TextureSlice, val left: Int, val right: Int, val bottom: Int, val top: Int) {
    constructor(texture: Texture, left: Int, right: Int, top: Int, bottom: Int) : this(
        texture.slice(),
        left,
        right,
        top,
        bottom
    )

    private val vertices = FloatArrayList(9 * 4 * 5)
    private var bottomLeft: Int = 0
    private var bottomCenter: Int = 0
    private var bottomRight: Int = 0
    private var middleLeft: Int = 0
    private var middleCenter: Int = 0
    private var middleRight: Int = 0
    private var topLeft: Int = 0
    private var topCenter: Int = 0
    private var topRight: Int = 0
    private var leftWidth: Float = 0f
    private var rightWidth: Float = 0f
    private var middleWidth: Float = 0f
    private var middleHeight: Float = 0f
    private var topHeight: Float = 0f
    private var bottomHeight: Float = 0f

    private var idx = 0

    init {
        val middleWidth = slice.width - left - right
        val middleHeight = slice.height - bottom - top
        val patches = arrayOfNulls<TextureSlice?>(9)

        if (bottom > 0) {
            if (left > 0) patches[BOTTOM_LEFT] = TextureSlice(slice, 0, 0, left, bottom)
            if (middleWidth > 0) patches[BOTTOM_CENTER] = TextureSlice(slice, left, 0, middleWidth, bottom)
            if (right > 0) patches[BOTTOM_RIGHT] = TextureSlice(slice, left + middleWidth, 0, right, bottom)
        }

        if (middleHeight > 0) {
            if (left > 0) patches[MIDDLE_LEFT] = TextureSlice(slice, 0, bottom, left, middleHeight)
            if (middleWidth > 0) patches[MIDDLE_CENTER] = TextureSlice(slice, left, bottom, middleWidth, middleHeight)
            if (right > 0) patches[MIDDLE_RIGHT] = TextureSlice(slice, left + middleWidth, bottom, right, middleHeight)
        }

        if (top > 0) {
            if (left > 0) patches[TOP_LEFT] = TextureSlice(slice, 0, bottom + middleHeight, left, top)
            if (middleWidth > 0) patches[TOP_CENTER] =
                TextureSlice(slice, left, bottom + middleHeight, middleWidth, top)
            if (right > 0) patches[TOP_RIGHT] =
                TextureSlice(slice, left + middleWidth, bottom + middleHeight, right, top)
        }

        // if split only vertically, move splits from right to center
        if (left == 0 && middleWidth == 0) {
            patches[TOP_CENTER] = patches[TOP_RIGHT]
            patches[MIDDLE_CENTER] = patches[MIDDLE_RIGHT]
            patches[BOTTOM_CENTER] = patches[BOTTOM_RIGHT]
            patches[TOP_RIGHT] = null
            patches[MIDDLE_RIGHT] = null
            patches[BOTTOM_RIGHT] = null
        }
        // if split only horizontally, move splits from bottom to center
        if (bottom == 0 && middleHeight == 0) {
            patches[MIDDLE_LEFT] = patches[BOTTOM_LEFT]
            patches[MIDDLE_CENTER] = patches[BOTTOM_CENTER]
            patches[MIDDLE_RIGHT] = patches[BOTTOM_RIGHT]
            patches[BOTTOM_LEFT] = null
            patches[BOTTOM_CENTER] = null
            patches[BOTTOM_RIGHT] = null
        }

        load(patches)
    }

    fun draw(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO
    ) {
        prepareVertices(x, y, width, height)
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        if (rotation != Angle.ZERO) {
            for (i in 0 until idx step 5) {
                val vx = (vertices[i] - worldOriginX) * scaleX
                val vy = (vertices[i + 1] - worldOriginY) * scaleY
                val cos = rotation.cosine
                val sin = rotation.sine
                vertices[i] = cos * vx - sin * vy + worldOriginX
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY
            }
        } else if (scaleX != 1f || scaleY != 1f) {
            for (i in 0 until idx step 5) {
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY
            }
        }

        batch.draw(slice.texture, vertices.data, count = idx)
    }

    private fun load(patches: Array<TextureSlice?>) {
        patches[BOTTOM_LEFT]?.let {
            bottomLeft = add(it, stretchW = false, stretchH = false)
            leftWidth = it.width.toFloat()
            bottomHeight = it.height.toFloat()
        } ?: run { bottomLeft = -1 }

        patches[BOTTOM_CENTER]?.let {
            bottomCenter = add(it, patches[BOTTOM_LEFT] != null || patches[BOTTOM_RIGHT] != null, false)
            middleWidth = max(middleWidth, it.width.toFloat())
            bottomHeight = max(bottomHeight, it.height.toFloat())
        } ?: run { bottomCenter = -1 }

        patches[BOTTOM_RIGHT]?.let {
            bottomRight = add(it, stretchW = false, stretchH = false)
            rightWidth = max(rightWidth, it.width.toFloat())
            bottomHeight = max(bottomHeight, it.height.toFloat())
        } ?: run { bottomRight = -1 }

        patches[MIDDLE_LEFT]?.let {
            middleLeft = add(it, false, patches[TOP_LEFT] != null || patches[BOTTOM_LEFT] != null)
            leftWidth = max(leftWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleLeft = -1 }

        patches[MIDDLE_CENTER]?.let {
            middleCenter = add(
                it,
                patches[MIDDLE_LEFT] != null || patches[MIDDLE_RIGHT] != null,
                patches[TOP_CENTER] != null || patches[BOTTOM_CENTER] != null
            )
            middleWidth = max(middleWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleCenter = -1 }

        patches[MIDDLE_RIGHT]?.let {
            middleRight = add(it, false, patches[TOP_RIGHT] != null || patches[BOTTOM_RIGHT] != null)
            rightWidth = max(rightWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleRight = -1 }

        patches[TOP_LEFT]?.let {
            topLeft = add(it, stretchW = false, stretchH = false)
            leftWidth = max(leftWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topLeft = -1 }

        patches[TOP_CENTER]?.let {
            topCenter = add(it, patches[TOP_LEFT] != null || patches[TOP_RIGHT] != null, false)
            middleWidth = max(middleWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topCenter = -1 }

        patches[TOP_RIGHT]?.let {
            topRight = add(it, stretchW = false, stretchH = false)
            rightWidth = max(rightWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topRight = -1 }
    }

    private fun add(slice: TextureSlice, stretchW: Boolean, stretchH: Boolean): Int {
        var u = slice.u
        var v = slice.v2
        var u2 = slice.u2
        var v2 = slice.v

        // Add half pixel offsets on stretchable dimensions to avoid color bleeding when GL_LINEAR
        // filtering is used for the texture. This nudges the texture coordinate to the center
        // of the texel where the neighboring pixel has 0% contribution in linear blending mode.
        if (slice.texture.magFilter == TexMagFilter.LINEAR || slice.texture.minFilter == TexMinFilter.LINEAR) {
            if (stretchW) {
                val halfTexelWidth = 0.5f * 1f / slice.texture.width
                u += halfTexelWidth
                u2 -= halfTexelWidth
            }
            if (stretchH) {
                val halfTexelHeight = 0.5f * 1f / slice.texture.height
                v -= halfTexelHeight
                v2 += halfTexelHeight
            }
        }

        val startIdx = idx
        vertices.let {
            it[idx + 3] = u
            it[idx + 4] = v
            it[idx + 8] = u
            it[idx + 9] = v2
            it[idx + 13] = u2
            it[idx + 14] = v2
            it[idx + 18] = u2
            it[idx + 19] = v
        }
        idx += 20
        return startIdx
    }

    private fun prepareVertices(x: Float, y: Float, width: Float, height: Float) {
        val centerX = x + leftWidth
        val centerY = y + bottomHeight
        val centerWidth = width - rightWidth - leftWidth
        val centerHeight = height - topHeight - bottomHeight
        val rightX = x + width - rightWidth
        val topY = y + height - topHeight
        val colorBits = Color.WHITE.toFloatBits() // TODO impl color
        if (bottomLeft != -1) set(bottomLeft, x, y, leftWidth, bottomHeight, colorBits)
        if (bottomCenter != -1) set(bottomCenter, centerX, y, centerWidth, bottomHeight, colorBits)
        if (bottomRight != -1) set(bottomRight, rightX, y, rightWidth, bottomHeight, colorBits)
        if (middleLeft != -1) set(middleLeft, x, centerY, leftWidth, centerHeight, colorBits)
        if (middleCenter != -1) set(middleCenter, centerX, centerY, centerWidth, centerHeight, colorBits)
        if (middleRight != -1) set(middleRight, rightX, centerY, rightWidth, centerHeight, colorBits)
        if (topLeft != -1) set(topLeft, x, topY, leftWidth, topHeight, colorBits)
        if (topCenter != -1) set(topCenter, centerX, topY, centerWidth, topHeight, colorBits)
        if (topRight != -1) set(topRight, rightX, topY, rightWidth, topHeight, colorBits)
    }

    private fun set(idx: Int, x: Float, y: Float, width: Float, height: Float, color: Float) {
        val fx2 = x + width
        val fy2 = y + height
        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color

        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color

        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color

        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
    }

    companion object {
        private const val TOP_LEFT = 0
        private const val TOP_CENTER = 1
        private const val TOP_RIGHT = 2
        private const val MIDDLE_LEFT = 3
        private const val MIDDLE_CENTER = 4
        private const val MIDDLE_RIGHT = 5
        private const val BOTTOM_LEFT = 6
        private const val BOTTOM_CENTER = 7
        private const val BOTTOM_RIGHT = 8
    }
}