package com.lehaine.littlekt.io

import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.render.TextureImage

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
actual class PlatformFileHandler {
    actual val logger: Logger
        get() = TODO("Not yet implemented")

    actual fun read(filename: String): Content<String> {
        TODO("Not yet implemented")
    }

    actual fun readData(filename: String): Content<ByteArray> {
        TODO("Not yet implemented")
    }

    actual fun readTextureImage(filename: String): Content<TextureImage> {
        TODO("Not yet implemented")
    }

    actual fun readSound(filename: String): Content<Sound> {
        TODO("Not yet implemented")
    }
}