package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.SystemCursor


/**
 * Contains graphic related properties and methods.
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {

    /**
     * The OpenGL instance.
     */
    val gl: GL

    /**
     * @return the width of the client area in logical pixels.
     */
    val width: Int

    /**
     * @return the height of the client area in logical pixels
     */
    val height: Int

    /**
     * @return the width of the back framebuffer in physical pixels
     */
    val backBufferWidth: Int

    /**
     * @return the height of the back framebuffer in physical pixels
     */
    val backBufferHeight: Int

    /**
     * @return the [GLVersion] of this Graphics instance
     */
    val glVersion: GLVersion get() = gl.version

    /**
     * @return if the current GL version is 3.0 or higher
     */
    val isGL30 get() = gl.isG30

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    fun supportsExtension(extension: String): Boolean

    /**
     * Overrides the current cursor with the specified [cursor].
     */
    fun setCursor(cursor: Cursor)

    /**
     * Overrides the current cursor with a default system cursor. See [SystemCursor]
     */
    fun setCursor(cursor: SystemCursor)

    /**
     * Converts back buffer x-coordinate to logical screen coordinates.
     */
    fun toLogicalX(backBufferX: Int) = backBufferX.toLogicalX

    /**
     * Converts back buffer y-coordinate to logical screen coordinates.
     */
    fun toLogicalY(backBufferY: Int) = backBufferY.toLogicalY

    /**
     * Converts logical screen x-coordinate to back buffer coordinates.
     */
    fun toBackBufferX(logicalX: Int) = logicalX.toBackBufferX

    /**
     * Converts logical screen y-coordinate to back buffer coordinates.
     */
    fun toBackBufferY(logicalY: Int) = logicalY.toBackBufferY

    /**
     * Converts back buffer x-coordinate to logical screen coordinates.
     */
    val Int.toLogicalX get() = (this * width / backBufferWidth.toFloat()).toInt()

    /**
     * Converts back buffer y-coordinate to logical screen coordinates.
     */
    val Int.toLogicalY get() = (this * height / backBufferHeight.toFloat()).toInt()

    /**
     * Converts logical screen x-coordinate to back buffer coordinates.
     */
    val Int.toBackBufferX get() = (this * backBufferWidth / width.toFloat()).toInt()

    /**
     * Converts logical screen y-coordinate to back buffer coordinates.
     */
    val Int.toBackBufferY get() = (this * backBufferHeight / height.toFloat()).toInt()
}