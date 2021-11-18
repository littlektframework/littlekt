package com.lehaine.littlekt.graphics.gl

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.TextureData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class GLTextureData(
    override val width: Int,
    override val height: Int,
    val mipMapLevel: Int,
    val internalGlFormat: Int,
    val glFormat: Int,
    val type: Int
) : TextureData {
    override val format: Pixmap.Format = Pixmap.Format.RGBA8888
    override val useMipMaps: Boolean = false
    override var isPrepared: Boolean = false
    override val isCustom: Boolean = true

    override fun prepare() {
        if (!isPrepared) {
            throw RuntimeException("Already prepared!")
        }
        isPrepared = true
    }

    override fun consumePixmap(): Pixmap {
        throw RuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun consumeCustomData(application: Application, target: Int) {
        val gl = application.graphics.gl
        gl.texImage2D(target, mipMapLevel, internalGlFormat, glFormat, type, width, height, byteArrayOf())
    }


}