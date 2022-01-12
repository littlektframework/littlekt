package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 1/11/2022
 */
actual class Cursor actual constructor(
    actual val pixmap: Pixmap,
    actual val xHotspot: Int,
    actual val yHotSpot: Int
) : Disposable {

    actual override fun dispose() {

    }

}