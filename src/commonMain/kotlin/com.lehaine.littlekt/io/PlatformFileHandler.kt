package com.lehaine.littlekt.io

import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.render.TextureImage

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
expect class PlatformFileHandler {

    val logger: Logger

    fun read(filename: String): Content<String>

    fun readData(filename: String): Content<ByteArray>

    fun readTextureImage(filename: String): Content<TextureImage>

    fun readSound(filename: String): Content<Sound>
}