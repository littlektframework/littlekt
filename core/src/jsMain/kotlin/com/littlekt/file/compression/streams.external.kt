package com.littlekt.file.compression

import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

external class ReadableStream(underlyingSource: UnderlyingSource = definedExternally) :
    TransformStream {
    val locked: Boolean

    fun cancel(): Promise<*>

    fun getReader(): ReadableStreamReader

    fun pipeThrough(transformStream: TransformStream): ReadableStream

    fun pipeTo(destination: WritableStream)
}

external class ReadableStreamController {
    fun enqueue(chunk: dynamic)

    fun close()
}

external interface UnderlyingSource {
    var start: (controller: ReadableStreamController) -> Unit
}

external class WritableStream : TransformStream {
    val locked: Boolean

    fun abort(): Promise<*>

    fun close(): Promise<*>

    fun getWriter(): WriteStreamWriter
}

external interface ReadableChunk {
    val done: Boolean
    val value: Uint8Array
}

external class ReadableStreamReader() {
    val closed: Promise<*>

    fun cancel(): Promise<*>

    fun read(): Promise<ReadableChunk>

    fun releaseLock()
}

external class WriteStreamWriter() {
    val ready: Promise<*>
    val closed: Promise<*>

    fun abort(): Promise<*>

    fun close(): Promise<*>

    fun releaseLock()

    fun write(chunk: dynamic): Promise<*>
}

open external class TransformStream {
    val readable: ReadableStream
    val writeable: WritableStream
}

external class CompressionStream(format: String) : TransformStream

external class DecompressionStream(format: String) : TransformStream
