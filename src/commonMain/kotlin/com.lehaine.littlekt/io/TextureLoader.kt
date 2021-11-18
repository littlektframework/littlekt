package com.lehaine.littlekt.io

import com.lehaine.littlekt.graphics.Texture

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class TextureLoader : FileLoader<Texture> {

    private val textureImageLoader = TextureDataLoader()

    override fun load(filename: String, handler: FileHandler): Content<Texture> {
        val textureData by textureImageLoader.load(filename, handler)
        val result = Texture(textureData)
        return handler.create(filename, result)
    }
}