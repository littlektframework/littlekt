package com.lehaine.littlekt.render

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.GL
import com.lehaine.littlekt.io.Asset
import com.lehaine.littlekt.shader.TextureReference

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class Texture private constructor(
    var textureImage: TextureImage? = null,
    var textureData: ByteArray,
    var width: Int,
    var height: Int,
    var hasAlpha: Boolean = false
) : Asset {
    var textureReference: TextureReference? = null

    private val onLoad = mutableListOf<(Asset) -> Unit>()

    constructor(source: TextureImage, hasAlpha: Boolean) : this(
        textureImage = source,
        textureData = byteArrayOf(),
        width = source.width,
        height = source.height,
        hasAlpha = hasAlpha
    )

    constructor(textureData: ByteArray, width: Int, height: Int, hasAlpha: Boolean) : this(
        textureImage = null,
        textureData = textureData,
        width = width,
        height = height,
        hasAlpha = hasAlpha
    )

    override fun load(application: Application) {
        val gl = application.graphics.GL
        val texture = textureReference ?: gl.createTexture()
        gl.bindTexture(GL.TEXTURE_2D, texture)

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

        val tx = textureImage
        if (tx != null) {
            gl.texImage2D(
                GL.TEXTURE_2D,
                0,
                GL.RGBA,
                GL.RGBA,
                GL.UNSIGNED_BYTE,
                tx
            )
        } else {
            gl.texImage2D(
                GL.TEXTURE_2D,
                0,
                GL.RGBA,
                GL.RGBA,
                width,
                height,
                GL.UNSIGNED_BYTE,
                textureData
            )
        }
        textureReference = texture

        // Invoke all callbacks
        onLoad.forEach { it.invoke(this) }
    }

    override fun onLoad(callback: (Asset) -> Unit) {
        onLoad.add(callback)
    }
}