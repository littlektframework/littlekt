package com.littlekt.file.vfs

import com.littlekt.async.await
import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.audio.WebAudioClip
import com.littlekt.audio.WebAudioStream
import com.littlekt.file.ByteBufferImpl
import com.littlekt.file.createImageBitmap
import com.littlekt.file.fetch
import com.littlekt.graphics.LazyTexture
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.ImageBitmapTexture
import com.littlekt.graphics.webgpu.Device
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.*
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.JsAny
import kotlin.js.JsArray

/**
 * Loads an image from the path as a [Texture].
 *
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(options: TextureOptions): Texture {
    val bitmap = readImageBitmap()
    return ImageBitmapTexture(
        vfs.context.graphics.device,
        options.format,
        bitmap,
        if (options.generateMipMaps) Texture.calculateNumMips(bitmap.width, bitmap.height) else 1,
        options.samplerDescriptor,
    )
}

private suspend fun fetchData(url: String): Result<Response> {
    val response = fetch(url).await<Response>()
    return if (response.ok) {
        Result.success(response)
    } else {
        val error = "Failed loading resource: $url: ${response.status} ${response.statusText}"
        Result.failure(IllegalStateException(error))
    }
}
expect fun encodeByteArrayToBase64(array: ByteArray) : String
/**
 * Decodes PNG or JPEG bytes, or any other image format the browser can decode, into a [Pixmap] of
 * RGBA pixels.
 *
 * @return the decoded pixmap
 */
actual suspend fun ByteArray.readPixmap(): Pixmap {
    val path = "data:${imageMimeType()};base64,${encodeByteArrayToBase64(this)}"

    return readPixmap(path)
}

/**
 * Decodes PNG or JPEG bytes, or any other image format the browser can decode, into a [Texture].
 *
 * @param device the device to create the texture with
 * @param options additional [TextureOptions] such as generating mips or passing along a
 *   [com.littlekt.graphics.webgpu.SamplerDescriptor].
 * @return the decoded texture
 */
actual suspend fun ByteArray.readTexture(device: Device, options: TextureOptions): Texture {
    val array = arrayOf<JsAny?>(com.littlekt.file.compression.toUint8Array(this))
    val blob = Blob(toJsArray(array), BlobPropertyBag(imageMimeType()))
    val bitmap = createImageBitmap(blob).await<ImageBitmap>()
    return ImageBitmapTexture(
        device,
        options.format,
        bitmap,
        if (options.generateMipMaps) Texture.calculateNumMips(bitmap.width, bitmap.height) else 1,
        options.samplerDescriptor,
    )
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
        null
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
    val pixels = ByteBufferImpl(canvasCtx.getImageData(0.0, 0.0, w, h).data).apply { filled() }

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

actual suspend fun VfsFile.readImageData(): LazyTexture.ImageData<*> =
    LazyTexture.ImageData(readImageBitmap())

private suspend fun VfsFile.readImageBitmap(): ImageBitmap {
    val response = fetchData(path).getOrThrow()
    val blob = response.blob().await<Blob>()
    val bitmap =
        createImageBitmap(blob)
            .await<ImageBitmap>()
    return bitmap
}

internal actual suspend fun ByteArray.readImageData(mimeType: String?): LazyTexture.ImageData<*> {
    val array = arrayOf<JsAny?>(com.littlekt.file.compression.toUint8Array(this))
    val blobParts = toJsArray(array)
    val blob = Blob(blobParts, BlobPropertyBag(mimeType ?: ""))
    val bitmap = createImageBitmap(blob).await<ImageBitmap>()
    return LazyTexture.ImageData(bitmap)
}

expect fun toJsArray(array: Array<JsAny?>): JsArray<JsAny?>