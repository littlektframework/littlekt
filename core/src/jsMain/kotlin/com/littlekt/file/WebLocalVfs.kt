package com.littlekt.file

import com.littlekt.Context
import com.littlekt.log.Logger
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class WebLocalVfs(context: Context, logger: Logger, assetsBaseDir: String) :
    LocalVfs(context, logger, assetsBaseDir) {

    override suspend fun loadRawAsset(rawRef: RawAssetRef) =
        LoadedRawAsset(rawRef, loadRaw(rawRef.url))

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        val buffer = loadRaw(sequenceRef.url)
        val stream = if (buffer != null) JsByteSequenceStream(buffer) else null
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
}
