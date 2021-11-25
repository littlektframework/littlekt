package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebFileHandler(
    application: Application,
    logger: Logger,
    assetsBaseDir: String
) : FileHandler(application, logger, assetsBaseDir) {

    override suspend fun loadRawAsset(rawRef: RawAssetRef) = LoadedRawAsset(rawRef, loadRaw(rawRef.url))

    override suspend fun loadTextureAsset(textureRef: TextureAssetRef) =
        LoadedTextureAsset(textureRef, loadImage(textureRef))

    private suspend fun loadRaw(url: String): Uint8Buffer? {
        val data = CompletableDeferred<Uint8Buffer?>(job)
        val req = XMLHttpRequest()
        req.responseType = XMLHttpRequestResponseType.ARRAYBUFFER
        req.onload = {
            val array = Uint8Array(req.response as ArrayBuffer)
            data.complete(Uint8BufferImpl(array))
        }
        req.onerror = {
            data.complete(null)
            logger.error { "Failed loading resource $url: $it" }
        }
        req.open("GET", url)
        req.send()

        return data.await()
    }

    private suspend fun loadImage(ref: TextureAssetRef): TextureData {
        val deferred = CompletableDeferred<Image>()

        val img = Image()
        img.onload = {
            deferred.complete(img)
        }
        img.onerror = { _, _, _, _, _ ->
            if (ref.url.startsWith("data:")) {
                deferred.completeExceptionally(RuntimeException("Failed loading tex from data URL"))
            } else {
                deferred.completeExceptionally(RuntimeException("Failed loading tex from ${ref.url}"))
            }
        }
        img.crossOrigin = ""
        img.src = ref.url

        val loadedImg = deferred.await()
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = loadedImg.width
        canvas.height = loadedImg.height
        val canvasCtx = canvas.getContext("2d") as CanvasRenderingContext2D

        val w = loadedImg.width.toDouble()
        val h = loadedImg.height.toDouble()
        canvasCtx.drawImage(img, 0.0, 0.0, w, h, 0.0, 0.0, w, h)
        val pixels = Uint8BufferImpl(canvasCtx.getImageData(0.0, 0.0, w, h).data)

        val pixmap = Pixmap(loadedImg.width, loadedImg.height, pixels)
        return PixmapTextureData(pixmap, true)

    }

    override suspend fun loadTexture(assetPath: String): Texture {
        val data = loadTextureData(assetPath)
        return Texture(data).also { it.prepare(application) }
    }

    override suspend fun loadAudioClip(assetPath: String): AudioClip {
        TODO("Not yet implemented")
    }


}