package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import org.lwjgl.glfw.GLFW

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class LwjglGraphics(val context: LwjglContext, engineStats: EngineStats) : Graphics {
    override val gl: GL = LwjglGL(engineStats)

    internal var _glVersion: GLVersion = GLVersion.GL_30
        set(value) {
            (gl as LwjglGL)._glVersion = value
            field = value
        }

    internal var _width: Int = 0
    internal var _height: Int = 0

    override val width: Int
        get() = _width
    override val height: Int
        get() = _height
    override val glVersion: GLVersion
        get() = _glVersion

    override fun supportsExtension(extension: String): Boolean {
        return GLFW.glfwExtensionSupported(extension)
    }

    override fun setCursor(cursor: Cursor) {
        GLFW.glfwSetCursor(context.windowHandle, cursor.cursorHandle)
    }
}