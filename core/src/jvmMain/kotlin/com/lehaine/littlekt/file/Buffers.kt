package com.lehaine.littlekt.file

import java.nio.*
import java.nio.Buffer


abstract class GenericBuffer<out B : Buffer>(override val capacity: Int, val buffer: B) :
    com.lehaine.littlekt.file.Buffer {
    override var limit: Int
        get() = buffer.limit()
        set(value) {
            buffer.limit(value)
        }

    override var position: Int
        get() = buffer.position()
        set(value) {
            buffer.position(value)
        }

    override val remaining: Int
        get() = buffer.remaining()

    override fun flip() {
        buffer.flip()
    }

    override fun clear() {
        buffer.clear()
    }
}

/**
 * ByteBuffer buffer implementation
 */
class Uint8BufferImpl(buffer: ByteBuffer) : Uint8Buffer, GenericBuffer<ByteBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder()))

    constructor(data: ByteArray) : this(ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())) {
        put(data)
    }

    override fun get(i: Int): Byte {
        return buffer[i]
    }

    override fun set(i: Int, value: Byte) {
        buffer.put(i, value)
    }

    override fun put(data: ByteArray, offset: Int, len: Int): Uint8Buffer {
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Byte): Uint8Buffer {
        buffer.put(value)
        return this
    }

    override fun put(data: Uint8Buffer): Uint8Buffer {
        if (data is Uint8BufferImpl) {
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * ShortBuffer buffer implementation
 */
class Uint16BufferImpl(buffer: ShortBuffer) : Uint16Buffer, GenericBuffer<ShortBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(
        ByteBuffer.allocateDirect(capacity * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
    )

    override fun get(i: Int): Short {
        return buffer[i]
    }

    override fun set(i: Int, value: Short) {
        buffer.put(i, value)
    }

    override fun put(data: ShortArray, offset: Int, len: Int): Uint16Buffer {
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Short): Uint16Buffer {
        buffer.put(value)
        return this
    }

    override fun put(data: Uint16Buffer): Uint16Buffer {
        if (data is Uint16BufferImpl) {
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * IntBuffer buffer implementation
 */
class Uint32BufferImpl(buffer: IntBuffer) : Uint32Buffer, GenericBuffer<IntBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(
        ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    )

    override fun get(i: Int): Int {
        return buffer[i]
    }

    override fun set(i: Int, value: Int) {
        buffer.put(i, value)
    }

    override fun put(data: IntArray, offset: Int, len: Int): Uint32Buffer {
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Int): Uint32Buffer {
        buffer.put(value)
        return this
    }

    override fun put(data: Uint32Buffer): Uint32Buffer {
        if (data is Uint32BufferImpl) {
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * FloatBuffer buffer implementation
 */
class Float32BufferImpl(buffer: FloatBuffer) : Float32Buffer, GenericBuffer<FloatBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(
        ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    )

    constructor(data: FloatArray) : this(
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    ) {
        put(data)
        position = buffer.position()
    }

    override fun get(i: Int): Float {
        return buffer[i]
    }

    override fun set(i: Int, value: Float) {
        buffer.put(i, value)
    }

    override fun put(data: FloatArray, offset: Int, len: Int): Float32Buffer {
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Float): Float32Buffer {
        buffer.put(value)
        return this
    }

    override fun put(data: Float32Buffer): Float32Buffer {
        if (data is Float32BufferImpl) {
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                buffer.put(data[i])
            }
        }
        return this
    }
}

class MixedBufferImpl(buffer: ByteBuffer) : MixedBuffer, GenericBuffer<ByteBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder()))
    constructor(data: ByteArray) : this(
        ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())
    ) {
        putInt8(data)
    }

    constructor(data: ByteArray, isBigEndian: Boolean) : this(
        ByteBuffer.allocateDirect(data.size).order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.nativeOrder())
    ) {
        putInt8(data)
    }

    override val readInt8: Byte get() = readUint8

    override fun getInt8(offset: Int): Byte {
        return getUint8(offset)
    }

    override fun getInt8s(startOffset: Int, endOffset: Int): ByteArray {
        return getUint8s(startOffset, endOffset)
    }

    override val readInt16: Short get() = readUin16

    override fun getInt16(offset: Int): Short {
        return getUint16(offset)
    }

    override val readInt32: Int
        get() = readUint32

    override fun getInt32(offset: Int): Int {
        return getUint32(offset)
    }

    override val readUint8: Byte
        get() = buffer.get()

    override fun getUint8(offset: Int): Byte {
        return buffer.get(offset)
    }

    override fun getUint8s(startOffset: Int, endOffset: Int): ByteArray {
        val dest = ByteArray(endOffset - startOffset)
        buffer.get(dest, startOffset, endOffset - startOffset)
        return dest
    }

    override fun putUint8(value: Byte): MixedBuffer {
        buffer.put(value)
        return this
    }

    override fun putUint8(data: ByteArray, offset: Int, len: Int): MixedBuffer {
        buffer.put(data, offset, len)
        return this
    }

    override fun putUint8(data: Uint8Buffer): MixedBuffer {
        if (data is Uint8BufferImpl) {
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                buffer.put(data[i])
            }
        }
        return this
    }

    override val readUin16: Short
        get() = buffer.short

    override fun getUint16(offset: Int): Short {
        return buffer.getShort(offset)
    }

    override fun putUint16(value: Short): MixedBuffer {
        buffer.putShort(value)
        return this
    }

    override fun putUint16(data: ShortArray, offset: Int, len: Int): MixedBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                buffer.putShort(data[offset + i])
            }
        } else {
            buffer.asShortBuffer().put(data, offset, len)
            position += len * 2
        }
        return this
    }

    override fun putUint16(data: Uint16Buffer): MixedBuffer {
        val len = data.limit - data.position
        if (data !is Uint16BufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                buffer.putShort(data[i])
            }
        } else {
            val dataPos = data.position
            buffer.asShortBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 2
        }
        return this
    }

    override val readUint32: Int
        get() = buffer.int

    override fun getUint32(offset: Int): Int {
        return buffer.getInt(offset)
    }

    override fun putUint32(value: Int): MixedBuffer {
        buffer.putInt(value)
        return this
    }

    override fun putUint32(data: IntArray, offset: Int, len: Int): MixedBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                buffer.putInt(data[offset + i])
            }
        } else {
            buffer.asIntBuffer().put(data, offset, len)
            position += len * 4
        }
        return this
    }

    override fun putUint32(data: Uint32Buffer): MixedBuffer {
        val len = data.limit - data.position
        if (data !is Uint32BufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                buffer.putInt(data[i])
            }
        } else {
            val dataPos = data.position
            buffer.asIntBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 4
        }
        return this
    }

    override val readFloat32: Float
        get() = buffer.float

    override fun getFloat32(offset: Int): Float {
        return buffer.getFloat(offset)
    }

    override fun putFloat32(value: Float): MixedBuffer {
        buffer.putFloat(value)
        return this
    }

    override fun putFloat32(data: FloatArray, offset: Int, len: Int): MixedBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                buffer.putFloat(data[offset + i])
            }
        } else {
            buffer.asFloatBuffer().put(data, offset, len)
            position += len * 4
        }
        return this
    }

    override fun putFloat32(data: Float32Buffer): MixedBuffer {
        val len = data.limit - data.position
        if (data !is Float32BufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                buffer.putFloat(data[i])
            }
        } else {
            val dataPos = data.position
            buffer.asFloatBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 4
        }
        return this
    }

    override fun getTag(offset: Int): String {
        var tag = ""
        for (i in offset until offset + 4) {
            tag += buffer.get(i).toInt().toChar()
        }
        return tag
    }

    override fun getOffset(offset: Int, offSize: Int): Int {
        var v = 0
        for (i in 0 until offSize) {
            v = v shl 8
            v += buffer.get(offset + i)
        }
        return v
    }

    companion object {
        // todo: find a good value / always / never convert buffer type
        private const val BUFFER_CONV_THRESH = 4
    }
}

actual fun createUint8Buffer(capacity: Int): Uint8Buffer = Uint8BufferImpl(capacity)
actual fun createUint8Buffer(array: ByteArray): Uint8Buffer = Uint8BufferImpl(array)

actual fun createUint16Buffer(capacity: Int): Uint16Buffer = Uint16BufferImpl(capacity)

actual fun createUint32Buffer(capacity: Int): Uint32Buffer = Uint32BufferImpl(capacity)

actual fun createFloat32Buffer(capacity: Int): Float32Buffer = Float32BufferImpl(capacity)
actual fun createFloat32Buffer(array: FloatArray): Float32Buffer = Float32BufferImpl(array)

actual fun createMixedBuffer(capacity: Int): MixedBuffer = MixedBufferImpl(capacity)
actual fun createMixedBuffer(array: ByteArray): MixedBuffer = MixedBufferImpl(array)
actual fun createMixedBuffer(array: ByteArray, isBigEndian: Boolean): MixedBuffer = MixedBufferImpl(array, isBigEndian)