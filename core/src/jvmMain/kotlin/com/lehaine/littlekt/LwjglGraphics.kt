package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.SystemCursor
import org.lwjgl.glfw.GLFW

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class LwjglGraphics(val context: LwjglContext, engineStats: EngineStats) : Graphics {
    private val systemCursors = mutableMapOf<SystemCursor, Long>()

    override val gl: LwjglGL = LwjglGL(engineStats)

    internal var _width: Int = 0
    internal var _height: Int = 0

    override val width: Int
        get() = _width
    override val height: Int
        get() = _height

    override fun supportsExtension(extension: String): Boolean {
        return GLFW.glfwExtensionSupported(extension)
    }

    override fun setCursor(cursor: Cursor) {
        GLFW.glfwSetCursor(context.windowHandle, cursor.cursorHandle)
    }

    override fun setCursor(cursor: SystemCursor) {
        var handle = systemCursors[cursor]
        if (handle == null) {
            handle = when (cursor) {
                SystemCursor.ARROW -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)
                SystemCursor.I_BEAM -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)
                SystemCursor.CROSSHAIR -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR)
                SystemCursor.HAND -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)
                SystemCursor.HORIZONTAL_RESIZE -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)
                SystemCursor.VERTICAL_RESIZE -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)
            }
            if (handle == 0L) return
            systemCursors[cursor] = handle
        }
        GLFW.glfwSetCursor(context.windowHandle, handle)
    }
}