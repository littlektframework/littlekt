package com.lehaine.littlekt.file

/**
 * A buffer sequence stream to read a sequence of bytes.
 * @author Colton Daily
 * @date 1/12/2022
 */
interface ByteSequenceStream : Sequence<Byte> {

    /**
     * Reads the next chunk of the specified [size] or until it reaches the end of the [ByteSequenceStream].
     * @return a new list with the read values.
     */
    fun readChunk(size: Int): List<Byte>

    /**
     * Resets the chunk iterator back to the beginning.
     */
    fun reset()

    /**
     * Closes the stream and prevents it from being read again.
     */
    fun close()
}