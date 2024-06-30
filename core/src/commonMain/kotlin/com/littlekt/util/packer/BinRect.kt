package com.littlekt.util.packer

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
open class BinRect(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    isRotated: Boolean = false,
    allowRotation: Boolean? = null,
    data: Map<String, Any> = mapOf(),
    tag: String? = null
) {
    protected var _dirty = 0

    protected var _x = x
    protected var _y = y
    protected var _width = width
    protected var _height = height
    protected var _isRotated: Boolean = isRotated
    protected var _allowRotation: Boolean? = allowRotation
    protected var _tag = tag

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

    var tag: String?
        get() = _tag
        set(value) {
            if (value == _tag) return
            _tag = value
            _dirty++
        }

    var dirty: Boolean
        get() = _dirty > 0
        set(value) {
            _dirty = if (value) _dirty + 1 else 0
        }

    var oversized: Boolean = false
    val area: Int
        get() = width * height

    val data: MutableMap<String, Any> = data.toMutableMap()

    fun collides(rect: BinRect): Boolean =
        (rect.x < x + width &&
            rect.x + rect.width > x &&
            rect.y < y + height &&
            rect.y + rect.height > y)

    fun contains(rect: BinRect): Boolean =
        (rect.x >= x &&
            rect.y >= y &&
            rect.x + rect.width <= x + width &&
            rect.y + rect.height <= y + height)

    override fun toString(): String {
        return "Rect(x=$x, y=$y, width=$width, height=$height, isRotated=$isRotated, allowRotation=$allowRotation, tag=$tag, dirty=$dirty, oversized=$oversized, area=$area, data=$data)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BinRect

        if (_dirty != other._dirty) return false
        if (_x != other._x) return false
        if (_y != other._y) return false
        if (_width != other._width) return false
        if (_height != other._height) return false
        if (_isRotated != other._isRotated) return false
        if (_allowRotation != other._allowRotation) return false
        if (_tag != other._tag) return false
        if (oversized != other.oversized) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _dirty
        result = 31 * result + _x
        result = 31 * result + _y
        result = 31 * result + _width
        result = 31 * result + _height
        result = 31 * result + _isRotated.hashCode()
        result = 31 * result + (_allowRotation?.hashCode() ?: 0)
        result = 31 * result + (_tag?.hashCode() ?: 0)
        result = 31 * result + oversized.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}
