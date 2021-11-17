package com.lehaine.littlekt.io

import com.lehaine.littlekt.graphics.render.Texture

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class TextureLoader : FileLoader<Texture> {

    private val textureImageLoader = TextureImageLoader()

    override fun load(filename: String, handler: FileHandler): Content<Texture> {
        val result = Texture(
            byteArrayOf(
                0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(),
                0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(),
                0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte()
            ),
            2,
            2,
            hasAlpha = false
        )

        // Load the default texture
        handler.application.assetManager.add(result)

        val content = handler.create(filename, result)

        textureImageLoader.load(filename, handler).onLoaded { textureImage ->
            // The final texture is loaded.
            // Load this texture instead of the default one.
            result.textureImage = textureImage
            result.height = textureImage.height
            result.width = textureImage.width
            handler.application.assetManager.add(result)
        }
        return content
    }
}