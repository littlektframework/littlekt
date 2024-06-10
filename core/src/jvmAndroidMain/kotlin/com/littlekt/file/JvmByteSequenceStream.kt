package com.littlekt.file

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

    override fun readByte(): Int {
        return chunkIterator.nextByte().toInt()
    }

    override fun readUByte(): Int {
        return chunkIterator.nextByte().toInt() and 0xff
    }

    override fun readShort(): Int {
        var s = readUShort()
        if (s > 32767) {
            s -= 65536
        }
        return s
    }

    override fun readUShort(): Int {
        var d = 0
        for (i in 0..1) {
            d = d or (readUByte() shl (i * 8))
        }
        return d
    }

    override fun readInt(): Int = readUInt()

    override fun readUInt(): Int {
        var d = 0
        for (i in 0..3) {
            d = d or (readUByte() shl (i * 8))
        }
        return d
    }

    override fun readFloat(): Float {
        return Float.fromBits(readUInt())
    }

    override fun readChunk(size: Int): ByteArray {
        val list = ByteArray(size)
        var i = 0
        while (i < size && chunkIterator.hasNext()) {
            list[i] = chunkIterator.nextByte()
            i++
        }
        if (i < size && chunkIterator.hasNext()) {
            throw IllegalStateException(
                "Attempt to read a chunk of $size but was only able to read a size of $i!"
            )
        }
        return list
    }

    override fun hasRemaining(): Boolean {
        return chunkIterator.hasNext()
    }

    override fun skip(amount: Int) {
        repeat(amount) { chunkIterator.nextByte() }
    }

    override fun reset() {
        chunkIterator = bufferedStream.iterator()
    }

    override fun close() {
        stream.close()
        bufferedStream.close()
    }
}
