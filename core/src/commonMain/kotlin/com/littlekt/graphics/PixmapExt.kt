package com.littlekt.graphics

import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.math.nextPowerOfTwo
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Slice up the pixmap in a list of [PixmapSlice] with the given size with an added border and
 * creates a new [Pixmap]. aThis can be used to prevent atlas bleeding.
 *
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 */
fun Pixmap.sliceWithBorder(
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1,
): List<PixmapSlice> = internalSliceWithBorder(sliceWidth, sliceHeight, border).second

/**
 * Slice up the Pixmap in a list of [PixmapSlice] with the given size with an added border and
 * creates a new [Texture] and then slices the texture into a list of [TextureSlice]. This can be
 * used to prevent atlas bleeding.
 *
 * @param device the current device - used to prepare the newly created Texture.
 * @param preferredFormat the format to use for the newly created Texture.
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 * @param mipmaps use mipmaps or not for the new texture
 * @return list of [TextureSlice] of the given width, height, and border
 */
fun Pixmap.sliceWithBorderToTexture(
    device: Device,
    preferredFormat: TextureFormat,
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1,
    mipmaps: Boolean = false
): List<TextureSlice> {
    val (sliceInfo, slices) = internalSliceWithBorder(sliceWidth, sliceHeight, border)
    val texture = PixmapTexture(device, preferredFormat, slices[0].pixmap)
    return List(slices.size) { n ->
        val y = n / sliceInfo.columns
        val x = n % sliceInfo.columns
        val px = x * sliceInfo.newWidth + border
        val py = y * sliceInfo.newHeight + border
        TextureSlice(texture, px, py, sliceWidth, sliceHeight)
    }
}

private fun Pixmap.internalSliceWithBorder(
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1
): Pair<BorderSliceInfo, List<PixmapSlice>> {
    val slices = slice(sliceWidth, sliceHeight).flatten()
    val newWidth = sliceWidth + border * 2
    val newHeight = sliceHeight + border * 2
    val area = newWidth * newHeight
    val fullArea = slices.size.nextPowerOfTwo * area
    val length = ceil(sqrt(fullArea.toDouble())).toInt().nextPowerOfTwo

    val out = Pixmap(length, length)

    val columns = (out.width / newWidth)

    slices.forEachIndexed { n, slice ->
        val y = n / columns
        val x = n % columns
        val px = x * newWidth + border
        val py = y * newHeight + border
        out.drawSlice(px, py, slice.pixmap, slice.x, slice.y, slice.width, slice.height, border)
    }
    val newSlices =
        List(slices.size) { n ->
            val y = n / columns
            val x = n % columns
            val px = x * newWidth + border
            val py = y * newHeight + border
            PixmapSlice(out, px, py, sliceWidth, sliceHeight)
        }
    return Pair(BorderSliceInfo(newWidth, newHeight, columns), newSlices)
}

private data class BorderSliceInfo(val newWidth: Int, val newHeight: Int, val columns: Int)

/**
 * Slice up the pixmap with the given size with an added border but returns the newly created
 * [Pixmap]. This can be used to prevent atlas bleeding.
 *
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 */
fun Pixmap.addBorderToSlices(sliceWidth: Int, sliceHeight: Int, border: Int = 1): Pixmap =
    internalSliceWithBorder(sliceWidth, sliceHeight, border).second[0].pixmap

/**
 * Slice up the pixmap with the given size with an added border but returns the newly created
 * [Texture]. This can be used to prevent atlas bleeding.
 *
 * @param device the current device - used to prepare the newly created Texture.
 * @param preferredFormat the preferred texture format for the newly created Texture.
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 * @param mipmaps use mipmaps or not for the new texture
 */
fun Pixmap.addBorderToSlicesToTexture(
    device: Device,
    preferredFormat: TextureFormat,
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1,
    mipmaps: Boolean = false
): Texture =
    PixmapTexture(device, preferredFormat, addBorderToSlices(sliceWidth, sliceHeight, border))

/** Slices the current pixmap into a 2D-array of [PixmapSlice]. */
fun Pixmap.slice(sliceWidth: Int, sliceHeight: Int): Array<Array<PixmapSlice>> {
    val cols = width / sliceWidth
    val rows = height / sliceHeight

    var y = -sliceHeight
    var x: Int
    val startX = -sliceWidth

    return Array(rows) {
        x = startX
        y += sliceHeight

        Array(cols) {
            x += sliceWidth
            PixmapSlice(this, x, y, sliceWidth, sliceHeight)
        }
    }
}

/** Draw the given [slice] onto this [Pixmap]. */
fun Pixmap.drawSlice(x: Int, y: Int, slice: TextureSlice, border: Int = 0) {
    val texture =
        slice.texture as? PixmapTexture
            ?: error("The texture needs to be a PixmapTexture in order to read the underlying data")
    val sliceWidth = slice.width
    val sliceHeight = slice.height
    drawSlice(x, y, texture.pixmap, slice.x, slice.y, sliceWidth, sliceHeight, border)
}
