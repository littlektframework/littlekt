package com.lehaine.littlekt.io

import com.lehaine.littlekt.graphics.render.TextureImage

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class TextureImageLoader : FileLoader<TextureImage> {

    override fun load(filename: String, handler: FileHandler): Content<TextureImage> {
        return handler.readTextureImage(filename)
    }
}