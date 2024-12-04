package com.littlekt.file.compression

import com.littlekt.file.ByteBuffer

/**
 * An implementation of the gzip algorithm.
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
expect class CompressionGZIP : Compression {
    override suspend fun compress(input: ByteBuffer): ByteBuffer

    override suspend fun compress(input: ByteArray): ByteArray

    override suspend fun decompress(input: ByteBuffer): ByteBuffer

    override suspend fun decompress(input: ByteArray): ByteArray

    companion object {
        operator fun invoke(): CompressionGZIP
    }
}
