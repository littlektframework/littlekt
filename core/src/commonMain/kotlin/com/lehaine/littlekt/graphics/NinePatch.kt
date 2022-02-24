package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.max
import kotlin.math.min

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
        texture.slice(), left, right, top, bottom
    )

    private val patches = arrayOfNulls<TextureSlice?>(9)
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
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        srcX: Float = 0f,
        srcY: Float = 0f,
        srcWidth: Float = width,
        srcHeight: Float = height
    ) {
        prepareVertices(x, y, width, height, color, srcX, srcY, srcWidth, srcHeight)
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

    private fun prepareVertices(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        srcX: Float,
        srcY: Float,
        srcWidth: Float,
        srcHeight: Float
    ) {
        val centerX = x + max(leftWidth, srcX)
        val centerY = y + max(bottomHeight, srcY)
        val centerWidth = max(width - rightWidth - max(leftWidth, srcX), 0f)
        val centerHeight = height - topHeight - bottomHeight
        val rightX = x + width - rightWidth
        val topY = y + height - topHeight
        val colorBits = color.toFloatBits()
        if (bottomLeft != -1) {
            patches[BOTTOM_LEFT]?.let {
                set(
                    idx = bottomLeft,
                    x = x + srcX,
                    y = y,
                    width = max(it.width - srcX, 0f),
                    height = bottomHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat(),
                    srcWidth = max(it.width - srcX, 0f),
                    srcHeight = it.height.toFloat()
                )
            }
        }
        if (bottomCenter != -1) {
            patches[BOTTOM_CENTER]?.let {
                set(
                    idx = bottomCenter,
                    x = centerX,
                    y = y,
                    width = centerWidth,
                    height = bottomHeight,
                    color = colorBits,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat(),
                    srcWidth = it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                    stretchW = patches[BOTTOM_LEFT] != null || patches[BOTTOM_RIGHT] != null,
                )
            }
        }
        if (bottomRight != -1) {
            patches[BOTTOM_RIGHT]?.let {
                set(
                    idx = bottomRight,
                    x = rightX + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    y = y,
                    width = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    height = bottomHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    srcY = it.y.toFloat(),
                    srcWidth = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    srcHeight = it.height.toFloat()
                )
            }
        }
        if (middleLeft != -1) {
            patches[MIDDLE_LEFT]?.let {
                set(
                    idx = middleLeft,
                    x = x + srcX,
                    y = centerY,
                    width = max(it.width - srcX, 0f),
                    height = centerHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat(),
                    srcWidth = max(it.width - srcX, 0f),
                    srcHeight = it.height.toFloat(),
                    stretchW = false,
                    stretchH = patches[TOP_LEFT] != null || patches[BOTTOM_LEFT] != null
                )
            }
        }
        if (middleCenter != -1) {
            patches[MIDDLE_CENTER]?.let {
                set(
                    idx = middleCenter,
                    x = centerX,
                    y = centerY,
                    width = centerWidth,
                    height = centerHeight,
                    color = colorBits,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat(),
                    srcWidth = it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                    stretchW = patches[MIDDLE_LEFT] != null || patches[MIDDLE_RIGHT] != null,
                    stretchH = patches[TOP_CENTER] != null || patches[BOTTOM_CENTER] != null
                )
            }
        }
        if (middleRight != -1) {
            patches[MIDDLE_RIGHT]?.let {
                set(
                    idx = middleRight,
                    x = rightX + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    y = centerY,
                    width = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    height = centerHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    srcY = it.y.toFloat(),
                    srcWidth = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                    stretchW = false,
                    stretchH = patches[TOP_RIGHT] != null || patches[BOTTOM_RIGHT] != null
                )
            }
        }
        if (topLeft != -1) {
            patches[TOP_LEFT]?.let {
                set(
                    idx = topLeft,
                    x = x + srcX,
                    y = topY,
                    width = max(it.width - srcX, 0f),
                    height = topHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat(),
                    srcWidth = max(it.width - srcX, 0f),
                    srcHeight = it.height.toFloat()
                )
            }
        }
        if (topCenter != -1) {
            patches[TOP_CENTER]?.let {
                set(
                    idx = topCenter,
                    x = centerX,
                    y = topY,
                    width = centerWidth,
                    height = topHeight,
                    color = colorBits,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat(),
                    srcWidth = it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                    stretchW = patches[TOP_LEFT] != null || patches[TOP_RIGHT] != null,
                )
            }
        }
        if (topRight != -1) {
            patches[TOP_RIGHT]?.let {
                set(
                    idx = topRight,
                    x = rightX + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    y = topY,
                    width = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    height = topHeight,
                    color = colorBits,
                    srcX = it.x.toFloat() + if (width - srcX <= it.width) it.width - (width - srcX) else 0f,
                    srcY = it.y.toFloat(),
                    srcWidth = if (width - srcX <= it.width) width - srcX else it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                )
            }
        }
    }


    private fun set(
        idx: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Float,
        srcX: Float,
        srcY: Float,
        srcWidth: Float,
        srcHeight: Float,
        stretchW: Boolean = false,
        stretchH: Boolean = false
    ) {
        val fx2 = x + width
        val fy2 = y + height
        val invTexWidth = 1f / slice.texture.width
        val invTexHeight = 1f / slice.texture.height
        var u = srcX * invTexWidth
        var v = srcY * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = (srcY + srcHeight) * invTexHeight

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

        vertices[idx] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = color
        vertices[idx + 3] = u
        vertices[idx + 4] = v

        vertices[idx + 5] = x
        vertices[idx + 6] = fy2
        vertices[idx + 7] = color
        vertices[idx + 8] = u
        vertices[idx + 9] = v2

        vertices[idx + 10] = fx2
        vertices[idx + 11] = fy2
        vertices[idx + 12] = color
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2

        vertices[idx + 15] = fx2
        vertices[idx + 16] = y
        vertices[idx + 17] = color
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
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