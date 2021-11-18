package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.GL
import com.lehaine.littlekt.io.Asset
import com.lehaine.littlekt.shader.TextureReference

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class Texture(
    val textureData: TextureData
) : Asset {
    val width: Int get() = textureData.width
    val height: Int get() = textureData.height
    var textureReference: TextureReference? = null

    private val onLoad = mutableListOf<(Asset) -> Unit>()

    override fun load(application: Application) {
        val gl = application.graphics.gl
        if (!textureData.isPrepared) {
            textureData.prepare()
        }
        val texture = textureReference ?: gl.createTexture()

        textureData.uploadImageData(application, GL.TEXTURE_2D, textureData)

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

        gl.bindTexture(GL.TEXTURE_2D, texture)

        textureReference = texture

        // Invoke all callbacks
        onLoad.forEach { it.invoke(this) }
    }

    override fun onLoad(callback: (Asset) -> Unit) {
        onLoad.add(callback)
    }
}