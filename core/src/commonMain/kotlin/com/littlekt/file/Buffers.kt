package com.littlekt.file

/** A container for data of a specific primitive type. */
interface Buffer {
    /** The buffer's limit. */
    var limit: Int

    /** The buffer's position. */
    var position: Int

    /** The amount of remaining elements between the current [position] and the [limit]. */
    val remaining: Int

    /** The capacity of this [Buffer]. */
    val capacity: Int

    /**
     * If this buffer has been changed since this was last set to false. This field may be changed
     * to false once checked, if needed.
     */
    var dirty: Boolean

    /**
     * Flip this buffer. The limit is set to the current position and then the position is set to
     * zero. After a sequence of channel-read or put operations, invoke this method to prepare for a
     * sequence of channel-write or relative get operations.
     */
    fun flip()

    /**
     * Clears this buffer. The position is set to zero, the limit is set to the capacity. Invoke
     * this method before using a sequence of channel-read or put operations to fill this buffer.
     * This method does not actually erase the data in the buffer, but it is named as if it did
     * because it will most often be used in situations in which that might as well be the case.
     */
    fun clear()

    /**
     * If this [position] is greater than the given [index] then the position will decrement by 1.
     * If this [limit] is greater than the given [index] then the limit will decrement by 1. This
     * method does not actually remove an element in the buffer.
     */
    fun removeAt(index: Int) {
        if (position > index) {
            position--
        }
        if (limit > index) {
            limit--
        }
    }
}

/** A [Short] buffer. */
interface ShortBuffer : Buffer {
    /** The capacity of this buffer in shorts. */
    override val capacity: Int

    operator fun get(i: Int): Short

    operator fun set(i: Int, value: Short)

    operator fun plusAssign(value: Short) {
        put(value)
    }

    fun put(value: Short): ShortBuffer

    fun put(data: ShortArray, srcOffset: Int = 0, len: Int = data.size - srcOffset): ShortBuffer

    fun put(
        data: ShortArray,
        dstOffset: Int,
        srcOffset: Int = 0,
        len: Int = data.size - srcOffset
    ): ShortBuffer

    fun put(data: ShortBuffer): ShortBuffer

    fun toArray(offset: Int = 0, len: Int = capacity - offset) =
        ShortArray(len) { get(it + offset) }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/** An [Int] buffer. */
interface IntBuffer : Buffer {
    /** The capacity of this buffer in ints. */
    override val capacity: Int

    operator fun get(i: Int): Int

    operator fun set(i: Int, value: Int)

    operator fun plusAssign(value: Int) {
        put(value)
    }

    fun put(value: Int): IntBuffer

    fun put(data: IntArray, srcOffset: Int = 0, len: Int = data.size - srcOffset): IntBuffer

    fun put(
        data: IntArray,
        dstOffset: Int,
        srcOffset: Int = 0,
        len: Int = data.size - srcOffset
    ): IntBuffer

    fun put(data: IntBuffer): IntBuffer

    fun toArray(offset: Int = 0, len: Int = capacity - offset) = IntArray(len) { get(it + offset) }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/** A [Float] buffer. */
interface FloatBuffer : Buffer {
    /** The capacity of this buffer in floats. */
    override val capacity: Int

    operator fun get(i: Int): Float

    operator fun set(i: Int, value: Float)

    operator fun plusAssign(value: Float) {
        put(value)
    }

    fun put(value: Float): FloatBuffer

    fun put(data: FloatArray, srcOffset: Int = 0, len: Int = data.size - srcOffset): FloatBuffer

    fun put(
        data: FloatArray,
        dstOffset: Int,
        srcOffset: Int = 0,
        len: Int = data.size - srcOffset
    ): FloatBuffer

    fun put(data: FloatBuffer): FloatBuffer

    fun toArray(offset: Int = 0, len: Int = capacity - offset) =
        FloatArray(len) { get(it + offset) }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/** A [Byte] buffer for mixed types. All buffer positions are in bytes. */
interface ByteBuffer : Buffer {
    /** The capacity of this buffer in bytess. */
    override val capacity: Int

    operator fun get(i: Int): Byte

    operator fun set(i: Int, value: Byte)

    operator fun set(i: Int, value: Int)

    operator fun set(i: Int, value: Short)

    operator fun set(i: Int, value: Float)

    operator fun plusAssign(value: Byte) {
        putByte(value)
    }

    operator fun plusAssign(value: Short) {
        putShort(value)
    }

    operator fun plusAssign(value: Int) {
        putInt(value)
    }

    operator fun plusAssign(value: Float) {
        putFloat(value)
    }

    val readByte: Byte

