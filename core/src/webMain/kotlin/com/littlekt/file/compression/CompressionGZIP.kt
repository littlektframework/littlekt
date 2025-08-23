package com.littlekt.file.compression

import com.littlekt.async.await
import com.littlekt.file.ByteBuffer
import com.littlekt.file.ByteBufferImpl
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.Uint8Array

/**
 * JS gzip implementation using the new Streams API
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
expect fun toUint8Array(input: ByteArray): Uint8Array
expect fun toUint16Array(input: ShortArray): Uint16Array
expect fun toUint32Array(input: IntArray): Uint32Array
expect fun toFloat32Array(input: FloatArray): Float32Array

actual class CompressionGZIP : Compression {
    actual override suspend fun compress(input: ByteBuffer): ByteBuffer {
        input as ByteBufferImpl
        val buffer = input.buffer.buffer
        val compressionStream = CompressionStream("gzip")
        val writer = compressionStream.writable.getWriter()
        writer.write(buffer)
        writer.close()
        val reader = compressionStream.readable.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await<ReadableChunk>()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return ByteBufferImpl(output)
    }

    actual override suspend fun compress(input: ByteArray): ByteArray {
        val buffer = toUint8Array(input)
        val compressionStream = CompressionStream("gzip")
        val writer = compressionStream.writable.getWriter()
        writer.write(buffer)
        writer.close()
        val reader = compressionStream.readable.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await<ReadableChunk>()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return nativeByteArray(output)
    }

    actual override suspend fun decompress(input: ByteBuffer): ByteBuffer {
        input as ByteBufferImpl
        val buffer = input.buffer.buffer
        val decompressedStream = DecompressionStream("gzip")
        val writer = decompressedStream.writable.getWriter()
        writer.write(buffer)
        writer.close()
        val reader = decompressedStream.readable.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await<ReadableChunk>()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return ByteBufferImpl(output)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    actual override suspend fun decompress(input: ByteArray): ByteArray {
        val buffer = toUint8Array(input).buffer
        val decompressedStream = DecompressionStream("gzip")
        val writer = decompressedStream.writable.getWriter()
        writer.write(buffer)
        writer.close()
        val reader = decompressedStream.readable.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await<ReadableChunk>()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return nativeByteArray(output)
    }

    actual companion object {
        actual operator fun invoke(): CompressionGZIP = CompressionGZIP()
    }
}

expect fun nativeByteArray(output: Uint8Array): ByteArray