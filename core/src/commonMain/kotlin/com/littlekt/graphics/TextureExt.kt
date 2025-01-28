package com.littlekt.graphics

import com.littlekt.graphics.g2d.TextureSlice
import io.ygdrasil.webgpu.Device

/** Creates a new [TextureSlice] that encompasses the entire [Texture]. */
fun Texture.slice(): TextureSlice = TextureSlice(this)

/** Slices the current [Texture] into a 2D-array of [TextureSlice]. */
fun Texture.slice(sliceWidth: Int, sliceHeight: Int): Array<Array<TextureSlice>> =
    TextureSlice(this).slice(sliceWidth, sliceHeight)

/**
 * Slice up a new [Texture] in a list of [TextureSlice] with the given size with an added border.
 * This can be used to prevent atlas bleeding.
 *
 * @param device the current device - used to prepare the newly created Texture.
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 * @param mipmaps use mipmaps or not for the new texture
 */
fun Texture.sliceWithBorder(
    device: Device,
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1,
    mipmaps: Boolean = false
): List<TextureSlice> {
    return if (this is PixmapTexture) {
        pixmap.sliceWithBorderToTexture(
            device,
            textureDescriptor.format,
            sliceWidth,
            sliceHeight,
            border,
            mipmaps
        )
    } else {
        error("Unsupported Texture type!")
    }
}

/**
 * Slice up the texture with the given size with an added border but returns the newly created
 * [Texture]. This can be used to prevent atlas bleeding. Currently, only supports [PixmapTexture].
 *
 * @param device the current device - used to prepare the newly created Texture.
 * @param sliceWidth the width of each slice
 * @param sliceHeight the height of each slice
 * @param border the thickness of the border for each slice
 * @param mipmaps use mipmaps or not for the new texture
 */
fun Texture.addBorderToSlices(
    device: Device,
    sliceWidth: Int,
    sliceHeight: Int,
    border: Int = 1,
    mipmaps: Boolean = false
): Texture {
    return if (this is PixmapTexture) {
        pixmap.addBorderToSlicesToTexture(
            device,
            textureDescriptor.format,
            sliceWidth,
            sliceHeight,
            border,
            mipmaps
        )
    } else {
        error("Unsupported Texture type!")
    }
}
