package com.littlekt.file

import com.littlekt.Context
import com.littlekt.async.await
import com.littlekt.log.Logger
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Response

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
        val data = fetchData(url).map { ByteBufferImpl(Uint8Array(it.arrayBuffer().await<ArrayBuffer>())) }
        return data.getOrNull()
    }

    private suspend fun fetchData(url: String): Result<Response> {
        val response = fetch(url).await<Response>()
        return if (response.ok) {
            Result.success(response)
        } else {
            val error = "Failed loading resource: $url: ${response.status} ${response.statusText}"
            logger.error { error }
            Result.failure(IllegalStateException(error))
        }
    }
}