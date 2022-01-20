package com.lehaine.littlekt.graphics

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
    ) : this(slice.texture, x, y, width, height)

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
        // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
        if (width == 1 && height == 1) {
            val adjustX = 0.25f / texture.width
            val adjustY = 0.25f / texture.height
            this.u += adjustX
            this.u2 -= adjustX
            this.v += adjustY
            this.v2 -= adjustY
        }
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

    fun slice(sliceWidth: Int, sliceHeight: Int): Array<Array<TextureSlice>> {
        val cols = width / sliceWidth
        val rows = height / sliceHeight

        var y = this.y - sliceHeight
        var x: Int
        val startX = this.x - sliceWidth

        return Array(rows) {
            x = startX
            y += sliceHeight

            Array(cols) {
                x += sliceWidth
                TextureSlice(texture, x, y, sliceWidth, sliceHeight)
            }
        }
    }

    override fun toString(): String {
        return "TextureSlice(u=$u, v=$v, u2=$u2, v2=$v2, width=$width, height=$height, x=$x, y=$y, isFlipH=$isFlipH, isFlipV=$isFlipV)"
    }
}