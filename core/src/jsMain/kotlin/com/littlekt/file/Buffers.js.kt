package com.littlekt.file

import org.khronos.webgl.*

internal abstract class GenericBuffer<out B : ArrayBufferView>(
    final override val capacity: Int,
    create: () -> B,
) : Buffer {
    override var dirty: Boolean = false
    val buffer = create()

    override var limit = capacity
        set(value) {
            if (value < 0 || value > capacity) {
                throw RuntimeException("Limit is out of bounds: $value (capacity: $capacity)")
            }
            field = value
            if (position > value) {
                position = value
            }
        }

    override var position = 0

    override val remaining: Int
        get() = limit - position

    override fun flip() {
        limit = position
        position = 0
    }

    override fun clear() {
        limit = capacity
        position = 0
    }
}

/** ShortBuffer buffer implementation */
internal class ShortBufferImpl(array: Uint16Array) :
    ShortBuffer, GenericBuffer<Uint16Array>(array.length, { array }) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(Uint16Array(capacity))

    override fun get(i: Int): Short {
        return buffer[i]
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: ShortArray, srcOffset: Int, len: Int): ShortBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(data: ShortArray, dstOffset: Int, srcOffset: Int, len: Int): ShortBuffer {
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Short): ShortBuffer {
        dirty = true
        buffer[position++] = value
        return this
    }

    override fun put(data: ShortBuffer): ShortBuffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

/** IntBuffer buffer implementation */
internal class IntBufferImpl(array: Uint32Array) :
    IntBuffer, GenericBuffer<Uint32Array>(array.length, { array }) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(Uint32Array(capacity))

    override fun get(i: Int): Int {
        return buffer[i]
    }

    fun getInt32Array(out: Int32Array): Int32Array {
        check(out.length >= capacity) { "out must be >= the buffer capacity!" }
        for (i in 0 until capacity) {
            out[i] = get(i)
        }
        return out
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: IntArray, srcOffset: Int, len: Int): IntBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(data: IntArray, dstOffset: Int, srcOffset: Int, len: Int): IntBuffer {
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Int): IntBuffer {
        dirty = true
        buffer[position++] = value
        return this
    }

    override fun put(data: IntBuffer): IntBuffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

/** FloatBuffer buffer implementation */
internal class FloatBufferImpl(array: Float32Array) :
    FloatBuffer, GenericBuffer<Float32Array>(array.length, { array }) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(Float32Array(capacity))

    override fun get(i: Int): Float {
        return buffer[i]
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: FloatArray, srcOffset: Int, len: Int): FloatBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(data: FloatArray, dstOffset: Int, srcOffset: Int, len: Int): FloatBuffer {
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Float): FloatBuffer {
        dirty = true
        buffer[position++] = value
        return this
    }

    override fun put(data: FloatBuffer): FloatBuffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }

    override fun put(data: FloatBuffer, dstOffset: Int, srcOffset: Int, len: Int): FloatBuffer {
        position = dstOffset
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
    }
}

