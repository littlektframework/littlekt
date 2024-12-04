package com.littlekt.file.compression

import com.littlekt.file.ByteBuffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM/Android implementation using java's GZIP implementation.
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
actual class CompressionGZIP : Compression {
    actual override suspend fun compress(input: ByteBuffer): ByteBuffer =
        ByteBuffer(compress(input.toArray()))

    actual override suspend fun compress(input: ByteArray): ByteArray {
        val compressedData =
            withContext(Dispatchers.IO) {
                ByteArrayOutputStream(input.size).use { outputStream ->
                    GZIPOutputStream(outputStream).use { gzipStream -> gzipStream.write(input) }
                    outputStream.toByteArray()
                }
            }
        return compressedData
    }

    actual override suspend fun decompress(input: ByteBuffer): ByteBuffer =
        ByteBuffer(decompress(input.toArray()))

    actual override suspend fun decompress(input: ByteArray): ByteArray {
        val uncompressData =
            withContext(Dispatchers.IO) {
                ByteArrayInputStream(input).use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream -> gzipStream.readBytes() }
                }
            }
        return uncompressData
    }

    actual companion object {
        actual operator fun invoke(): CompressionGZIP = CompressionGZIP()
    }
}
