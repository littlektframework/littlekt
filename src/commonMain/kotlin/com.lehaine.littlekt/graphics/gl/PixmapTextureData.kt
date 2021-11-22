package com.lehaine.littlekt.graphics.gl

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.TextureData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class PixmapTextureData(override val pixmap: Pixmap, override val useMipMaps: Boolean) : TextureData {
    override val width: Int
        get() = pixmap.width
    override val height: Int
        get() = pixmap.height
    override val format: Pixmap.Format = Pixmap.Format.RGBA8888
    override val isPrepared: Boolean = true
    override val isCustom: Boolean = false

    override fun prepare() {
        throw RuntimeException("prepare()must not be called on a PixmapTextureData instance as it is already prepared.")
    }

    override fun consumePixmap(): Pixmap {
        return pixmap
    }

    override fun consumeCustomData(application:Application, target: Int) {
        throw RuntimeException("This TextureData implementation does not upload data itself")
    }
}