/** ByteBuffer buffer implementation */
internal class ByteBufferImpl(buffer: ArrayBuffer) :
    ByteBuffer, GenericBuffer<DataView>(buffer.byteLength, { DataView(buffer) }) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(ArrayBuffer(capacity))

    constructor(array: Uint8Array) : this(array.buffer)

    constructor(array: Uint8ClampedArray) : this(array.buffer)

    constructor(array: ByteArray) : this(Uint8Array(array.toTypedArray()))

    override fun get(i: Int): Byte {
        return buffer.getInt8(i)
    }

    override fun set(i: Int, value: Byte) {
        dirty = true
        buffer.setInt8(i, value)
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        buffer.setInt16(i, value, true)
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer.setInt32(i, value, true)
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer.setFloat32(i, value, true)
    }

    override val readByte: Byte
        get() = buffer.getInt8(position++)

    override fun getByte(offset: Int): Byte {
        return buffer.getInt8(offset)
    }

    fun getUInt8Array(out: Uint8Array): Uint8Array {
        check(out.length >= capacity) { "out must be >= the buffer capacity!" }
        for (i in 0 until capacity) {
            out[i] = getByte(i)
        }
        return out
    }

    override fun getByteArray(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= startOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = buffer.getInt8(i)
        }
        position += endOffset - startOffset
        return bytes
    }

    override val readShort: Short
        get() {
            val result = buffer.getInt16(position, true)
            position += 2
            return result
        }

    override fun getShort(offset: Int): Short {
        return buffer.getInt16(offset, true)
    }

    override val readInt: Int
        get() {
            val result = buffer.getInt32(position, true)
            position += 4
            return result
        }

    override fun getInt(offset: Int): Int {
        return buffer.getInt32(offset, true)
    }

    override val readUByte: Byte
        get() = buffer.getUint8(position++)

    override fun getUByte(offset: Int): UByte {
        return buffer.getUint8(offset).toUByte()
    }

    override fun getUByteArray(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= endOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = buffer.getUint8(i)
        }
        position += endOffset - startOffset
        return bytes
    }

    override fun putUByte(value: UByte): ByteBuffer {
        dirty = true
        buffer.setUint8(position++, value.toByte())
        return this
    }

    override fun putUByte(offset: Int, value: UByte): ByteBuffer {
        dirty = true
        buffer.setUint8(offset, value.toByte())
        position = offset + 1
        return this
    }

    override fun putUByte(data: ByteArray, srcOffset: Int, len: Int): ByteBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            buffer.setUint8(position++, data[i])
        }
        return this
    }

    override fun putUByte(data: ByteArray, dstOffset: Int, srcOffset: Int, len: Int): ByteBuffer {
        position = dstOffset
        return putUByte(data, srcOffset, len)
    }

    override fun putUByte(data: ByteBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            buffer.setUint8(position++, data.getUByte(i).toByte())
        }
        return this
    }

    override val readUShort: Short
        get() {
            val result = buffer.getUint16(position, true)
            position += 2
            return result
        }

    override fun getUShort(offset: Int): UShort {
        return buffer.getUint16(offset, true).toUShort()
    }

    override fun putUShort(value: UShort): ByteBuffer {
        dirty = true
        buffer.setUint16(position, value.toShort(), true)
        position += 2
        return this
    }

    override fun putUShort(offset: Int, value: UShort): ByteBuffer {
        dirty = true
        buffer.setUint16(offset, value.toShort(), true)
        position = offset + 2
        return this
    }

    override fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            putShort(data[offset + i])
        }
        return this
    }

    override fun putUShort(data: ShortArray, offset: Int, dstOffset: Int, len: Int): ByteBuffer {
        position = dstOffset
        return putUShort(data, offset, len)
    }

    override fun putUShort(data: ShortBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            putShort(data[i])
        }
        return this
    }

    override val readUInt: Int
        get() {
            val result = buffer.getUint32(position, true)
            position += 4
            return result
        }

    override fun getUInt(offset: Int): UInt {
        return buffer.getUint32(offset, true).toUInt()
    }

    override fun putUInt(value: UInt): ByteBuffer {
        dirty = true
        buffer.setUint32(position, value.toInt(), true)
        position += 4
        return this
    }

    override fun putUInt(offset: Int, value: UInt): ByteBuffer {
        dirty = true
        buffer.setUint32(offset, value.toInt(), true)
        position += 4
        return this
    }

    override fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            putInt(data[offset + i])
        }
        return this
    }

    override fun putUInt(data: IntArray, srcOffset: Int, dstOffset: Int, len: Int): ByteBuffer {
        position = dstOffset
        return putUInt(data, srcOffset, len)
    }

    override fun putUInt(data: IntBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            buffer.setUint32(position, data[i])
            position += 4
        }
        return this
    }

    override val readFloat: Float
        get() {
            val result = buffer.getFloat32(position, true)
            position += 4
            return result
        }

    override fun getFloat(offset: Int): Float {
        return buffer.getFloat32(offset, true)
    }

    override fun putFloat(value: Float): ByteBuffer {
        dirty = true
        buffer.setFloat32(position, value, true)
        position += 4
        return this
    }

    override fun putFloat(offset: Int, value: Float): ByteBuffer {
        dirty = true
        buffer.setFloat32(offset, value, true)
        position = offset + 4
        return this
    }

    override fun putFloat(data: FloatArray, offset: Int, len: Int): ByteBuffer {
        for (i in 0 until len) {
            dirty = true
            putFloat(data[offset + i])
        }
        return this
    }

    override fun putFloat(data: FloatArray, srcOffset: Int, dstOffset: Int, len: Int): ByteBuffer {
        position = dstOffset
        return putFloat(data, srcOffset, len)
    }

    override fun putFloat(data: FloatBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            putFloat(data[i])
        }
        return this
    }

    override fun getString(offset: Int, length: Int): String {
        var tag = ""
        for (i in offset until offset + length) {
            tag += buffer.getInt8(i).toInt().toChar()
        }
        return tag
    }

    override fun getOffset(offset: Int, offSize: Int): Int {
        var v = 0
        for (i in 0 until offSize) {
            v = v shl 8
            v += buffer.getUint8(offset + i)
        }
        return v
    }
}

actual fun ShortBuffer(capacity: Int): ShortBuffer {
    return ShortBufferImpl(capacity)
}

actual fun ShortBuffer(array: ShortArray): ShortBuffer {
    return ShortBufferImpl(Uint16Array(array.toTypedArray()))
}

actual fun IntBuffer(capacity: Int): IntBuffer {
    return IntBufferImpl(capacity)
}

actual fun IntBuffer(array: IntArray): IntBuffer {
    return IntBufferImpl(Uint32Array(array.toTypedArray()))
}

actual fun FloatBuffer(capacity: Int): FloatBuffer {
    return FloatBufferImpl(capacity)
}

actual fun FloatBuffer(array: FloatArray): FloatBuffer {
    return FloatBufferImpl(Float32Array(array.toTypedArray()))
}

actual fun ByteBuffer(capacity: Int): ByteBuffer {
    return ByteBufferImpl(capacity)
}

actual fun ByteBuffer(array: ByteArray): ByteBuffer {
    return ByteBufferImpl(Uint8Array(array.toTypedArray()).buffer)
}

actual fun ByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer = ByteBuffer(array)