    fun getByte(offset: Int): Byte

    fun getByteArray(startOffset: Int, endOffset: Int): ByteArray

    fun putByte(value: Byte) = putUByte(value.toUByte())

    fun putByte(data: ByteArray) = putUByte(data)

    fun putByte(data: ByteArray, srcOffset: Int = 0, len: Int = data.size - srcOffset) =
        putUByte(data, srcOffset, len)

    fun putByte(
        data: ByteArray,
        dstOffset: Int,
        srcOffset: Int = 0,
        len: Int = data.size - srcOffset
    ) = putUByte(data, dstOffset, srcOffset, len)

    fun putByte(data: ByteBuffer) = putUByte(data)

    val readShort: Short

    fun getShort(offset: Int): Short

    fun putShort(value: Short) = putUShort(value.toUShort())

    fun putShort(data: ShortArray) = putUShort(data)

    fun putShort(data: ShortArray, offset: Int, len: Int) = putUShort(data, offset, len)

    fun putShort(data: ShortArray, offset: Int, dstOffset: Int, len: Int) =
        putUShort(data, offset, dstOffset, len)

    fun putShort(data: ShortBuffer) = putUShort(data)

    val readInt: Int

    fun getInt(offset: Int): Int

    fun putInt(offset: Int, value: Int) = putUInt(offset, value.toUInt())

    fun putInt(value: Int) = putUInt(value.toUInt())

    fun putInt(data: IntArray) = putUInt(data)

    fun putInt(data: IntArray, offset: Int, len: Int) = putUInt(data, offset, len)

    fun putInt(data: IntArray, srcOffset: Int, dstOffset: Int, len: Int) =
        putUInt(data, srcOffset, dstOffset, len)

    fun putInt(data: IntBuffer) = putUInt(data)

    val readUByte: Byte

    fun getUByte(offset: Int): UByte

    fun getUByteArray(startOffset: Int, endOffset: Int): ByteArray

    fun putUByte(value: UByte): ByteBuffer

    fun putUByte(data: ByteArray, srcOffset: Int = 0, len: Int = data.size - srcOffset): ByteBuffer

    fun putUByte(
        data: ByteArray,
        dstOffset: Int,
        srcOffset: Int = 0,
        len: Int = data.size - srcOffset
    ): ByteBuffer

    fun putUByte(data: ByteBuffer): ByteBuffer

    val readUShort: Short

    fun getUShort(offset: Int): UShort

    fun putUShort(value: UShort): ByteBuffer

    fun putUShort(data: ShortArray): ByteBuffer = putUShort(data, 0, data.size)

    fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer

    fun putUShort(data: ShortArray, offset: Int, dstOffset: Int, len: Int): ByteBuffer

    fun putUShort(data: ShortBuffer): ByteBuffer

    val readUInt: Int

    fun getUInt(offset: Int): UInt

    fun putUInt(value: UInt): ByteBuffer

    fun putUInt(offset: Int, value: UInt): ByteBuffer

    fun putUInt(data: IntArray): ByteBuffer = putUInt(data, 0, data.size)

    fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer

    fun putUInt(data: IntArray, srcOffset: Int, dstOffset: Int, len: Int): ByteBuffer

    fun putUInt(data: IntBuffer): ByteBuffer

    val readFloat: Float

    fun getFloat(offset: Int): Float

    fun putFloat(value: Float): ByteBuffer

    fun putFloat(data: FloatArray): ByteBuffer = putFloat(data, 0, data.size)

    fun putFloat(data: FloatArray, offset: Int, len: Int): ByteBuffer

    fun putFloat(data: FloatArray, offset: Int, dstOffset: Int, len: Int): ByteBuffer

    fun putFloat(data: FloatBuffer): ByteBuffer

    /** @return a 4-character tag */
    fun getString(offset: Int, length: Int): String

    /**
     * Offsets are 1 to 4 bytes in length, depending on the [offSize] param.
     *
     * @return an offset from the buffer
     */
    fun getOffset(offset: Int, offSize: Int): Int

    fun toArray(offset: Int = 0, len: Int = capacity - offset) = ByteArray(len) { get(it + offset) }

