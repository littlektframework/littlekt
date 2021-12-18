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
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
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

    private val base64abc = arrayOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "/"
    )

    private fun binToBase64(data: Uint8Array): String {
        var result = ""
        val l = data.length
        var j = 2
        for (i in 2 until l step 3) {
            j = i
            result += base64abc[data[i - 2].toInt() shr 2]
            result += base64abc[data[i - 2].toInt() and 0x03 shl 4 or (data[i - 1].toInt() shr 4)]
            result += base64abc[data[i - 1].toInt() and 0x0F shl 2 or (data[i].toInt() shr 6)]
            result += base64abc[data[i].toInt() and 0x3F]
        }
        if (j == l + 1) { // 1 octet yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4]
            result += "=="
        }
        if (j == l) {  // 2 octets yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4 or (data[j - 1].toInt() shr 4)]
            result += base64abc[data[j - 1].toInt() and 0x0F shl 2]
            result += "="
        }
        return result
    }

    private fun base64ToBin(base64: String): Uint8Array {
        val binaryString = window.atob(base64)
        val bytes = Uint8Array(binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].digitToInt().toByte()
        }
        return bytes
    }
}