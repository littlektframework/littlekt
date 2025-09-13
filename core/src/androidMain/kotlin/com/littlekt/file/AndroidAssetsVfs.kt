package com.littlekt.file

import com.littlekt.AndroidContext
import com.littlekt.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidAssetsVfs(context: AndroidContext, logger: Logger) : LocalVfs(context, logger, baseDir = "") {

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        return LoadedRawAsset(rawRef, assetInputStream(rawRef.url)?.let {
            ByteBufferImpl(it.readBytes())
        })
    }

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        return SequenceStreamCreatedAsset(sequenceRef, assetInputStream(sequenceRef.url)?.let {
            JvmByteSequenceStream(it)
        })
    }

    private suspend fun assetInputStream(url: String) = withContext(Dispatchers.IO) {
        try {
            (context as AndroidContext).androidContext.assets.open(url)
        } catch (e: Exception) {
            logger.error { "Failed loading asset $url: $e" }
            null
        }
    }
}