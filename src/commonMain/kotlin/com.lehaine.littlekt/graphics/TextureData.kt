package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.GL

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
interface TextureData {
    val width: Int
    val height: Int

    val format: Pixmap.Format

    val pixmap: Pixmap
        get() {
            if (!isPrepared) {
                prepare()
            }
            return consumePixmap()
        }

    val useMipMaps: Boolean

    val isPrepared: Boolean
    val isCustom: Boolean

    fun prepare()

    fun consumePixmap(): Pixmap

    fun consumeCustomData(gl: GL, target: Int)

}

fun <T : TextureData> T.uploadImageData(gl: GL, target: Int, data: TextureData, mipLevel: Int = 0) {
    if (!data.isPrepared) {
        data.prepare()
    }

    if (data.isCustom) {
        data.consumeCustomData(gl, target)
        return
    }

    val pixmap = data.consumePixmap()
    if (data.useMipMaps) {

    } else {
        gl.texImage2D(
            target,
            mipLevel,
            pixmap.glFormat,
            pixmap.glFormat,
            pixmap.width,
            pixmap.height,
            pixmap.glType,
            pixmap.pixels
        )
    }
}