package com.lehaine.littlekt.file

import java.io.InputStream

/**
 * @author Colton Daily
 * @date 1/12/2022
 */
class JvmByteSequenceStream(val stream: InputStream) : ByteSequenceStream {
    private val bufferedStream = stream.buffered()
    private var chunkIterator = bufferedStream.iterator()

    override fun iterator(): Iterator<Byte> {
        return bufferedStream.iterator()
    }

    override fun readChunk(size: Int): List<Byte> {
        val list = mutableListOf<Byte>()
        var i = 0
        while (i < size && chunkIterator.hasNext()) {
            list += chunkIterator.nextByte()
            i++
        }
        return list
    }

    override fun reset() {
        chunkIterator = bufferedStream.iterator()
    }

    override fun close() {
        stream.close()
        bufferedStream.close()
    }
}