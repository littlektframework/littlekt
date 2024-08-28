package com.littlekt.file

import com.littlekt.Context
import com.littlekt.log.Logger
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Colton Daily
 * @date 8/23/2024
 */
class JvmUrlVfs(context: Context, logger: Logger) : UrlVfs(context, logger) {

    init {
        HttpCache.initCache(File(".httpCache"))
    }

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        check(!rawRef.isLocal) { "Only http resource assets may be loaded!" }
        return loadHttpRaw(rawRef)
    }

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        var sequence: ByteSequenceStream? = null

        withContext(Dispatchers.IO) {
            try {
                openLocalStream(sequenceRef.url).let { sequence = JvmByteSequenceStream(it) }
            } catch (e: Exception) {
                logger.error {
                    "Failed loading creating buffered sequence of ${sequenceRef.url}: $e"
                }
            }
        }
        return SequenceStreamCreatedAsset(sequenceRef, sequence)
    }

    private suspend fun loadHttpRaw(httpRawRef: RawAssetRef): LoadedRawAsset {
        var data: ByteBufferImpl? = null

        if (httpRawRef.url.startsWith("data:", true)) {
            data = decodeDataUrl(httpRawRef.url)
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val f =
                        HttpCache.loadHttpResource(httpRawRef.url)
                            ?: throw IOException("Failed downloading ${httpRawRef.url}")
                    runCatching { FileInputStream(f).use { data = ByteBufferImpl(it.readBytes()) } }
                } catch (e: Exception) {
                    logger.error { "Failed loading asset ${httpRawRef.url}: $e" }
                }
            }
        }
        return LoadedRawAsset(httpRawRef, data)
    }

    private fun decodeDataUrl(dataUrl: String): ByteBufferImpl {
        val dataIdx = dataUrl.indexOf(";base64,") + 8
        return ByteBufferImpl(java.util.Base64.getDecoder().decode(dataUrl.substring(dataIdx)))
    }

    private fun openLocalStream(assetPath: String): InputStream {
        var inStream = ClassLoader.getSystemResourceAsStream(assetPath)
        if (inStream == null) {
            // if asset wasn't found in resources try to load it from file system
            inStream = FileInputStream(assetPath)
        }
        return inStream
    }
}
