package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.gl.TextureTarget

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

    fun consumeCustomData(context: Context, target: TextureTarget)

}

fun <T : TextureData> T.generateMipMap(
    context: Context,
    target: TextureTarget,
    pixmap: Pixmap,
    width: Int,
    height: Int,
    useHWMipMap: Boolean = true
) {
    if (!useHWMipMap) {
        generateMipMapCPU(context, target, pixmap, width, height)
    }

    when (context.platform) {
        Context.Platform.DESKTOP -> generateMipMapDesktop(context, target, pixmap, width, height)
        Context.Platform.JS, Context.Platform.ANDROID, Context.Platform.IOS -> generateMipMapGLES20(
            context,
            target,
            pixmap
        )
    }

}

private fun TextureData.generateMipMapGLES20(context: Context, target: TextureTarget, pixmap: Pixmap) {
    val gl = context.graphics.gl
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
    context: Context,
    target: TextureTarget,
    pixmap: Pixmap,
    width: Int,
    height: Int
) {
    val gl = context.graphics.gl

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
    context: Context,
    target: TextureTarget,
    pixmap: Pixmap,
    width: Int,
    height: Int
) {
    val gl = context.graphics.gl

    if (context.graphics.supportsExtension("GL_ARB_framebuffer_object")
        || context.graphics.supportsExtension("GL_EXT_framebuffer_object")
        || context.graphics.isGL30OrHigher()
    ) {
        gl.texImage2D(
            target = target,
            level = 0,
            internalFormat = pixmap.glFormat,
            format = pixmap.glFormat,
            width = pixmap.width,
            height = pixmap.height,
            type = pixmap.glType,
            source = pixmap.pixels
        )
        gl.generateMipmap(target)
    } else {
        generateMipMapCPU(context, target, pixmap, width, height)
    }
}


fun <T : TextureData> T.uploadImageData(
    context: Context,
    target: TextureTarget,
    data: TextureData,
    mipLevel: Int = 0
) {
    val gl = context.graphics.gl
    if (!data.isPrepared) {
        data.prepare()
    }

    if (data.isCustom) {
        data.consumeCustomData(context, target)
        return
    }

    gl.pixelStorei(GL.UNPACK_ALIGNMENT, 1)
    val pixmap = data.consumePixmap()
    if (data.useMipMaps) {
        generateMipMap(context, target, pixmap, data.width, data.height)
    } else {
        gl.texImage2D(
            target = target,
            level = mipLevel,
            internalFormat = pixmap.glFormat,
            format = pixmap.glFormat,
            width = pixmap.width,
            height = pixmap.height,
            type = pixmap.glType,
            source = pixmap.pixels
        )
    }
}