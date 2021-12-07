package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion


/**
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {

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
    fun getGLVersion(): GLVersion

    /**
     * @return if the current GL version is 3.2 or higher
     */
    fun isGL32() = getGLVersion() == GLVersion.GL_30

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    fun supportsExtension(extension: String): Boolean
}