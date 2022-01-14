package com.lehaine.littlekt.file

/**
 * @author Colton Daily
 * @date 1/12/2022
 */
class JsByteSequenceStream(val buffer: ByteBuffer) : ByteSequenceStream {
    private var chunkPosition = 0
    private var closed = false

    init {
        buffer.flip()
    }

    override fun iterator(): Iterator<Byte> {
        check(!closed) { "Stream already closed!" }

        val buffer = createByteBuffer(buffer.toArray())
        return object : ByteIterator() {
            override fun hasNext(): Boolean {
                return buffer.remaining > 0
            }

            override fun nextByte(): Byte {
                return buffer.readByte
            }
        }
    }

    override fun readChunk(size: Int): List<Byte> {
        check(!closed) { "Stream already closed!" }
        val prevPos = buffer.position
        buffer.position = chunkPosition
        val list = mutableListOf<Byte>()
        for (i in 0 until size) {
            if (buffer.remaining <= 0) break
            list += buffer.readByte
        }
        chunkPosition = buffer.position
        buffer.position = prevPos
        return list
    }

    override fun reset() {
        check(!closed) { "Stream already closed!" }
        buffer.flip()
        chunkPosition = 0
    }

    override fun close() {
        check(!closed) { "Stream already closed!" }
        closed = true
        buffer.clear()
    }
}