package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
open class Rect(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    isRotated: Boolean = false,
    allowRotation: Boolean? = null,
    data: Map<String, Any> = mapOf()
) {

    protected var _dirty = 0

    protected var _x = x
    protected var _y = y
    protected var _width = width
    protected var _height = height
    protected var _isRotated: Boolean = isRotated
    protected var _allowRotation: Boolean? = allowRotation

    var x: Int
        get() = _x
        set(value) {
            if (value == _x) return
            _x = value
            _dirty++
        }
    var y: Int
        get() = _y
        set(value) {
            if (value == _y) return
            _y = value
            _dirty++
        }
    var width: Int
        get() = _width
        set(value) {
            if (value == _width) return
            _width = value
            _dirty++
        }
    var height: Int
        get() = _height
        set(value) {
            if (value == _height) return
            _height = value
            _dirty++
        }

    var isRotated: Boolean
        get() = _isRotated
        set(value) {
            if (_allowRotation == false) return
            if (value == _isRotated) return
            val tmp = width
            width = height
            height = tmp
            _isRotated = value
            _dirty++
        }
    var allowRotation: Boolean?
        get() = _allowRotation
        set(value) {
            if (value == _allowRotation) return
            _allowRotation = value
            _dirty++
        }

    var dirty: Boolean
        get() = _dirty > 0
        set(value) {
            _dirty = if (value) _dirty + 1 else 0
        }

    var oversized: Boolean = false
    val area: Int get() = width * height


    val data: MutableMap<String, Any> = data.toMutableMap()

    fun collides(rect: Rect): Boolean =
        (rect.x < x + width
                && rect.x + rect.width > x
                && rect.y < y + height
                && rect.y + rect.height > y)

    fun contains(rect: Rect): Boolean = (rect.x >= x && rect.y >= y
            && rect.x + rect.width <= x + width
            && rect.y + rect.height <= y + height)

    override fun toString(): String {
        return "Rect(x=$x, y=$y, width=$width, height=$height, isRotated=$isRotated, useRotation=$allowRotation, dirty=$dirty, oversized=$oversized, area=$area, data=$data)"
    }

}