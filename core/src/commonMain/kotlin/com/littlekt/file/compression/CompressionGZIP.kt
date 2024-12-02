package com.littlekt.file.compression

/**
 * An implementation of the gzip algorithm.
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
expect class CompressionGZIP : Compression {
    companion object {
        operator fun invoke(): CompressionGZIP
    }
}
