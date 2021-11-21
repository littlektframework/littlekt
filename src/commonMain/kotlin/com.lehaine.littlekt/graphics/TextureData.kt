package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.GL
import com.lehaine.littlekt.Platform

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

    fun consumeCustomData(application: Application, target: Int)

}

fun <T : TextureData> T.generateMipMap(
    application: Application,
    target: Int,
    pixmap: Pixmap,
    width: Int,
    height: Int,
    useHWMipMap: Boolean = true
) {
    if (!useHWMipMap) {
        generateMipMapCPU(application, target, pixmap, width, height)
    }


    when (application.platform) {
        Platform.DESKTOP -> generateMipMapDesktop(application, target, pixmap, width, height)
        Platform.JS, Platform.ANDROID, Platform.IOS -> generateMipMapGLES20(application, target, pixmap)
    }

}

private fun TextureData.generateMipMapGLES20(application: Application, target: Int, pixmap: Pixmap) {
    val gl = application.graphics.gl
    gl.texImage2D(
        target,
        0,
        pixmap.glFormat,
        pixmap.glFormat,
        pixmap.width,
        pixmap.height,
        pixmap.glType,
        pixmap.pixels
    )
    gl.generateMipmap(target)
}

private fun TextureData.generateMipMapCPU(
    application: Application,
    target: Int,
    pixmap: Pixmap,
    width: Int,
    height: Int
) {
    val gl = application.graphics.gl

    gl.texImage2D(
        target,
        0,
        pixmap.glFormat,
        pixmap.glFormat,
        pixmap.width,
        pixmap.height,
        pixmap.glType,
        pixmap.pixels
    )
    var tw = pixmap.width / 2
    var th = pixmap.height / 2
    var level = 1

    var tmp = pixmap
    while (tw > 0 && th > 0) {
        val tmp2 = Pixmap(tw, th)
        tmp2.draw(tmp, dstWidth = tw, dstHeight = th)
        tmp = tmp2
        gl.texImage2D(target, level, tmp.glFormat, tmp.glFormat, tmp.width, tmp.height, tmp.glType, tmp.pixels)

        tw = tmp.width / 2
        th = pixmap.height / 2
        level++
    }
}

private fun TextureData.generateMipMapDesktop(
    application: Application,
    target: Int,
    pixmap: Pixmap,
    width: Int,
    height: Int
) {
    val gl = application.graphics.gl

    if (application.graphics.supportsExtension("GL_ARB_framebuffer_object")
        || application.graphics.supportsExtension("GL_EXT_framebuffer_object")
        || gl.isGL32()
    ) {
        gl.texImage2D(
            target = target,
            level = 0,
            internalformat = pixmap.glFormat,
            format = pixmap.glFormat,
            width = pixmap.width,
            height = pixmap.height,
            type = pixmap.glType,
            source = pixmap.pixels
        )
        gl.generateMipmap(target)
    } else {
        generateMipMapCPU(application, target, pixmap, width, height)
    }
}


fun <T : TextureData> T.uploadImageData(application: Application, target: Int, data: TextureData, mipLevel: Int = 0) {
    val gl = application.graphics.gl
    if (!data.isPrepared) {
        data.prepare()
    }

    if (data.isCustom) {
        data.consumeCustomData(application, target)
        return
    }

    gl.pixelStorei(GL.UNPACK_ALIGNMENT, 1)
    val pixmap = data.consumePixmap()
    if (data.useMipMaps) {
        generateMipMap(application, target, pixmap, data.width, data.height)
    } else {
        gl.texImage2D(
            target = target,
            level = mipLevel,
            internalformat = pixmap.glFormat,
            format = pixmap.glFormat,
            width = pixmap.width,
            height = pixmap.height,
            type = pixmap.glType,
            source = pixmap.pixels
        )
    }
}