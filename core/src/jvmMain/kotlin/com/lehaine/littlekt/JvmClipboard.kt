package com.lehaine.littlekt

import com.lehaine.littlekt.util.Clipboard
import org.lwjgl.glfw.GLFW

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
class JvmClipboard(private val window: Long) : Clipboard {
    override val hasContents: Boolean
        get() {
            val value = contents
            return value != null && value.isNotEmpty()
        }
    override var contents: String?
        get() = GLFW.glfwGetClipboardString(window)
        set(value) {
            GLFW.glfwSetClipboardString(window, value ?: "")
        }
}