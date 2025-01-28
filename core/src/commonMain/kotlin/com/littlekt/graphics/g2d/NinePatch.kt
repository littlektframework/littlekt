package com.littlekt.graphics.g2d

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.slice
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.sine
import com.littlekt.util.datastructure.FloatArrayList
import io.ygdrasil.webgpu.FilterMode
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a ninepatch by slicing up the [TextureSlice] into nine patches which produces clean
 * panels of any size, based on a small texture by splitting it into a 3x3 grid. The drawn, the
 * texture tiles the textures sides horizontally or vertically and the center on both axes but
 * doesn't scale or tile the corners.
 *
 * @param slice the slice to convert into a nine patch
 * @param left amount of pixels from the left edge
 * @param right amount of pixels from the right edge
 * @param top amount of pixels from the top edge
 * @param bottom amount of pixels from the bottom edge
 * @author Colton Daily
 * @date 1/18/2022
 */
class NinePatch(
    private val slice: TextureSlice,
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
) {
    constructor(
        texture: Texture,
        left: Int,
        right: Int,
        top: Int,
        bottom: Int
    ) : this(texture.slice(), left, right, top, bottom)

    val totalHeight: Float
        get() = topHeight + middleHeight + bottomHeight

    val totalWidth: Float
        get() = leftWidth + middleWidth + rightWidth

    private val patches = arrayOfNulls<TextureSlice?>(9)
    private val vertices =
        FloatArrayList(9 * 4 * 8) // 9 patches * 4 points of a quad * 8 vertex size
    private var topLeft: Int = 0
    private var topCenter: Int = 0
    private var topRight: Int = 0
    private var middleLeft: Int = 0
    private var middleCenter: Int = 0
    private var middleRight: Int = 0
    private var bottomLeft: Int = 0
    private var bottomCenter: Int = 0
    private var bottomRight: Int = 0
    private var leftWidth: Float = 0f
    private var rightWidth: Float = 0f
    private var middleWidth: Float = 0f
    private var middleHeight: Float = 0f
    private var bottomHeight: Float = 0f
    private var topHeight: Float = 0f

    private var idx = 0

    init {
        val middleWidth = slice.width - left - right
        val middleHeight = slice.height - top - bottom

        if (top > 0) {
            if (left > 0) patches[TOP_LEFT] = TextureSlice(slice, 0, 0, left, top)
            if (middleWidth > 0)
                patches[TOP_CENTER] = TextureSlice(slice, left, 0, middleWidth, top)
            if (right > 0)
                patches[TOP_RIGHT] = TextureSlice(slice, left + middleWidth, 0, right, top)
        }

        if (middleHeight > 0) {
            if (left > 0) patches[MIDDLE_LEFT] = TextureSlice(slice, 0, top, left, middleHeight)
            if (middleWidth > 0)
                patches[MIDDLE_CENTER] = TextureSlice(slice, left, top, middleWidth, middleHeight)
            if (right > 0)
                patches[MIDDLE_RIGHT] =
                    TextureSlice(slice, left + middleWidth, top, right, middleHeight)
        }

        if (bottom > 0) {
            if (left > 0)
                patches[BOTTOM_LEFT] = TextureSlice(slice, 0, top + middleHeight, left, bottom)
            if (middleWidth > 0)
                patches[BOTTOM_CENTER] =
                    TextureSlice(slice, left, top + middleHeight, middleWidth, bottom)
            if (right > 0)
                patches[BOTTOM_RIGHT] =
                    TextureSlice(slice, left + middleWidth, top + middleHeight, right, bottom)
        }

        // if split only vertically, move splits from right to center
        if (left == 0 && middleWidth == 0) {
            patches[BOTTOM_CENTER] = patches[BOTTOM_RIGHT]
            patches[MIDDLE_CENTER] = patches[MIDDLE_RIGHT]
            patches[TOP_CENTER] = patches[TOP_RIGHT]
            patches[BOTTOM_RIGHT] = null
            patches[MIDDLE_RIGHT] = null
            patches[TOP_RIGHT] = null
        }
        // if split only horizontally, move splits from bottom to center
        if (top == 0 && middleHeight == 0) {
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
        srcHeight: Float = height,
    ) {
        check(srcX >= 0f) { "srcX must be >= 0!" }
        check(srcY >= 0f) { "srcY must be >= 0!" }
        check(srcWidth <= width) { "srcWidth must be <= width!" }
        check(srcHeight <= height) { "srcHeight must be <= height!" }

        prepareVertices(x, y, width, height, color, srcX, srcY, srcWidth, srcHeight)
        val worldOriginX = x + originX
        val worldOriginY = y + originY
        if (rotation != Angle.ZERO) {
            for (i in 0 until idx step 9) {
                val vx = (vertices[i] - worldOriginX) * scaleX
                val vy = (vertices[i + 1] - worldOriginY) * scaleY
                val cos = rotation.cosine
                val sin = rotation.sine
                vertices[i] = cos * vx - sin * vy + worldOriginX
                vertices[i + 1] = sin * vx + cos * vy + worldOriginY
            }
        } else if (scaleX != 1f || scaleY != 1f) {
            for (i in 0 until idx step 9) {
                vertices[i] = (vertices[i] - worldOriginX) * scaleX + worldOriginX
                vertices[i + 1] = (vertices[i + 1] - worldOriginY) * scaleY + worldOriginY
            }
        }

        batch.draw(slice.texture, vertices.data, count = idx)
    }

    private fun load(patches: Array<TextureSlice?>) {
        patches[TOP_LEFT]?.let {
            topLeft = add()
            leftWidth = max(leftWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topLeft = -1 }

        patches[TOP_CENTER]?.let {
            topCenter = add()
            middleWidth = max(middleWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topCenter = -1 }

        patches[TOP_RIGHT]?.let {
            topRight = add()
            rightWidth = max(rightWidth, it.width.toFloat())
            topHeight = max(topHeight, it.height.toFloat())
        } ?: run { topRight = -1 }

        patches[MIDDLE_LEFT]?.let {
            middleLeft = add()
            leftWidth = max(leftWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleLeft = -1 }

        patches[MIDDLE_CENTER]?.let {
            middleCenter = add()
            middleWidth = max(middleWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleCenter = -1 }

        patches[MIDDLE_RIGHT]?.let {
            middleRight = add()
            rightWidth = max(rightWidth, it.width.toFloat())
            middleHeight = max(middleHeight, it.height.toFloat())
        } ?: run { middleRight = -1 }

        patches[BOTTOM_LEFT]?.let {
            bottomLeft = add()
            leftWidth = it.width.toFloat()
            bottomHeight = it.height.toFloat()
        } ?: run { bottomLeft = -1 }

        patches[BOTTOM_CENTER]?.let {
            bottomCenter = add()
            middleWidth = max(middleWidth, it.width.toFloat())
            bottomHeight = max(bottomHeight, it.height.toFloat())
        } ?: run { bottomCenter = -1 }

        patches[BOTTOM_RIGHT]?.let {
            bottomRight = add()
            rightWidth = max(rightWidth, it.width.toFloat())
            bottomHeight = max(bottomHeight, it.height.toFloat())
        } ?: run { bottomRight = -1 }
    }

    private fun add(): Int {
        val startIdx = idx
        idx += 36
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
        srcHeight: Float,
    ) {
        val centerX = x + max(leftWidth, srcX)
        val centerY = y + max(bottomHeight, srcY)
        val centerWidth = max(width - max(width - srcWidth, rightWidth) - max(leftWidth, srcX), 0f)
        val centerHeight =
            max(height - max(height - srcHeight, topHeight) - max(bottomHeight, srcY), 0f)

        val leftWidthOffset = min(max(leftWidth - srcX, 0f), min(leftWidth, srcWidth))

        val bottomHeightOffset = min(max(bottomHeight - srcY, 0f), min(bottomHeight, srcHeight))

        val rightX = x + width - rightWidth
        val rightXOffset = min(rightWidth, max(rightWidth - (width - srcX), 0f))
        val rightWidthOffset =
            min(min(width - srcX, rightWidth), max(0f, rightWidth - (width - srcWidth)))

        val topY = y + height - topHeight
        val topYOffset = min(topHeight, max(topHeight - (height - srcY), 0f))
        val topHeightOffset =
            min(min(height - srcY, topHeight), max(0f, topHeight - (height - srcHeight)))

        if (topLeft != -1) {
            patches[TOP_LEFT]?.let {
                set(
                    idx = topLeft,
                    x = x + srcX,
                    y = topY + topYOffset,
                    width = leftWidthOffset,
                    height = topHeightOffset,
                    color = color,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat() + topYOffset,
                    srcWidth = leftWidthOffset,
                    srcHeight = topHeightOffset
                )
            }
        }
        if (topCenter != -1) {
            patches[TOP_CENTER]?.let {
                set(
                    idx = topCenter,
                    x = centerX,
                    y = topY + topYOffset,
                    width = centerWidth,
                    height = topHeightOffset,
                    color = color,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat() + topYOffset,
                    srcWidth = it.width.toFloat(),
                    srcHeight = topHeightOffset,
                    stretchW = patches[TOP_LEFT] != null || patches[TOP_RIGHT] != null,
                )
            }
        }
        if (topRight != -1) {
            patches[TOP_RIGHT]?.let {
                set(
                    idx = topRight,
                    x = rightX + rightXOffset,
                    y = topY + topYOffset,
                    width = rightWidthOffset,
                    height = topHeightOffset,
                    color = color,
                    srcX = it.x.toFloat() + rightXOffset,
                    srcY = it.y.toFloat() + topYOffset,
                    srcWidth = rightWidthOffset,
                    srcHeight = topHeightOffset
                )
            }
        }
        if (middleLeft != -1) {
            patches[MIDDLE_LEFT]?.let {
                set(
                    idx = middleLeft,
                    x = x + srcX,
                    y = centerY,
                    width = leftWidthOffset,
                    height = centerHeight,
                    color = color,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat(),
                    srcWidth = leftWidthOffset,
                    srcHeight = it.height.toFloat(),
                    stretchW = false,
                    stretchH = patches[BOTTOM_LEFT] != null || patches[TOP_LEFT] != null
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
                    color = color,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat(),
                    srcWidth = it.width.toFloat(),
                    srcHeight = it.height.toFloat(),
                    stretchW = patches[MIDDLE_LEFT] != null || patches[MIDDLE_RIGHT] != null,
                    stretchH = patches[BOTTOM_CENTER] != null || patches[TOP_CENTER] != null
                )
            }
        }
        if (middleRight != -1) {
            patches[MIDDLE_RIGHT]?.let {
                set(
                    idx = middleRight,
                    x = rightX + rightXOffset,
                    y = centerY,
                    width = rightWidthOffset,
                    height = centerHeight,
                    color = color,
                    srcX = it.x.toFloat() + rightXOffset,
                    srcY = it.y.toFloat(),
                    srcWidth = rightWidthOffset,
                    srcHeight = it.height.toFloat(),
                    stretchW = false,
                    stretchH = patches[BOTTOM_RIGHT] != null || patches[TOP_RIGHT] != null
                )
            }
        }
        if (bottomLeft != -1) {
            patches[BOTTOM_LEFT]?.let {
                set(
                    idx = bottomLeft,
                    x = x + srcX,
                    y = y + srcY,
                    width = leftWidthOffset,
                    height = bottomHeightOffset,
                    color = color,
                    srcX = it.x.toFloat() + min(srcX, it.width.toFloat()),
                    srcY = it.y.toFloat() + min(srcY, it.height.toFloat()),
                    srcWidth = leftWidthOffset,
                    srcHeight = bottomHeightOffset
                )
            }
        }
        if (bottomCenter != -1) {
            patches[BOTTOM_CENTER]?.let {
                set(
                    idx = bottomCenter,
                    x = centerX,
                    y = y + srcY,
                    width = centerWidth,
                    height = bottomHeightOffset,
                    color = color,
                    srcX = it.x.toFloat(),
                    srcY = it.y.toFloat() + min(srcY, it.height.toFloat()),
                    srcWidth = it.width.toFloat(),
                    srcHeight = bottomHeightOffset,
                    stretchW = patches[BOTTOM_LEFT] != null || patches[BOTTOM_RIGHT] != null,
                )
            }
        }
        if (bottomRight != -1) {
            patches[BOTTOM_RIGHT]?.let {
                set(
                    idx = bottomRight,
                    x = rightX + rightXOffset,
                    y = y + srcY,
                    width = rightWidthOffset,
                    height = bottomHeightOffset,
                    color = color,
                    srcX = it.x.toFloat() + rightXOffset,
                    srcY = it.y.toFloat() + min(srcY, it.height.toFloat()),
                    srcWidth = rightWidthOffset,
                    srcHeight = bottomHeightOffset,
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
        color: Color,
        srcX: Float,
        srcY: Float,
        srcWidth: Float,
        srcHeight: Float,
        stretchW: Boolean = false,
        stretchH: Boolean = false,
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
        if (
            slice.texture.samplerDescriptor.magFilter == FilterMode.Linear ||
                slice.texture.samplerDescriptor.minFilter == FilterMode.Linear
        ) {
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

        // top left
        vertices[idx] = x
        vertices[idx + 1] = fy2
        vertices[idx + 2] = 0f // z
        vertices[idx + 3] = color.r
        vertices[idx + 4] = color.g
        vertices[idx + 5] = color.b
        vertices[idx + 6] = color.a
        vertices[idx + 7] = u
        vertices[idx + 8] = v

        // top right
        vertices[idx + 9] = fx2
        vertices[idx + 10] = fy2
        vertices[idx + 11] = 0f // z
        vertices[idx + 12] = color.r
        vertices[idx + 13] = color.g
        vertices[idx + 14] = color.b
        vertices[idx + 15] = color.a
        vertices[idx + 16] = u2
        vertices[idx + 17] = v

        // bottom right
        vertices[idx + 18] = fx2
        vertices[idx + 19] = y
        vertices[idx + 20] = 0f // z
        vertices[idx + 21] = color.r
        vertices[idx + 22] = color.g
        vertices[idx + 23] = color.b
        vertices[idx + 24] = color.a
        vertices[idx + 25] = u2
        vertices[idx + 26] = v2

        // bottom left
        vertices[idx + 27] = x
        vertices[idx + 28] = y
        vertices[idx + 29] = 0f // z
        vertices[idx + 30] = color.r
        vertices[idx + 31] = color.g
        vertices[idx + 32] = color.b
        vertices[idx + 33] = color.a
        vertices[idx + 34] = u
        vertices[idx + 35] = v2
    }

    companion object {
        private const val BOTTOM_LEFT = 0
        private const val BOTTOM_CENTER = 1
        private const val BOTTOM_RIGHT = 2
        private const val MIDDLE_LEFT = 3
        private const val MIDDLE_CENTER = 4
        private const val MIDDLE_RIGHT = 5
        private const val TOP_LEFT = 6
        private const val TOP_CENTER = 7
        private const val TOP_RIGHT = 8
    }
}
