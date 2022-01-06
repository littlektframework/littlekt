package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.WebAudioClip
import com.lehaine.littlekt.file.ByteBufferImpl
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image

/**
 * @author Colton Daily
 * @date 12/20/2021
 */

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(minFilter: TexMinFilter, magFilter: TexMagFilter, mipmaps: Boolean): Texture {
    val data = PixmapTextureData(readPixmap(), mipmaps)
    return Texture(data).also {
        it.minFilter = minFilter
        it.magFilter = magFilter
        it.prepare(vfs.context)
    }
}

/**
 * Loads an image from the path as a [Pixmap].
 * @return the loaded texture
 */
actual suspend fun VfsFile.readPixmap(): Pixmap {
    val deferred = CompletableDeferred<Image>()

    val img = Image()
    img.onload = {
        deferred.complete(img)
    }
    img.onerror = { _, _, _, _, _ ->
        if (path.startsWith("data:")) {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from data URL"))
        } else {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from ${path}"))
        }
    }
    img.crossOrigin = ""
    img.src = path

    val loadedImg = deferred.await()
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = loadedImg.width
    canvas.height = loadedImg.height
    val canvasCtx = canvas.getContext("2d") as CanvasRenderingContext2D

    val w = loadedImg.width.toDouble()
    val h = loadedImg.height.toDouble()
    canvasCtx.drawImage(img, 0.0, 0.0, w, h, 0.0, 0.0, w, h)
    val pixels = ByteBufferImpl(canvasCtx.getImageData(0.0, 0.0, w, h).data)

    return Pixmap(loadedImg.width, loadedImg.height, pixels)
}

/**
 * Loads audio from the path as an [AudioClip].
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    return if (isHttpUrl()) {
        WebAudioClip(path)
    } else {
        WebAudioClip("${vfs.baseDir}/$path")
    }
}


actual suspend fun VfsFile.writePixmap(pixmap: Pixmap) {
    TODO("IMPLEMENT ME")
}