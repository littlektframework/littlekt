package com.lehaine.littlekt.math.geom

/**
 * @author Colton Daily
 * @date 12/28/2021
 */
open class Point(open val x: Float, open val y: Float) {
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    companion object {
        val ZERO = Point(0f, 0f)
    }
}

open class MutablePoint(override var x: Float, override var y: Float) : Point(x, y) {
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())
}