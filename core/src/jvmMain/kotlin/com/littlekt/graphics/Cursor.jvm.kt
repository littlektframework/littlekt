package com.littlekt.graphics

import com.littlekt.Releasable
import java.nio.ByteBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWImage

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor
actual constructor(actual val pixmap: Pixmap, actual val xHotspot: Int, actual val yHotSpot: Int) :
    Releasable {

    private val glfwImage: GLFWImage
    private val pixmapCopy: Pixmap
    private var destroyed = false

    val cursorHandle: Long

    init {
        check((pixmap.width and (pixmap.width - 1)) == 0) {
            "Cursor image pixmap width of ${pixmap.width} is not a power-of-two greater than zero."
        }
        check((pixmap.height and (pixmap.height - 1)) == 0) {
            "Cursor image pixmap height of ${pixmap.height} is not a power-of-two greater than zero."
        }
        check(xHotspot > 0 && xHotspot < pixmap.width) {
            "xHotspot coordinate of $xHotspot is not within image width bounds: [0, ${pixmap.width})."
        }
        check(yHotSpot > 0 && yHotSpot < pixmap.height) {
            "yHotSpot coordinate of $yHotSpot is not within image width bounds: [0, ${pixmap.height})."
        }
        pixmapCopy = Pixmap(pixmap.width, pixmap.height).also { it.draw(pixmap, 0, 0) }
        glfwImage =
            GLFWImage.malloc().apply {
                width(pixmapCopy.width)
                height(pixmapCopy.height)
                pixels(
                    ByteBuffer.allocateDirect(pixmapCopy.pixels.capacity)
                        .put(pixmapCopy.pixels.toArray())
                        .flip()
                )
            }

        cursorHandle = GLFW.glfwCreateCursor(glfwImage, xHotspot, yHotSpot)
    }

    actual override fun release() {
        check(!destroyed) { "Cursor already disposed." }

        destroyed = true
        glfwImage.free()
        GLFW.glfwDestroyCursor(cursorHandle)
    }
}
