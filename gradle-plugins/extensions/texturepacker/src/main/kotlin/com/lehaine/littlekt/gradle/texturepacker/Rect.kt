package com.lehaine.littlekt.gradle.texturepacker

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
    useRotation: Boolean = false
) {

    protected var _dirty = 0

    protected var _x = x
    protected var _y = y
    protected var _width = width
    protected var _height = height
    protected var _isRotated: Boolean = isRotated
    protected var _useRotation: Boolean = useRotation

    var x: Int = x
        get() = _x
        set(value) {
            if (value == field) return
            _x = value
            _dirty++
        }
    var y: Int = _y
        get() = _y
        set(value) {
            if (value == field) return
            _y = value
            _dirty++
        }
    var width: Int = _width
        get() = _width
        set(value) {
            if (value == field) return
            _width = value
            _dirty++
        }
    var height: Int = _height
        get() = _height
        set(value) {
            if (value == field) return
            _height = value
            _dirty++
        }

    var isRotated: Boolean = _isRotated
        get() = _isRotated
        set(value) {
            if (!_useRotation) return
            if (value == field) return
            val tmp = width
            width = height
            height = tmp
            _isRotated = value
            _dirty++
        }
    var useRotation: Boolean = _useRotation
        get() = _useRotation
        set(value) {
            if (value == field) return
            _useRotation = value
            _dirty++
        }

    var dirty: Boolean
        get() = _dirty > 0
        set(value) {
            _dirty = if (value) _dirty + 1 else 0
        }

    var oversized: Boolean = false
    val area: Int get() = width * height

    fun collides(rect: Rect): Boolean =
        (rect.x < x + width
                && rect.x + rect.width > x
                && rect.y < y + height
                && rect.y + rect.height > y)

    fun contains(rect: Rect): Boolean = (rect.x >= x && rect.y >= y
            && rect.x + rect.width <= x + width
            && rect.y + rect.height <= y + height)
}