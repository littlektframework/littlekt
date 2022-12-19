package com.lehaine.littlekt.file

import org.khronos.webgl.*

internal abstract class GenericBuffer<out B : ArrayBufferView>(final override val capacity: Int, create: () -> B) :
    Buffer {
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

/**
 * ShortBuffer buffer implementation
 */
internal class ShortBufferImpl(capacity: Int) : ShortBuffer, GenericBuffer<Uint16Array>(capacity, {
    Uint16Array(capacity)
}) {
    override var dirty: Boolean = false
    override fun get(i: Int): Short {
        return buffer[i]
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: ShortArray, offset: Int, len: Int): ShortBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
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

/**
 * IntBuffer buffer implementation
 */
internal class IntBufferImpl(capacity: Int) : IntBuffer, GenericBuffer<Uint32Array>(capacity, {
    Uint32Array(capacity)
}) {
    override var dirty: Boolean = false
    override fun get(i: Int): Int {
        return buffer[i]
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: IntArray, offset: Int, len: Int): IntBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
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

/**
 * FloatBuffer buffer implementation
 */
internal class FloatBufferImpl(array: Float32Array) : FloatBuffer,
    GenericBuffer<Float32Array>(array.length, { array }) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(Float32Array(capacity))


    override fun get(i: Int): Float {
        return buffer[i]
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer[i] = value
    }

    override fun put(data: FloatArray, offset: Int, len: Int): FloatBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer[position++] = data[i]
        }
        return this
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
}

/**
 * ByteBuffer buffer implementation
 */
internal class ByteBufferImpl(buffer: ArrayBuffer) : ByteBuffer, GenericBuffer<DataView>(buffer.byteLength, {
    DataView(buffer)
}) {
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
        buffer.setInt16(i, value)
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer.setInt32(i, value)
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer.setFloat32(i, value)
    }

    override val readByte: Byte
        get() = buffer.getInt8(position++)

    override fun getByte(offset: Int): Byte {
        return buffer.getInt8(offset)
    }

    override fun getByteArray(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= endOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = buffer.getInt8(i)
        }
        position += endOffset - startOffset
        return bytes
    }

    override val readShort: Short
        get() {
            val result = buffer.getInt16(position)
            position += 2
            return result
        }

    override fun getShort(offset: Int): Short {
        return buffer.getInt16(offset)
    }

    override val readInt: Int
        get() {
            val result = buffer.getInt32(position)
            position += 4
            return result
        }

    override fun getInt(offset: Int): Int {
        return buffer.getInt32(offset)
    }

    override val readUByte: Byte
        get() = buffer.getUint8(position++)

    override fun getUByte(offset: Int): Byte {
        return buffer.getUint8(offset)
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

    override fun putUByte(value: Byte): ByteBuffer {
        dirty = true
        buffer.setUint8(position++, value)
        return this
    }

    override fun putUByte(data: ByteArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer.setUint8(position++, data[i])
        }
        return this
    }

    override fun putUByte(data: ByteBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            buffer.setUint8(position++, data.getUByte(i))
        }
        return this
    }

    override val readUShort: Short
        get() {
            val result = buffer.getUint16(position)
            position += 2
            return result

        }

    override fun getUShort(offset: Int): Short {
        return buffer.getUint16(offset)
    }

    override fun putUShort(value: Short): ByteBuffer {
        dirty = true
        buffer.setUint16(position, value)
        position += 2
        return this
    }

    override fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer.setUint16(position, data[i])
            position += 2
        }
        return this
    }

    override fun putUShort(data: ShortBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            buffer.setUint16(position, data[i])
            position += 2
        }
        return this
    }

    override val readUInt: Int
        get() {
            val result = buffer.getUint32(position)
            position += 4
            return result
        }

    override fun getUInt(offset: Int): Int {
        return buffer.getUint32(offset)
    }

    override fun putUInt(value: Int): ByteBuffer {
        dirty = true
        buffer.setUint32(position, value)
        position += 4
        return this
    }

    override fun putUInt(offset: Int, value: Int): ByteBuffer {
        dirty = true
        buffer.setUint32(offset, value)
        return this
    }

    override fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer.setUint32(position, data[i])
            position += 4
        }
        return this
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
            val result = buffer.getFloat32(position)
            position += 4
            return result
        }

    override fun getFloat(offset: Int): Float {
        return buffer.getFloat32(offset)
    }

    override fun putFloat(value: Float): ByteBuffer {
        dirty = true
        buffer.setFloat32(position, value)
        position += 4
        return this
    }

    override fun putFloat(data: FloatArray, offset: Int, len: Int): ByteBuffer {
        for (i in offset until offset + len) {
            dirty = true
            buffer.setFloat32(position, data[i])
            position += 4
        }
        return this
    }

    override fun putFloat(data: FloatBuffer): ByteBuffer {
        for (i in data.position until data.limit) {
            dirty = true
            buffer.setFloat32(position, data[i])
            position += 4
        }
        return this
    }

    override fun getString(offset: Int, length: Int): String {
        var tag = "";
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

actual fun createShortBuffer(capacity: Int): ShortBuffer {
    return ShortBufferImpl(capacity)
}

actual fun createIntBuffer(capacity: Int): IntBuffer {
    return IntBufferImpl(capacity)
}

actual fun createFloatBuffer(capacity: Int): FloatBuffer {
    return FloatBufferImpl(capacity)
}

actual fun createFloatBuffer(array: FloatArray): FloatBuffer {
    return FloatBufferImpl(Float32Array(array.toTypedArray()))
}

actual fun createByteBuffer(capacity: Int): ByteBuffer {
    return ByteBufferImpl(capacity)
}

actual fun createByteBuffer(array: ByteArray): ByteBuffer {
    return ByteBufferImpl(Uint8Array(array.toTypedArray()).buffer)
}

actual fun createByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer = createByteBuffer(array)