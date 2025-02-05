package com.littlekt.file

/** Create a [ByteSequenceStream] from the underlying buffer. */
expect fun ByteBuffer.toStream(): ByteSequenceStream
