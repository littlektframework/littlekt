package com.littlekt

import com.littlekt.util.Clipboard
import org.lwjgl.glfw.GLFW

/**
 * @author Colton Daily
 * @date 2/21/2022
 */
class JvmClipboard(private val window: Long) : Clipboard {
    override val hasContents: Boolean
        get() {
            val value = contents
            return !value.isNullOrEmpty()
        }

    override var contents: String?
        get() = GLFW.glfwGetClipboardString(window)
        set(value) {
            GLFW.glfwSetClipboardString(window, value ?: "")
        }
}
