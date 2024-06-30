package com.littlekt.graphics.g2d

import com.littlekt.graphics.Texture
import com.littlekt.math.Rect
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.atomicfu.atomic

/**
 * A rectangular "slice" or "region" of a [Texture]. Top-left is `0,0` and bottom right is
 * `width,height`
 *
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

    /** Hash code based on the [Texture.id] and the coordinates and size of this slice. */
    val id: Int = run {
        var result = texture.id
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + lastId.getAndIncrement()
        result
    }

    private var _u = 0f
    private var _v = 0f
    private var _u2 = 0f
    private var _v2 = 0f
    private var _width = 0
    private var _height = 0

    /** The u-coordinate (i.e. 'x') of this slice. `0f` would indicate the left side. */
    var u: Float
        get() = _u
        set(value) {
            _u = value
            _width = (abs(u1 - u) * texture.width).roundToInt()
        }

    /** The v-coordinate (i.e. 'y') of this slice. `0f` would indicate the top side. */
    var v: Float
        get() = _v
        set(value) {
            _v = value
            _height = (abs(v1 - v) * texture.height).roundToInt()
        }

    /** The u1-coordinate (i.e. 'x2') of this slice. `1f` would indicate the right side. */
    var u1: Float
        get() = _u2
        set(value) {
            _u2 = value
            _width = (abs(u1 - u) * texture.width).roundToInt()
        }

    /** The v1-coordinate (i.e. 'y') of this slice. `1f` would indicate the bottom side. */
    var v1: Float
        get() = _v2
        set(value) {
            _v2 = value
            _height = (abs(v1 - v) * texture.height).roundToInt()
        }

    /** The width of the slice. */
    var width: Int
        get() = _width
        set(value) {
            if (isFlipH) {
                u = u1 + value / texture.width.toFloat()
            } else {
                u1 = u + value / texture.width.toFloat()
            }
        }

    /** The height of the slice. */
    var height: Int
        get() = _height
        set(value) {
            if (isFlipV) {
                v = v1 + value / texture.height.toFloat()
            } else {
                v1 = v + value / texture.height.toFloat()
            }
        }

    /** The x-coord of the location of the slice on the texture. */
    var x: Int
        get() = (u * texture.width).roundToInt()
        set(value) {
            u = value / texture.width.toFloat()
        }

    /** The y-coord of the location of the slice on the texture. */
    var y: Int
        get() = (v * texture.height).roundToInt()
        set(value) {
            v = value / texture.height.toFloat()
        }

    /** Is the slice flipped horizontally: `u > u1`. */
    val isFlipH: Boolean
        get() = u > u1

    /** Is the slice flipped vertically: `v > v1`. */
    val isFlipV: Boolean
        get() = v > v1

    var virtualFrame: Rect? = null

    val offsetX: Int
        get() = virtualFrame?.x?.toInt() ?: 0

    val offsetY: Int
        get() = virtualFrame?.y?.toInt() ?: 0

    val packedWidth: Int
        get() = virtualFrame?.width?.toInt() ?: width

    val packedHeight: Int
        get() = virtualFrame?.height?.toInt() ?: height

    var originalWidth: Int = abs(width)
    var originalHeight: Int = abs(height)

    var rotated: Boolean = false

    init {
        setSlice(x, y, width, height)
    }

    fun setSlice(x: Int, y: Int, width: Int, height: Int) {
        val invTexWidth = 1f / texture.width
        val invTexHeight = 1f / texture.height
        setSlice(
            x * invTexWidth,
            y * invTexHeight,
            (x + width) * invTexWidth,
            (y + height) * invTexHeight
        )
        this.width = abs(width)
        this.height = abs(height)
    }

    fun setSlice(u: Float, v: Float, u2: Float, v2: Float) {
        width = (abs(u2 - u) * texture.width).roundToInt()
        height = (abs(v2 - v) * texture.height).roundToInt()

        this.u = u
        this.v = v
        this.u1 = u2
        this.v1 = v2
    }

    fun setSlice(slice: TextureSlice) {
        texture = slice.texture
        setSlice(slice.u, slice.v, slice.u1, slice.v1)
    }

    fun setSlice(slice: TextureSlice, x: Int, y: Int, width: Int, height: Int) {
        texture = slice.texture
        setSlice(slice.x + x, slice.y + y, width, height)
    }

    fun flipH() {
        val temp = u
        u = u1
        u1 = temp
    }

    fun flipV() {
        val temp = v
        v = v1
        v1 = temp
    }

    /**
     * Slice this [TextureSlice] into smaller slices.
     *
     * @param sliceWidth the width of the slice
     * @param sliceHeight the height of the slice
     * @param border the thickness of the border the slice has. This will usually be `0` but could
     *   change if [Texture.sliceWithBorder] was used and needed to keep the original slice sizes.
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
        return "TextureSlice(id=$id, u=$u, v=$v, u2=$u1, v2=$v1, width=$width, height=$height, x=$x, y=$y, isFlipH=$isFlipH, isFlipV=$isFlipV)"
    }

    companion object {
        private var lastId = atomic(0)
    }
}
