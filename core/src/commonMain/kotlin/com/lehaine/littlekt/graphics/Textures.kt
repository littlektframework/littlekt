package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.graphics.gl.PixmapTextureData

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
object Textures {
    val default = Texture(
        PixmapTextureData(
            Pixmap(
                9, 6,
                createByteBuffer(
                    byteArrayOf(
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                    )
                )
            ),
            false
        )
    )
    private val defaultSlices = default.slice(1, 1)
    val white: TextureSlice = defaultSlices[1][1]
    val black: TextureSlice = defaultSlices[1][4]
    val red: TextureSlice = defaultSlices[1][7]
    val green: TextureSlice = defaultSlices[4][1]
    val blue: TextureSlice = defaultSlices[4][4]

    val transparent: TextureSlice = defaultSlices[4][7]

}