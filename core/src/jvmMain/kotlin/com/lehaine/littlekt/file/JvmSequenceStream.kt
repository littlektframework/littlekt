package com.lehaine.littlekt.file

import java.io.InputStream

/**
 * @author Colton Daily
 * @date 1/12/2022
 */
class JvmSequenceStream(private val stream: InputStream) : SequenceStream {

    override fun iterator(): Iterator<Byte> {
        return stream.buffered().iterator()
    }

    override fun close() {
        stream.close()
    }
}