package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.util.Signal

/**
 * A base viewport where the virtual size is the same as the viewport/screen size.
 * @author Colton Daily
 * @date 11/27/2021
 */
open class Viewport(
    var x: Int, var y: Int,
    /**
     * The width of the viewport.
     */
    var width: Int,
    /**
     * The height of the viewport.
     */
    var height: Int
) {
    constructor() : this(0, 0, 0, 0)

    val onSizeChanged = Signal()

    /**
     * The virtual/world width.
     */
    var virtualWidth = width

    /**
     * The virtual/world height.
     */
    var virtualHeight = height

    val aspectRatio get() = width.toFloat() / height.toFloat()

    fun isInViewport(x: Float, y: Float) = x >= this.x && x < this.x + width && y >= this.y && y < this.y + height

    /**
     * Set the viewport position and size.
     */
    fun set(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.width = width
        this.y = y
        this.height = height
    }

    /**
     * Resize the viewport based new size.
     */
    open fun update(width: Int, height: Int, context: Context) {
        set(0, 0, width, height)
        apply(context)
        onSizeChanged.emit()
    }

    /**
     * Sets the OpenGL viewport based on viewport and virtual sizes by calling [GL.viewport].
     */
    fun apply(context: Context) {
        context.gl.viewport(x, y, width, height)
    }

    override fun toString(): String {
        return "Viewport(x=$x, y=$y, width=$width, height=$height, virtualWidth=$virtualWidth, virtualHeight=$virtualHeight, aspectRatio=$aspectRatio)"
    }

}