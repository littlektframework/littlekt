package com.littlekt.file.compression

import com.littlekt.file.ByteBuffer

/**
 * Compression interface to provide basic compress/decompress functions.
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
interface Compression {

    /**
     * Compress the entire byte buffer.
     *
     * @param input uncompressed data.
     * @return compressed data ByteBuffer.
     */
    suspend fun compress(input: ByteBuffer): ByteBuffer

    /**
     * Compress a block of data
     *
     * @param input uncompressed data array
     * @return compressed data array.
     */
    suspend fun compress(input: ByteArray): ByteArray

    /**
     * Decompress the entire byte buffer.
     *
     * @param input compressed data
     * @return decompressed data ByteBuffer.
     */
    suspend fun decompress(input: ByteBuffer): ByteBuffer

    /**
     * Decompress a block of data
     *
     * @param input compressed data array
     * @return decompressed data array.
     */
    suspend fun decompress(input: ByteArray): ByteArray
}
