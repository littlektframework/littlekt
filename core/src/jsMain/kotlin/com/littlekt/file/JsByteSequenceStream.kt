package com.littlekt.file

/**
 * @param buffer assumed [ByteBuffer] where the position is set to be read from until the limit.
 * @author Colton Daily
 * @date 1/12/2022
 */
class JsByteSequenceStream(val buffer: ByteBuffer) : ByteSequenceStream {
    private var closed = false

    override fun iterator(): Iterator<Byte> {
        check(!closed) { "Stream already closed!" }

        val buffer = ByteBuffer(buffer.toArray())
        return object : ByteIterator() {
            override fun hasNext(): Boolean {
                return buffer.remaining > 0
            }

            override fun nextByte(): Byte {
                return buffer.readByte
            }
        }
    }

    override fun readChunk(size: Int): ByteArray {
        check(!closed) { "Stream already closed!" }
        val list = ByteArray(size)
        for (i in 0 until size) {
            if (buffer.remaining <= 0) break
            list[i] = buffer.readByte
        }
        return list
    }

    override fun readByte(): Int {
        return buffer.readByte.toInt()
    }

    override fun readUByte(): Int {
        return buffer.readUByte.toInt()
    }

    override fun readShort(): Int {
        return buffer.readShort.toInt()
    }

    override fun readUShort(): Int {
        return buffer.readUShort.toInt()
    }

    override fun readInt(): Int {
        return buffer.readInt
    }

    override fun readUInt(): Int {
        return buffer.readUInt
    }

    override fun readFloat(): Float {
        return Float.fromBits(readUInt())
    }

    override fun hasRemaining(): Boolean {
        return buffer.remaining > 0
    }

    override fun skip(amount: Int) {
        buffer.position += amount
    }

    override fun reset() {
        check(!closed) { "Stream already closed!" }
        buffer.flip()
    }

    override fun close() {
        check(!closed) { "Stream already closed!" }
        closed = true
        buffer.clear()
    }
}
