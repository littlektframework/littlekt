package com.lehaine.littlekt.file

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebFileHandler(
    context: Context,
    logger: Logger,
    assetsBaseDir: String
) : FileHandler(context, logger, assetsBaseDir) {

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
        val pixels = MixedBufferImpl(canvasCtx.getImageData(0.0, 0.0, w, h).data)

        val pixmap = Pixmap(loadedImg.width, loadedImg.height, pixels)
        return PixmapTextureData(pixmap, true)

    }

    override suspend fun loadTexture(assetPath: String): Texture {
        val data = loadTextureData(assetPath)
        return Texture(data).also { it.prepare(context) }
    }

    override suspend fun loadAudioClip(assetPath: String): AudioClip {
        TODO("Not yet implemented")
    }

    override fun store(key: String, data: ByteArray): Boolean {
        return try {
            localStorage[key] = binToBase64(Uint8Array(data.toTypedArray()))
            true
        } catch (e: Exception) {
            logger.error { "Failed storing data '$key' to localStorage: $e" }
            false
        }
    }

    override fun store(key: String, data: String): Boolean {
        return try {
            localStorage[key] = data
            true
        } catch (e: Exception) {
            logger.error { "Failed storing string '$key' to localStorage: $e" }
            false
        }
    }

    override fun load(key: String): Uint8Buffer? {
        return localStorage[key]?.let { Uint8BufferImpl(base64ToBin(it)) }
    }

    override fun loadString(key: String): String? {
        return localStorage[key]
    }

    /**
     * Cumbersome / ugly method to convert Uint8Array into a base64 string in javascript
     */
    @Suppress("UNUSED_PARAMETER")
    private fun binToBase64(uint8Data: Uint8Array): String = js(
        """
        var chunkSize = 0x8000;
        var c = [];
        for (var i = 0; i < uint8Data.length; i += chunkSize) {
            c.push(String.fromCharCode.apply(null, uint8Data.subarray(i, i+chunkSize)));
        }
        return window.btoa(c.join(""));
    """
    ) as String

    @Suppress("UNUSED_PARAMETER")
    private fun base64ToBin(base64: String): Uint8Array = js(
        """
        var binary_string = window.atob(base64);
        var len = binary_string.length;
        var bytes = new Uint8Array(len);
        for (var i = 0; i < len; i++) {
            bytes[i] = binary_string.charCodeAt(i);
        }
        return bytes;
    """
    ) as Uint8Array
}