package com.lehaine.littlekt.file

/**
 * Super class for platform-dependent buffers.
 * On the JVM these buffers directly map to the corresponding NIO buffers.
 */
interface Buffer {
    var limit: Int
    var position: Int
    val remaining: Int
    val capacity: Int

    fun flip()
    fun clear()

    fun removeAt(index: Int) {
        if (position > index) {
            position--
        }
        if (limit > index) {
            limit--
        }
    }
}


/**
 * A [Short] buffer
 */
interface ShortBuffer : Buffer {
    operator fun get(i: Int): Short
    operator fun set(i: Int, value: Short)
    operator fun plusAssign(value: Short) {
        put(value)
    }

    fun put(value: Short): ShortBuffer
    fun put(data: ShortArray): ShortBuffer = put(data, 0, data.size)
    fun put(data: ShortArray, offset: Int, len: Int): ShortBuffer
    fun put(data: ShortBuffer): ShortBuffer

    fun toArray(): ShortArray {
        val array = ShortArray(capacity)
        for (i in 0 until capacity) {
            array[i] = get(i)
        }
        return array
    }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/**
 * An [Int] buffer.
 */
interface IntBuffer : Buffer {
    operator fun get(i: Int): Int
    operator fun set(i: Int, value: Int)
    operator fun plusAssign(value: Int) {
        put(value)
    }

    fun put(value: Int): IntBuffer
    fun put(data: IntArray): IntBuffer = put(data, 0, data.size)
    fun put(data: IntArray, offset: Int, len: Int): IntBuffer
    fun put(data: IntBuffer): IntBuffer

    fun toArray(): IntArray {
        val array = IntArray(capacity)
        for (i in 0 until capacity) {
            array[i] = get(i)
        }
        return array
    }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/**
 * A [Float] buffer.
 */
interface FloatBuffer : Buffer {
    operator fun get(i: Int): Float
    operator fun set(i: Int, value: Float)
    operator fun plusAssign(value: Float) {
        put(value)
    }

    fun put(value: Float): FloatBuffer
    fun put(data: FloatArray): FloatBuffer = put(data, 0, data.size)
    fun put(data: FloatArray, offset: Int, len: Int): FloatBuffer
    fun put(data: FloatBuffer): FloatBuffer

    fun toArray(): FloatArray {
        val array = FloatArray(capacity)
        for (i in 0 until capacity) {
            array[i] = get(i)
        }
        return array
    }

    override fun removeAt(index: Int) {
        for (i in index until position) {
            this[i] = this[i + 1]
        }
        super.removeAt(index)
    }
}

/**
 * A [Byte] buffer for mixed types. All buffer positions are in bytes.
 */
interface ByteBuffer : Buffer {

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
    fun putByte(value: Byte) = putUByte(value)
    fun putByte(data: ByteArray) = putUByte(data)
    fun putByte(data: ByteArray, offset: Int, len: Int) = putUByte(data, offset, len)
    fun putByte(data: ByteBuffer) = putUByte(data)

    val readShort: Short
    fun getShort(offset: Int): Short
    fun putShort(value: Short) = putUShort(value)
    fun putShort(data: ShortArray) = putUShort(data)
    fun putShort(data: ShortArray, offset: Int, len: Int) = putUShort(data, offset, len)
    fun putShort(data: ShortBuffer) = putUShort(data)

    val readInt: Int
    fun getInt(offset: Int): Int
    fun putInt(offset: Int, value: Int) = putUInt(offset, value)
    fun putInt(value: Int) = putUInt(value)
    fun putInt(data: IntArray) = putUInt(data)
    fun putInt(data: IntArray, offset: Int, len: Int) = putUInt(data, offset, len)
    fun putInt(data: IntBuffer) = putUInt(data)

    val readUByte: Byte
    fun getUByte(offset: Int): Byte
    fun getUByteArray(startOffset: Int, endOffset: Int): ByteArray
    fun putUByte(value: Byte): ByteBuffer
    fun putUByte(data: ByteArray): ByteBuffer = putUByte(data, 0, data.size)
    fun putUByte(data: ByteArray, offset: Int, len: Int): ByteBuffer
    fun putUByte(data: ByteBuffer): ByteBuffer

    val readUShort: Short
    fun getUShort(offset: Int): Short
    fun putUShort(value: Short): ByteBuffer
    fun putUShort(data: ShortArray): ByteBuffer = putUShort(data, 0, data.size)
    fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer
    fun putUShort(data: ShortBuffer): ByteBuffer

    val readUInt: Int
    fun getUInt(offset: Int): Int
    fun putUInt(value: Int): ByteBuffer
    fun putUInt(offset: Int, value: Int): ByteBuffer
    fun putUInt(data: IntArray): ByteBuffer = putUInt(data, 0, data.size)
    fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer
    fun putUInt(data: IntBuffer): ByteBuffer

    val readFloat: Float
    fun getFloat(offset: Int): Float
    fun putFloat(value: Float): ByteBuffer
    fun putFloat(data: FloatArray): ByteBuffer = putFloat(data, 0, data.size)
    fun putFloat(data: FloatArray, offset: Int, len: Int): ByteBuffer
    fun putFloat(data: FloatBuffer): ByteBuffer

    /**
     * @return a 4-character tag
     */
    fun getString(offset: Int, length: Int): String

    /**
     * Offsets are 1 to 4 bytes in length, depending on the [offSize] param.
     * @return an offset from the buffer
     */
    fun getOffset(offset: Int, offSize: Int): Int

    fun toArray(): ByteArray {
        val array = ByteArray(capacity)
        for (i in 0 until capacity) {
            array[i] = getByte(i)
        }
        return array
    }

    override fun removeAt(index: Int) {
        throw RuntimeException("MixedBuffer does not support element removal")
    }
}

expect fun createShortBuffer(capacity: Int): ShortBuffer

expect fun createIntBuffer(capacity: Int): IntBuffer

expect fun createFloatBuffer(capacity: Int): FloatBuffer
expect fun createFloatBuffer(array: FloatArray): FloatBuffer

expect fun createByteBuffer(capacity: Int): ByteBuffer
expect fun createByteBuffer(array: ByteArray): ByteBuffer
expect fun createByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer