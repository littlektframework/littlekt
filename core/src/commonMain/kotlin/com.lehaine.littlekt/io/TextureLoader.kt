package com.lehaine.littlekt.io

import com.lehaine.littlekt.graphics.Texture

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class TextureLoader : FileLoader<Texture> {

    private val textureImageLoader = TextureDataLoader()

    override fun load(filename: String, handler: FileHandler): Content<Texture> {
        val content = handler.create<Texture>(filename)
        textureImageLoader.load(filename, handler).onLoaded {
            val texture = Texture(it)
            content.load(texture)
            handler.application.assetManager.add(texture)
        }
        return content
    }
}