package com.lehaine.littlekt.file

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.log.Logger
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
class WebVfs(
    context: Context,
    logger: Logger,
    assetsBaseDir: String
) : Vfs(context, logger, assetsBaseDir) {

    override suspend fun loadRawAsset(rawRef: RawAssetRef) = LoadedRawAsset(rawRef, loadRaw(rawRef.url))
    override suspend fun loadSequenceStreamAsset(sequenceRef: SequenceAssetRef): SequenceStreamCreatedAsset {
        val buffer = loadRaw(sequenceRef.url)
        val stream = if (buffer != null) JsSequenceStream(buffer) else null
        return SequenceStreamCreatedAsset(sequenceRef, stream)
    }

    private suspend fun loadRaw(url: String): ByteBuffer? {
        val data = CompletableDeferred<ByteBuffer?>(job)
        val req = XMLHttpRequest()
        req.responseType = XMLHttpRequestResponseType.ARRAYBUFFER
        req.onload = {
            val array = Uint8Array(req.response as ArrayBuffer)
            data.complete(ByteBufferImpl(array))
        }
        req.onerror = {
            data.complete(null)
            logger.error { "Failed loading resource $url: $it" }
        }
        req.open("GET", url)
        req.send()

        return data.await()
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

    override fun load(key: String): ByteBuffer? {
        return localStorage[key]?.let { ByteBufferImpl(base64ToBin(it)) }
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