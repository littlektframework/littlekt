package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion


/**
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {
    /**
     * Enumeration describing different types of [Graphics] implementations.
     */
    enum class GraphicsType {
        AndroidGL, WebGL, iOSGL, Mock, LWJGL3
    }

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
     * @return the width of the framebuffer in physical pixels
     */
    val backBufferWidth: Int

    /**
     * @return the height of the framebuffer in physical pixels
     */
    val backBufferHeight: Int

    /**
     * @return amount of pixels per logical pixel (point)
     */
    val backBufferScale: Float get() = backBufferWidth / width.toFloat()

    /**
     * @return the [GraphicsType] of this Graphics instance
     */
    fun getType(): GraphicsType?

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