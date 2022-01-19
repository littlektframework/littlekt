package com.lehaine.littlekt.graph.node.component.ui

import com.lehaine.littlekt.math.geom.MutablePoint
import com.lehaine.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
open class StyleBox {
    open var marginLeft: Float = 0f
    open var marginRight: Float = 0f
    open var marginTop: Float = 0f
    open var marginBottom: Float = 0f

    open val minWidth get() = marginLeft - marginRight
    open val minHeight get() = marginTop - marginBottom

    private val _offset = MutablePoint(0f, 0f)
    val offset: Point
        get() = _offset.also {
            it.x = marginLeft
            it.y = marginTop
        }
}