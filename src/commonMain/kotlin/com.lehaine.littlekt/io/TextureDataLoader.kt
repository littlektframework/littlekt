package com.lehaine.littlekt.io

import com.lehaine.littlekt.graphics.TextureData

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class TextureDataLoader : FileLoader<TextureData> {

    override fun load(filename: String, handler: FileHandler): Content<TextureData> {
        return handler.readTextureData(filename)
    }
}