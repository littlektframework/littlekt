package com.littlekt.graphics

/**
 * A rectangular slice of a pixmap.
 *
 * @author Colt Daily 12/22/2021
 */
data class PixmapSlice(
    val pixmap: Pixmap,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = pixmap.width,
    val height: Int = pixmap.height,
)
