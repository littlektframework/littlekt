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
 * A [Byte] buffer.
 */
interface Uint8Buffer : Buffer {
    operator fun get(i: Int): Byte
    operator fun set(i: Int, value: Byte)
    operator fun plusAssign(value: Byte) {
        put(value)
    }

    fun put(value: Byte): Uint8Buffer
    fun put(data: ByteArray): Uint8Buffer = put(data, 0, data.size)
    fun put(data: ByteArray, offset: Int, len: Int): Uint8Buffer
    fun put(data: Uint8Buffer): Uint8Buffer

    fun toArray(): ByteArray {
        val array = ByteArray(capacity)
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
 * A [Short] buffer
 */
interface Uint16Buffer : Buffer {
    operator fun get(i: Int): Short
    operator fun set(i: Int, value: Short)
    operator fun plusAssign(value: Short) {
        put(value)
    }

    fun put(value: Short): Uint16Buffer
    fun put(data: ShortArray): Uint16Buffer = put(data, 0, data.size)
    fun put(data: ShortArray, offset: Int, len: Int): Uint16Buffer
    fun put(data: Uint16Buffer): Uint16Buffer

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
interface Uint32Buffer : Buffer {
    operator fun get(i: Int): Int
    operator fun set(i: Int, value: Int)
    operator fun plusAssign(value: Int) {
        put(value)
    }

    fun put(value: Int): Uint32Buffer
    fun put(data: IntArray): Uint32Buffer = put(data, 0, data.size)
    fun put(data: IntArray, offset: Int, len: Int): Uint32Buffer
    fun put(data: Uint32Buffer): Uint32Buffer

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
interface Float32Buffer : Buffer {
    operator fun get(i: Int): Float
    operator fun set(i: Int, value: Float)
    operator fun plusAssign(value: Float) {
        put(value)
    }

    fun put(value: Float): Float32Buffer
    fun put(data: FloatArray): Float32Buffer = put(data, 0, data.size)
    fun put(data: FloatArray, offset: Int, len: Int): Float32Buffer
    fun put(data: Float32Buffer): Float32Buffer

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
interface MixedBuffer : Buffer {
    operator fun set(i: Int, value: Byte)
    operator fun set(i: Int, value: Int)
    operator fun set(i: Int, value: Short)
    operator fun set(i: Int, value: Float)

    operator fun plusAssign(value: Byte) {
        putInt8(value)
    }

    operator fun plusAssign(value: Short) {
        putInt16(value)
    }

    operator fun plusAssign(value: Int) {
        putInt32(value)
    }

    operator fun plusAssign(value: Float) {
        putFloat32(value)
    }

    val readInt8: Byte
    fun getInt8(offset: Int): Byte
    fun getInt8s(startOffset: Int, endOffset: Int): ByteArray
    fun putInt8(value: Byte) = putUint8(value)
    fun putInt8(data: ByteArray) = putUint8(data)
    fun putInt8(data: ByteArray, offset: Int, len: Int) = putUint8(data, offset, len)
    fun putInt8(data: Uint8Buffer) = putUint8(data)

    val readInt16: Short
    fun getInt16(offset: Int): Short
    fun putInt16(value: Short) = putUint16(value)
    fun putInt16(data: ShortArray) = putUint16(data)
    fun putInt16(data: ShortArray, offset: Int, len: Int) = putUint16(data, offset, len)
    fun putInt16(data: Uint16Buffer) = putUint16(data)

    val readInt32: Int
    fun getInt32(offset: Int): Int
    fun putInt32(offset: Int, value: Int) = putUint32(offset, value)
    fun putInt32(value: Int) = putUint32(value)
    fun putInt32(data: IntArray) = putUint32(data)
    fun putInt32(data: IntArray, offset: Int, len: Int) = putUint32(data, offset, len)
    fun putInt32(data: Uint32Buffer) = putUint32(data)

    val readUint8: Byte
    fun getUint8(offset: Int): Byte
    fun getUint8s(startOffset: Int, endOffset: Int): ByteArray
    fun putUint8(value: Byte): MixedBuffer
    fun putUint8(data: ByteArray): MixedBuffer = putUint8(data, 0, data.size)
    fun putUint8(data: ByteArray, offset: Int, len: Int): MixedBuffer
    fun putUint8(data: Uint8Buffer): MixedBuffer

    val readUin16: Short
    fun getUint16(offset: Int): Short
    fun putUint16(value: Short): MixedBuffer
    fun putUint16(data: ShortArray): MixedBuffer = putUint16(data, 0, data.size)
    fun putUint16(data: ShortArray, offset: Int, len: Int): MixedBuffer
    fun putUint16(data: Uint16Buffer): MixedBuffer

    val readUint32: Int
    fun getUint32(offset: Int): Int
    fun putUint32(value: Int): MixedBuffer
    fun putUint32(offset: Int, value: Int): MixedBuffer
    fun putUint32(data: IntArray): MixedBuffer = putUint32(data, 0, data.size)
    fun putUint32(data: IntArray, offset: Int, len: Int): MixedBuffer
    fun putUint32(data: Uint32Buffer): MixedBuffer

    val readFloat32: Float
    fun getFloat32(offset: Int): Float
    fun putFloat32(value: Float): MixedBuffer
    fun putFloat32(data: FloatArray): MixedBuffer = putFloat32(data, 0, data.size)
    fun putFloat32(data: FloatArray, offset: Int, len: Int): MixedBuffer
    fun putFloat32(data: Float32Buffer): MixedBuffer

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
            array[i] = getInt8(i)
        }
        return array
    }

    override fun removeAt(index: Int) {
        throw RuntimeException("MixedBuffer does not support element removal")
    }
}

expect fun createUint8Buffer(capacity: Int): Uint8Buffer
expect fun createUint8Buffer(array: ByteArray): Uint8Buffer

expect fun createUint16Buffer(capacity: Int): Uint16Buffer

expect fun createUint32Buffer(capacity: Int): Uint32Buffer

expect fun createFloat32Buffer(capacity: Int): Float32Buffer
expect fun createFloat32Buffer(array: FloatArray): Float32Buffer

expect fun createMixedBuffer(capacity: Int): MixedBuffer
expect fun createMixedBuffer(array: ByteArray): MixedBuffer
expect fun createMixedBuffer(array: ByteArray, isBigEndian: Boolean): MixedBuffer