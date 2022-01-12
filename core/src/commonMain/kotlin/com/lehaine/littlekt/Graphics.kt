package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.Pixmap


/**
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {

    /**
     * THe OpenGL instance.
     */
    val gl: GL

    /**
     * @return the width of the client area in logical pixels.
     */
    val width: Int

    /**
     *  @return the height of the client area in logical pixels
     */
    val height: Int

    /**
     * @return the [GLVersion] of this Graphics instance
     */
    val glVersion: GLVersion

    /**
     * @return if the current GL version is 3.0 or higher
     */
    fun isGL30OrHigher() =
        glVersion == GLVersion.GL_30 || glVersion == GLVersion.GL_32_PLUS || glVersion == GLVersion.WEBGL2

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    fun supportsExtension(extension: String): Boolean

    /**
     * Overrides the current cursor with the specified [cursor].
     */
    fun setCursor(cursor: Cursor)
}