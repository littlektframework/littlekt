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
    fun readChunk(size: Int): ByteArray

    /**
     * Reads the next [Byte] and returns as an [Int].
     * @return the next byte
     */
    fun readByte(): Int


    /**
     * Reads the next [Byte] as an unsigned byte and returns as an [Int].
     * @return the next unsigned byte
     */
    fun readUByte(): Int


    /**
     * Reads the next [Short] and returns as an [Int].
     * @return the next short
     */
    fun readShort(): Int

    /**
     * Reads the next [Short] as an unsigned short and returns as an [Int].
     * @return the next unsigned short
     */
    fun readUShort(): Int

    /**
     * Reads the next [Int]
     * @return the next int
     */
    fun readInt(): Int

    /**
     * Reads the next [Int] as an unsigned int and returns as an [Int].
     * @return the next unsigned int
     */
    fun readUInt(): Int

    /**
     * Reads the next [Float].
     * @return the next float
     */
    fun readFloat(): Float

    /**
     * @return `true` if has more data to read; `false` otherwise.
     */
    fun hasRemaining(): Boolean

    /**
     * Skip the next specified amount of bytes to read
     * @param amount the amount of bytes to skip
     */
    fun skip(amount: Int)

    /**
     * Resets the chunk iterator back to the beginning.
     */
    fun reset()

    /**
     * Closes the stream and prevents it from being read again.
     */
    fun close()

}

expect fun ByteSequenceStream(data: ByteBuffer, offset: Int): ByteSequenceStream