package com.littlekt.file

/**
 * A buffer sequence stream that can be read directly by the corresponding index.
 */
class IndexedByteSequenceStream(val buffer: ByteBuffer, val step: Int = 0) : ByteSequenceStream {
    private var chunkPosition = 0
    private var closed = false

    /**
     * The current index of the stream. This may be changed directly to set the position in the stream. This value
     * is incremented when calling any of the 'read' functions.
     */
    var index: Int = 0

    init {
        require(step >= 0) { "step must be >= 0!" }
    }

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
        val prevPos = buffer.position
        buffer.position = chunkPosition
        val list = ByteArray(size)
        for (i in 0 until size) {
            if (buffer.remaining <= 0) break
            list[i] = buffer[index++]
        }
        chunkPosition = buffer.position
        buffer.position = prevPos
        return list
    }

    override fun readByte(): Int {
        return buffer[step + index++].toInt()
    }

    override fun readUByte(): Int {
        return buffer[step + index++].toInt() and 0xff
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

    override fun readInt(): Int {
        return readUInt()
    }

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

    override fun hasRemaining(): Boolean {
        return buffer.remaining > 0
    }

    override fun skip(amount: Int) {
        index += amount
    }

    override fun reset() {
        check(!closed) { "Stream already closed!" }
        buffer.flip()
        chunkPosition = 0
        index = 0
    }

    override fun close() {
        check(!closed) { "Stream already closed!" }
        closed = true
        buffer.clear()
        index = 0
    }
}