package com.littlekt.file

import java.io.ByteArrayInputStream

actual fun ByteBuffer.toStream(): ByteSequenceStream {
    return JvmByteSequenceStream(ByteArrayInputStream(toArray()))
}