package com.littlekt.file

/**
 * A buffer sequence stream to read a sequence of bytes.
 *
 * @author Colton Daily
 * @date 1/12/2022
 */
interface ByteSequenceStream : Sequence<Byte> {
    /**
     * Reads the next chunk of the specified [size] or until it reaches the end of the
     * [ByteSequenceStream].
     *
     * @return a new list with the read values.
     */
    fun readChunk(size: Int): ByteArray

    /**
     * Reads the next [Byte] and returns as an [Int].
     *
     * @return the next byte
     */
    fun readByte(): Int

    /**
     * Reads the next [Byte] as an unsigned byte and returns as an [Int].
     *
     * @return the next unsigned byte
     */
    fun readUByte(): Int

    /**
     * Reads the next [Short] and returns as an [Int].
     *
     * @return the next short
     */
    fun readShort(): Int

    /**
     * Reads the next [Short] as an unsigned short and returns as an [Int].
     *
     * @return the next unsigned short
     */
    fun readUShort(): Int

    /**
     * Reads the next [Int]
     *
     * @return the next int
     */
    fun readInt(): Int

    /**
     * Reads the next [Int] as an unsigned int and returns as an [Int].
     *
     * @return the next unsigned int
     */
    fun readUInt(): Int

    /**
     * Reads the next [Float].
     *
     * @return the next float
     */
    fun readFloat(): Float

    /** @return `true` if has more data to read; `false` otherwise. */
    fun hasRemaining(): Boolean

    /**
     * Skip the next specified amount of bytes to read
     *
     * @param amount the amount of bytes to skip
     */
    fun skip(amount: Int)

    /** Resets the chunk iterator back to the beginning. */
    fun reset()

    /** Closes the stream and prevents it from being read again. */
    fun close()
}

class IndexedByteSequenceStream(val buffer: ByteBuffer, val step: Int = 0) : ByteSequenceStream {
    private var chunkPosition = 0
    private var closed = false
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
