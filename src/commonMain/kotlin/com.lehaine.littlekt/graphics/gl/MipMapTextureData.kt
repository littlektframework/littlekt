package com.lehaine.littlekt.graphics.gl

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.uploadImageData

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class MipMapTextureData(vararg mipMapData: TextureData) : TextureData {
    val mips = arrayOf(*mipMapData)

    override val format: Pixmap.Format = mips[0].format
    override val width: Int = mips[0].width
    override val height: Int = mips[0].height
    override val useMipMaps: Boolean = false
    override val isPrepared: Boolean = true
    override val isCustom: Boolean = true

    override fun prepare() = Unit

    override fun consumePixmap(): Pixmap {
        throw RuntimeException("It's compressed, use the compressed method")
    }

    override fun consumeCustomData(application: Application, target: Int) {
        mips.forEachIndexed { index, mip ->
            uploadImageData(application, target, mip, index)
        }
    }
}