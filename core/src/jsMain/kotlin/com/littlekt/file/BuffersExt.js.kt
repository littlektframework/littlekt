package com.littlekt.file

actual fun ByteBuffer.toStream(): ByteSequenceStream {
    return JsByteSequenceStream(this)
}
