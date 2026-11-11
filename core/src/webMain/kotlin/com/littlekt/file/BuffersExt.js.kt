package com.littlekt.file

actual fun ByteBuffer.toStream(): ByteSequenceStream {
    val source =
        if (this is ByteBufferImpl) ByteBufferImpl(buffer.buffer.slice(0))
        else ByteBuffer(toArray()).also { it.flip() }
    return JsByteSequenceStream(source)
}
