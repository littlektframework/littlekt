package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor actual constructor(
    pixmap: Pixmap,
    xHotspot: Int,
    yHotSpot: Int
) : Disposable {
    actual val pixmap: Pixmap
        get() = TODO("Not yet implemented")
    actual val xHotspot: Int
        get() = TODO("Not yet implemented")
    actual val yHotSpot: Int
        get() = TODO("Not yet implemented")

    actual override fun dispose() {}

}