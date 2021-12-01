package com.lehaine.littlekt.file

import org.khronos.webgl.*

internal abstract class GenericBuffer<out B : ArrayBufferView>(final override val capacity: Int, create: () -> B) :
    Buffer {
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
 * ByteBuffer buffer implementation
 */
internal class Uint8BufferImpl(array: Uint8Array) : Uint8Buffer, GenericBuffer<Uint8Array>(array.length, { array }) {

    constructor(capacity: Int) : this(Uint8Array(capacity))
    constructor(array: Uint8ClampedArray) : this(Uint8Array(array.buffer))

    override fun get(i: Int): Byte {
        return buffer[i]
    }

    override fun set(i: Int, value: Byte) {
        buffer[i] = value
    }

    override fun put(data: ByteArray, offset: Int, len: Int): Uint8Buffer {
        for (i in offset until offset + len) {
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(value: Byte): Uint8Buffer {
        buffer[position++] = value
        return this
    }

    override fun put(data: Uint8Buffer): Uint8Buffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

/**
 * ShortBuffer buffer implementation
 */
internal class Uint16BufferImpl(capacity: Int) : Uint16Buffer, GenericBuffer<Uint16Array>(capacity, {
    Uint16Array(capacity)
}) {
    override fun get(i: Int): Short {
        return buffer[i]
    }

    override fun set(i: Int, value: Short) {
        buffer[i] = value
    }

    override fun put(data: ShortArray, offset: Int, len: Int): Uint16Buffer {
        for (i in offset..(offset + len - 1)) {
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(value: Short): Uint16Buffer {
        buffer[position++] = value
        return this
    }

    override fun put(data: Uint16Buffer): Uint16Buffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

/**
 * IntBuffer buffer implementation
 */
internal class Uint32BufferImpl(capacity: Int) : Uint32Buffer, GenericBuffer<Uint32Array>(capacity, {
    Uint32Array(capacity)
}) {
    override fun get(i: Int): Int {
        return buffer[i]
    }

    override fun set(i: Int, value: Int) {
        buffer[i] = value
    }

    override fun put(data: IntArray, offset: Int, len: Int): Uint32Buffer {
        for (i in offset..(offset + len - 1)) {
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(value: Int): Uint32Buffer {
        buffer[position++] = value
        return this
    }

    override fun put(data: Uint32Buffer): Uint32Buffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

/**
 * FloatBuffer buffer implementation
 */
internal class Float32BufferImpl(array: Float32Array) : Float32Buffer,
    GenericBuffer<Float32Array>(array.length, { array }) {
    constructor(capacity: Int) : this(Float32Array(capacity))


    override fun get(i: Int): Float {
        return buffer[i]
    }

    override fun set(i: Int, value: Float) {
        buffer[i] = value
    }

    override fun put(data: FloatArray, offset: Int, len: Int): Float32Buffer {
        for (i in offset until offset + len) {
            buffer[position++] = data[i]
        }
        return this
    }

    override fun put(value: Float): Float32Buffer {
        buffer[position++] = value
        return this
    }

    override fun put(data: Float32Buffer): Float32Buffer {
        for (i in data.position until data.limit) {
            put(data[i])
        }
        return this
    }
}

internal class MixedBufferImpl(capacity: Int) : MixedBuffer, GenericBuffer<DataView>(capacity, {
    DataView(ArrayBuffer(capacity))
}) {
    override val readInt8: Byte
        get() = buffer.getInt8(position++)

    override fun getInt8(offset: Int): Byte {
        return buffer.getInt8(offset)
    }

    override fun getInt8s(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= endOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = buffer.getInt8(i)
        }
        position += endOffset - startOffset
        return bytes
    }

    override val readInt16: Short
        get() {
            val result = buffer.getInt16(position)
            position += 2
            return result
        }

    override fun getInt16(offset: Int): Short {
        return buffer.getInt16(offset)
    }

    override val readInt32: Int
        get() {
            val result = buffer.getInt32(position)
            position += 4
            return result
        }

    override fun getInt32(offset: Int): Int {
        return buffer.getInt32(offset)
    }

    override val readUint8: Byte
        get() = buffer.getUint8(position++)

    override fun getUint8(offset: Int): Byte {
        return buffer.getUint8(offset)
    }

    override fun getUint8s(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= endOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = buffer.getUint8(i)
        }
        position += endOffset - startOffset
        return bytes
    }

    override fun putUint8(value: Byte): MixedBuffer {
        buffer.setUint8(position++, value)
        return this
    }

    override fun putUint8(data: ByteArray, offset: Int, len: Int): MixedBuffer {
        for (i in offset until offset + len) {
            buffer.setUint8(position++, data[i])
        }
        return this
    }

    override fun putUint8(data: Uint8Buffer): MixedBuffer {
        for (i in data.position until data.limit) {
            buffer.setUint8(position++, data[i])
        }
        return this
    }

    override val readUin16: Short
        get() {
            val result = buffer.getUint16(position)
            position += 2
            return result

        }

    override fun getUint16(offset: Int): Short {
        return buffer.getUint16(offset)
    }

    override fun putUint16(value: Short): MixedBuffer {
        buffer.setUint16(position, value)
        position += 2
        return this
    }

    override fun putUint16(data: ShortArray, offset: Int, len: Int): MixedBuffer {
        for (i in offset until offset + len) {
            buffer.setUint16(position, data[i])
            position += 2
        }
        return this
    }

    override fun putUint16(data: Uint16Buffer): MixedBuffer {
        for (i in data.position until data.limit) {
            buffer.setUint16(position, data[i])
            position += 2
        }
        return this
    }

    override val readUint32: Int
        get() {
            val result = buffer.getUint32(position)
            position += 4
            return result
        }

    override fun getUint32(offset: Int): Int {
        return buffer.getUint32(offset)
    }

    override fun putUint32(value: Int): MixedBuffer {
        buffer.setUint32(position, value)
        position += 4
        return this
    }

    override fun putUint32(data: IntArray, offset: Int, len: Int): MixedBuffer {
        for (i in offset until offset + len) {
            buffer.setUint32(position, data[i])
            position += 4
        }
        return this
    }

    override fun putUint32(data: Uint32Buffer): MixedBuffer {
        for (i in data.position until data.limit) {
            buffer.setUint32(position, data[i])
            position += 4
        }
        return this
    }

    override val readFloat32: Float
        get() {
            val result = buffer.getFloat32(position)
            position += 4
            return result
        }

    override fun getFloat32(offset: Int): Float {
        return buffer.getFloat32(offset)
    }

    override fun putFloat32(value: Float): MixedBuffer {
        buffer.setFloat32(position, value)
        position += 4
        return this
    }

    override fun putFloat32(data: FloatArray, offset: Int, len: Int): MixedBuffer {
        for (i in offset until offset + len) {
            buffer.setFloat32(position, data[i])
            position += 4
        }
        return this
    }

    override fun putFloat32(data: Float32Buffer): MixedBuffer {
        for (i in data.position until data.limit) {
            buffer.setFloat32(position, data[i])
            position += 4
        }
        return this
    }

    override fun getTag(offset: Int): String {
        var tag = "";
        for (i in offset until offset + 4) {
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

actual fun createUint8Buffer(capacity: Int): Uint8Buffer {
    return Uint8BufferImpl(capacity)
}

actual fun createUint8Buffer(array: ByteArray): Uint8Buffer = Uint8BufferImpl(Uint8Array(array.toTypedArray()))

actual fun createUint16Buffer(capacity: Int): Uint16Buffer {
    return Uint16BufferImpl(capacity)
}

actual fun createUint32Buffer(capacity: Int): Uint32Buffer {
    return Uint32BufferImpl(capacity)
}

actual fun createFloat32Buffer(capacity: Int): Float32Buffer {
    return Float32BufferImpl(capacity)
}

actual fun createFloat32Buffer(array: FloatArray): Float32Buffer {
    return Float32BufferImpl(Float32Array(array.toTypedArray()))
}

actual fun createMixedBuffer(capacity: Int): MixedBuffer {
    return MixedBufferImpl(capacity)
}