    override fun removeAt(index: Int) {
        throw RuntimeException("MixedBuffer does not support element removal")
    }
}

/**
 * Assumes the underlying [ByteArray] is in little endian order.
 */
fun ByteArray.put(value: Int, offset: Int): ByteArray {
    this[offset + 3] = (value shr 24).toByte()
    this[offset + 2] = (value shr 16).toByte()
    this[offset + 1] = (value shr 8).toByte()
    this[offset + 0] = value.toByte()
    return this
}

/**
 * Assumes the underlying [ByteArray] is in little endian order.
 */
fun ByteArray.put(value: Float, offset: Int): ByteArray {
    val bits = value.toRawBits()
    this[offset + 3] = (bits shr 24).toByte()
    this[offset + 2] = (bits shr 16).toByte()
    this[offset + 1] = (bits shr 8).toByte()
    this[offset + 0] = bits.toByte()
    return this
}

/**
 * Assumes the underlying [ByteArray] is in little endian order.
 */
fun ByteArray.put(value: Short, offset: Int): ByteArray {
    this[offset + 1] = (value.toInt() shr 8).toByte()
    this[offset + 0] = value.toByte()
    return this
}

/**
 * Converts the [FloatArray] to a little endian ordered [ByteArray].
 */
fun FloatArray.toByteArray(offset: Int = 0, size: Int = this.size): ByteArray {
    val floatArray = this
    val bytes = ByteArray(size * 4)
    for ((index, i) in (offset until size).withIndex()) {
        val float = floatArray[i]
        val arrIdx = index * 4
        bytes.put(float, arrIdx)
    }

    return bytes
}

/**
 * Converts the [ByteArray] to a little endian [FloatArray].
 */
fun ByteArray.toFloatArray(): FloatArray {
    val byteArray = this
    val floats = FloatArray(byteArray.size / 4)

    for (i in floats.indices) {
        val byteI = i * 4
        val bits =
            (byteArray[byteI].toInt() and 0xFF) or
                    ((byteArray[byteI + 1].toInt() and 0xFF) shl 8) or
                    ((byteArray[byteI + 2].toInt() and 0xFF) shl 16) or
                    ((byteArray[byteI + 3].toInt() and 0xFF) shl 24)

        floats[i] = Float.fromBits(bits)
    }

    return floats
}

/**
 * Converts the [ByteArray] to a little endian [IntArray].
 */
fun ByteArray.toIntArray(): IntArray {
    val byteArray = this
    val ints = IntArray(byteArray.size / 4)

    for (i in ints.indices) {
        val byteI = i * 4
        ints[i] =
            byteArray[byteI].toInt() or
                    (byteArray[byteI + 1].toInt() shl 8) or
                    (byteArray[byteI + 2].toInt() shl 16) or
                    (byteArray[byteI + 3].toInt() shl 24)
    }

    return ints
}

/**
 * Converts the [IntArray] to a little endian ordered [ByteArray].
 */
fun IntArray.toByteArray(): ByteArray {
    val intArray = this
    val bytes = ByteArray(intArray.size * 4)
    intArray.forEachIndexed { index, bits ->
        val i = index * 4
        bytes.put(bits, i)
    }

    return bytes
}

/**
 * Converts the [ShortArray] to a little endian ordered [ByteArray].
 */
fun ShortArray.toByteArray(): ByteArray {
    val shortArray = this
    val bytes = ByteArray(shortArray.size * 2)
    shortArray.forEachIndexed { index, value ->
        val i = index * 2
        bytes.put(value, i)
    }

    return bytes
}

/**
 * @param capacity this buffer's capacity, in shorts.
 * @return a new [ShortBuffer] with the given [capacity].
 */
expect fun ShortBuffer(capacity: Int): ShortBuffer

/**
 * @return a new [ShortBuffer] using the given short [array] as the data and its size as the
 *   capacity.
 */
expect fun ShortBuffer(array: ShortArray): ShortBuffer

/**
 * @param capacity this buffer's capacity, in ints.
 * @return a new [IntBuffer] with the given [capacity].
 */
expect fun IntBuffer(capacity: Int): IntBuffer

/**
 * @return a new [IntBuffer] using the given int [array] as the data and its size as the capacity.
 */
expect fun IntBuffer(array: IntArray): IntBuffer

/**
 * @param capacity this buffer's capacity, in floats.
 * @return a new [FloatBuffer] with the given [capacity].
 */
expect fun FloatBuffer(capacity: Int): FloatBuffer

/**
 * @return a new [FloatBuffer] using the given float [array] as the data and its size as the
 *   capacity.
 */
expect fun FloatBuffer(array: FloatArray): FloatBuffer

/**
 * @param capacity this buffer's capacity, in bytes.
 * @return a new [ByteBuffer] with the given [capacity].
 */
expect fun ByteBuffer(capacity: Int): ByteBuffer

/**
 * @return a new [ByteBuffer] using the given byte [array] as the data and its size as the capacity.
 */
expect fun ByteBuffer(array: ByteArray): ByteBuffer

/**
 * @return a new [ByteBuffer] using the given byte [array] as the data and its size as the capacity.
 */
expect fun ByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer
