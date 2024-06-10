package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.file.ByteBufferImpl
import kotlinx.browser.document
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor
actual constructor(actual val pixmap: Pixmap, actual val xHotspot: Int, actual val yHotSpot: Int) :
    Releasable {
    val cssCursorProperty: String

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
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = pixmap.width
        canvas.height = pixmap.height
        val canvasCtx = canvas.getContext("2d") as CanvasRenderingContext2D
        canvasCtx.putImageData(
            ImageData(
                Uint8ClampedArray((pixmap.pixels as ByteBufferImpl).buffer.buffer),
                pixmap.width,
                pixmap.height
            ),
            0.0,
            0.0
        )
        val dataUrl = canvas.toDataURL("image/png")
        cssCursorProperty = "url('${dataUrl}')$xHotspot $yHotSpot,auto"
    }

    actual override fun release() = Unit
}
