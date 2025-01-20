package com.littlekt.file.vfs

import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.audio.WebAudioClip
import com.littlekt.audio.WebAudioStream
import com.littlekt.file.Base64.encodeToBase64
import com.littlekt.file.ByteBufferImpl
import com.littlekt.file.createImageBitmap
import com.littlekt.file.fetch
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.ImageBitmapTexture
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.await
import org.w3c.dom.*
import org.w3c.fetch.Response

/**
 * Loads an image from the path as a [Texture].
 *
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(options: TextureOptions): Texture {
    val response = fetchData(path).getOrThrow()
    val blob = response.blob().await()
    val bitmap =
        createImageBitmap(blob, ImageBitmapOptions(premultiplyAlpha = PremultiplyAlpha.NONE))
            .await()
    return ImageBitmapTexture(
        vfs.context.graphics.device,
        options.format,
        bitmap,
        if (options.generateMipMaps) Texture.calculateNumMips(bitmap.width, bitmap.height) else 1,
        options.samplerDescriptor,
    )
}

private suspend fun fetchData(url: String): Result<Response> {
    val response = fetch(url).await()
    return if (response.ok) {
        Result.success(response)
    } else {
        val error = "Failed loading resource: $url: ${response.status} ${response.statusText}"
        Result.failure(IllegalStateException(error))
    }
}

/** Reads Base64 encoded ByteArray for embedded images. */
internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    val path = "data:image/png;base64,${encodeToBase64()}"

    return readPixmap(path)
}

/**
 * Loads an image from the path as a [Pixmap].
 *
 * @return the loaded texture
 */
actual suspend fun VfsFile.readPixmap(): Pixmap {
    return readPixmap(path)
}

private suspend fun readPixmap(path: String): Pixmap {
    val deferred = CompletableDeferred<Image>()

    val img = Image()
    img.onload = { deferred.complete(img) }
    img.onerror = { _, _, _, _, _ ->
        if (path.startsWith("data:")) {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from data URL"))
        } else {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from $path"))
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
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    return if (isHttpUrl()) {
        WebAudioClip(path)
    } else {
        WebAudioClip("${vfs.baseDir}/$path")
    }
}

/**
 * Streams audio from the path as an [AudioStream].
 *
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    return if (isHttpUrl()) {
        WebAudioStream(path)
    } else {
        WebAudioStream("${vfs.baseDir}/$path")
    }
}
