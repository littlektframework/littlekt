package com.littlekt.file.compression

import com.littlekt.file.ByteBuffer
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * JS gzip implementation using the new Streams API
 *
 * @author Colton Daily
 * @date 12/2/2024
 */
actual class CompressionGZIP : Compression {
    actual override suspend fun compress(input: ByteBuffer): ByteBuffer =
        ByteBuffer(compress(input.toArray()))

    actual override suspend fun compress(input: ByteArray): ByteArray {
        val uint8Array = Uint8Array(input.toTypedArray())
        val readableStream =
            ReadableStream(
                object : UnderlyingSource {
                    override var start: (controller: ReadableStreamController) -> Unit =
                        { controller ->
                            controller.enqueue(uint8Array)
                            controller.close()
                        }
                }
            )
        val compressedStream = readableStream.pipeThrough(CompressionStream("gzip"))
        val reader = compressedStream.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return ByteArray(output.length) { output[it] }
    }

    actual override suspend fun decompress(input: ByteBuffer): ByteBuffer =
        ByteBuffer(decompress(input.toArray()))

    actual override suspend fun decompress(input: ByteArray): ByteArray {
        val uint8Array = Uint8Array(input.toTypedArray())
        val readableStream =
            ReadableStream(
                object : UnderlyingSource {
                    override var start: (controller: ReadableStreamController) -> Unit =
                        { controller ->
                            controller.enqueue(uint8Array)
                            controller.close()
                        }
                }
            )
        val decompressedStream = readableStream.pipeThrough(DecompressionStream("gzip"))
        val reader = decompressedStream.getReader()
        var output = Uint8Array(0)
        while (true) {
            val chunk = reader.read().await()
            if (chunk.done) break
            val temp = Uint8Array(output.length + chunk.value.length)
            temp.set(output)
            temp.set(chunk.value, output.length)
            output = temp
        }
        return ByteArray(output.length) { output[it] }
    }

    actual companion object {
        actual operator fun invoke(): CompressionGZIP = CompressionGZIP()
    }
}
