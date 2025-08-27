package com.littlekt.file

actual fun nativeReadChunk(stream: JsByteSequenceStream, size: Int): ByteArray {
    check(!stream.closed) { "Stream already closed!" }
    val list = ByteArray(size)
    for (i in 0 until size) {
        if (stream.buffer.remaining <= 0) break
        list[i] = stream.buffer.readByte
    }
    return list
}