package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TextureTarget
import com.lehaine.littlekt.io.Asset

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class Texture(
    val textureData: TextureData
) : Asset {
    val width: Int get() = textureData.width
    val height: Int get() = textureData.height
    var glTexture: com.lehaine.littlekt.graphics.gl.GlTexture? = null
    private var gl: GL? = null

    private val onLoad = mutableListOf<(Asset) -> Unit>()

    override fun load(application: Application) {
        this.gl = application.graphics.gl
        val gl = application.graphics.gl
        if (!textureData.isPrepared) {
            textureData.prepare()
        }
        val texture = glTexture ?: gl.createTexture()

        gl.bindTexture(GL.TEXTURE_2D, texture)

        // TODO - impl setting min/max filters
        gl.texParameteri(
            GL.TEXTURE_2D,
            GL.TEXTURE_MAG_FILTER,
            GL.NEAREST
        )
        gl.texParameteri(
            GL.TEXTURE_2D,
            GL.TEXTURE_MIN_FILTER,
            GL.NEAREST
        )

        // TODO - impl setting wrap
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)

        textureData.uploadImageData(application, TextureTarget._2D, textureData)

        glTexture = texture

        // Invoke all callbacks
        onLoad.forEach { it.invoke(this) }
    }

    fun bind(unit: Int = 0) {
        val gl = gl
        val textureReference = glTexture
        if (gl == null || textureReference == null) {
            throw IllegalStateException("Texture has not been loaded yet! Unable to bind!")
        }
        gl.activeTexture(GL.TEXTURE0 + unit)
        gl.bindTexture(GL.TEXTURE_2D, textureReference)

    }

    override fun onLoad(callback: (Asset) -> Unit) {
        onLoad.add(callback)
    }

    companion object {
        val DEFAULT = Texture(
            PixmapTextureData(
                Pixmap(
                    2, 2,
                    byteArrayOf(
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(),
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(),
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                        0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte()
                    )
                ),
                true
            )
        )
    }
}