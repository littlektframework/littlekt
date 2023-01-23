package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.OrthographicCamera

/**
 * A base viewport where the virtual size is the same as the viewport/screen size.
 * @author Colton Daily
 * @date 11/27/2021
 */
open class Viewport(
    var x: Int = 0,
    var y: Int = 0,
    /**
     * The width of the viewport.
     */
    var width: Int = 0,
    /**
     * The height of the viewport.
     */
    var height: Int = 0,

    var camera: Camera = OrthographicCamera(),
) {

    /**
     * The virtual/world width.
     */
    var virtualWidth: Float = width.toFloat()

    /**
     * The virtual/world height.
     */
    var virtualHeight: Float = height.toFloat()

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
     * Resize the viewport based new size. If using the base [Viewport] this function must be overridden
     * for it to do anything other than apply the current viewport size.
     */
    open fun update(width: Int, height: Int, context: Context, centerCamera: Boolean = false) {
        apply(context, centerCamera)
    }

    /**
     * Sets the OpenGL viewport based on viewport and virtual sizes by calling [GL.viewport].
     */
    fun apply(context: Context, centerCamera: Boolean = false) {
        context.gl.viewport(x, y, width, height)
        camera.virtualWidth = virtualWidth
        camera.virtualHeight = virtualHeight
        if (centerCamera) {
            camera.position.set(virtualWidth * 0.5f, virtualHeight * 0.5f, 0f)
        }
        camera.update()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(x=$x, y=$y, width=$width, height=$height, virtualWidth=$virtualWidth, virtualHeight=$virtualHeight, aspectRatio=$aspectRatio)"
    }

}