package com.littlekt.file

import com.littlekt.AndroidContext
import com.littlekt.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

internal class AndroidApplicationVfs(androidContext: AndroidContext, logger: Logger) :
    LocalVfs(androidContext, logger, baseDir = "") {

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        return LoadedRawAsset(rawRef, internalFileInputStream(rawRef.url)?.let {
            ByteBufferImpl(it.readBytes())
        })
    }

    override suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset {
        return SequenceStreamCreatedAsset(sequenceRef, internalFileInputStream(sequenceRef.url)?.let {
            JvmByteSequenceStream(it)
        })
    }

    private suspend fun internalFileInputStream(url: String) = withContext(Dispatchers.IO) {
        try {
            FileInputStream(File((context as AndroidContext).androidContext.filesDir, url))
        } catch (e: Exception) {
            logger.error { "Failed loading asset $url: $e" }
            null
        }
    }
}