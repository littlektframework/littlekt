package com.littlekt.graphics

import com.littlekt.Releasable

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor actual constructor(pixmap: Pixmap, xHotspot: Int, yHotSpot: Int) : Releasable {
    actual val pixmap: Pixmap
        get() = TODO("Not yet implemented")

    actual val xHotspot: Int
        get() = TODO("Not yet implemented")

    actual val yHotSpot: Int
        get() = TODO("Not yet implemented")

    actual override fun release() {
        TODO("Not yet implemented")
    }
}
