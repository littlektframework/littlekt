package com.littlekt.graphics

import com.littlekt.Releasable

actual class Cursor
actual constructor(actual val pixmap: Pixmap, actual val xHotspot: Int, actual val yHotSpot: Int) : Releasable {

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
    }

    actual override fun release() = Unit
}