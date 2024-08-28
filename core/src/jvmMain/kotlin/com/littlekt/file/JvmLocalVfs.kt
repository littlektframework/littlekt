package com.littlekt.file

import com.littlekt.Context
import com.littlekt.log.Logger
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Colton Daily
 * @date 8/23/2024
 */
abstract class JvmLocalVfs(context: Context, logger: Logger, baseDir: String) :
    LocalVfs(context, logger, baseDir) {

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        check(rawRef.isLocal) { "Only local resource assets may be loaded!" }
        return loadLocalRaw(rawRef)
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

    private suspend fun loadLocalRaw(localRawRef: RawAssetRef): LoadedRawAsset {
        var data: ByteBufferImpl? = null
        withContext(Dispatchers.IO) {
            try {
                openLocalStream(localRawRef.url).use { data = ByteBufferImpl(it.readBytes()) }
            } catch (e: Exception) {
                logger.error { "Failed loading asset ${localRawRef.url}: $e" }
            }
        }
        return LoadedRawAsset(localRawRef, data)
    }

    protected abstract fun openLocalStream(assetPath: String): InputStream
}
