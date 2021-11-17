package com.lehaine.littlekt.graphics.render

import java.nio.ByteBuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class TextureImage(
    actual val width: Int,
    actual val height: Int,
    val glFormat: Int,
    val glType: Int,
    val pixels: ByteBuffer
)