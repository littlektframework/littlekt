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
    actual val pixmap: Pixmap = Pixmap(0, 0)
    actual val xHotspot: Int = 0
    actual val yHotSpot: Int = 0
    actual override fun dispose() = Unit
}