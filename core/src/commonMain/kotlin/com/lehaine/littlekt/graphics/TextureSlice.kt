package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.math.Rect
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
open class TextureSlice(
    var texture: Texture,
    x: Int = 0,
    y: Int = 0,
    width: Int = texture.width,
    height: Int = texture.height
) {
    constructor(
        slice: TextureSlice,
        x: Int = 0,
        y: Int = 0,
        width: Int = slice.width,
        height: Int = slice.height
    ) : this(slice.texture, x + slice.x, y + slice.y, width, height)

    private var _u = 0f
    private var _v = 0f
    private var _u2 = 0f
    private var _v2 = 0f
    private var _width = 0
    private var _height = 0

    var u: Float
        get() = _u
        set(value) {
            _u = value
            _width = (abs(u2 - u) * texture.width).roundToInt()
        }
    var v: Float
        get() = _v
        set(value) {
            _v = value
            _height = (abs(v2 - v) * texture.height).roundToInt()
        }
    var u2: Float
        get() = _u2
        set(value) {
            _u2 = value
            _width = (abs(u2 - u) * texture.width).roundToInt()
        }
    var v2: Float
        get() = _v2
        set(value) {
            _v2 = value
            _height = (abs(v2 - v) * texture.height).roundToInt()
        }
    var width: Int
        get() = _width
        set(value) {
            if (isFlipH) {
                u = u2 + value / texture.width.toFloat()
            } else {
                u2 = u + value / texture.width.toFloat()
            }
        }
    var height: Int
        get() = _height
        set(value) {
            if (isFlipV) {
                v = v2 + value / texture.height.toFloat()
            } else {
                v2 = v + value / texture.height.toFloat()
            }
        }

    var x: Int
        get() = (u * texture.width).roundToInt()
        set(value) {
            u = value / texture.width.toFloat()
        }
    var y: Int
        get() = (v2 * texture.height).roundToInt()
        set(value) {
            v2 = value / texture.height.toFloat()
        }

    val isFlipH: Boolean get() = u > u2
    val isFlipV: Boolean get() = v > v2

    var virtualFrame: Rect? = null

    val offsetX: Int get() = virtualFrame?.x?.toInt() ?: 0
    val offsetY: Int get() = virtualFrame?.y?.toInt() ?: 0
    val packedWidth: Int get() = virtualFrame?.width?.toInt() ?: width
    val packedHeight: Int get() = virtualFrame?.height?.toInt() ?: height

    var originalWidth: Int = abs(width)
    var originalHeight: Int = abs(height)

    var rotated: Boolean = false

    init {
        setSlice(x, y, width, height)
    }

    fun setSlice(x: Int, y: Int, width: Int, height: Int) {
        val invTexWidth = 1f / texture.width
        val invTexHeight = 1f / texture.height
        setSlice(x * invTexWidth, (y + height) * invTexHeight, (x + width) * invTexWidth, y * invTexHeight)
        this.width = abs(width)
        this.height = abs(height)
    }

    fun setSlice(u: Float, v: Float, u2: Float, v2: Float) {
        width = (abs(u2 - u) * texture.width).roundToInt()
        height = (abs(v2 - v) * texture.height).roundToInt()

        this.u = u
        this.v = v
        this.u2 = u2
        this.v2 = v2
    }

    fun setSlice(slice: TextureSlice) {
        texture = slice.texture
        setSlice(slice.u, slice.v, slice.u2, slice.v2)
    }

    fun setSlice(slice: TextureSlice, x: Int, y: Int, width: Int, height: Int) {
        texture = slice.texture
        setSlice(slice.x + x, slice.y + y, width, height)
    }

    fun flipH() {
        val temp = u
        u = u2
        u2 = temp
    }

    fun flipV() {
        val temp = v
        v = v2
        v2 = temp
    }

    /**
     * Slice this [TextureSlice] into smaller slices.
     * @param sliceWidth the width of the slice
     * @param sliceHeight the height of the slice
     * @param border the thickness of the border the slice has. This will usually be `0` but could change if
     * [Texture.sliceWithBorder] was used and needed to keep the original slice sizes.
     */
    fun slice(sliceWidth: Int, sliceHeight: Int, border: Int = 0): Array<Array<TextureSlice>> {
        val cols = width / (sliceWidth + border * 2)
        val rows = height / (sliceHeight + border * 2)

        var y = this.y - sliceHeight - border
        var x: Int
        val startX = this.x - sliceWidth - border

        return Array(rows) {
            x = startX
            y += sliceHeight + border * 2

            Array(cols) {
                x += sliceWidth + border * 2
                TextureSlice(texture, x, y, sliceWidth, sliceHeight)
            }
        }
    }

    override fun toString(): String {
        return "TextureSlice(u=$u, v=$v, u2=$u2, v2=$v2, width=$width, height=$height, x=$x, y=$y, isFlipH=$isFlipH, isFlipV=$isFlipV)"
    }
}