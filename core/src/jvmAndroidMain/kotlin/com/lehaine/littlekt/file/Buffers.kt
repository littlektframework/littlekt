package com.lehaine.littlekt.file

import java.nio.ByteOrder
import java.nio.Buffer as NioBuffer
import java.nio.ByteBuffer as NioByteBuffer
import java.nio.FloatBuffer as NioFloatBuffer
import java.nio.IntBuffer as NioIntBuffer
import java.nio.ShortBuffer as NioShortBuffer


abstract class GenericBuffer<out B : NioBuffer>(override val capacity: Int, val buffer: B) : Buffer {
    override var dirty: Boolean = false
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
 * ShortBuffer buffer implementation
 */
class ShortBufferImpl(buffer: NioShortBuffer) : ShortBuffer, GenericBuffer<NioShortBuffer>(buffer.capacity(), buffer) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(
        NioByteBuffer.allocateDirect(capacity * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
    )

    override fun get(i: Int): Short {
        return buffer[i]
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        buffer.put(i, value)
    }

    override fun put(data: ShortArray, offset: Int, len: Int): ShortBuffer {
        dirty = true
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Short): ShortBuffer {
        dirty = true
        buffer.put(value)
        return this
    }

    override fun put(data: ShortBuffer): ShortBuffer {
        if (data is ShortBufferImpl) {
            dirty = true
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * IntBuffer buffer implementation
 */
class IntBufferImpl(buffer: NioIntBuffer) : IntBuffer, GenericBuffer<NioIntBuffer>(buffer.capacity(), buffer) {
    override var dirty: Boolean = false

    constructor(capacity: Int) : this(
        NioByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
    )

    override fun get(i: Int): Int {
        return buffer[i]
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer.put(i, value)
    }

    override fun put(data: IntArray, offset: Int, len: Int): IntBuffer {
        dirty = true
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Int): IntBuffer {
        dirty = true
        buffer.put(value)
        return this
    }

    override fun put(data: IntBuffer): IntBuffer {
        if (data is IntBufferImpl) {
            dirty = true
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * FloatBuffer buffer implementation
 */
class FloatBufferImpl(buffer: NioFloatBuffer) : FloatBuffer, GenericBuffer<NioFloatBuffer>(buffer.capacity(), buffer) {

    override var dirty: Boolean = false

    constructor(capacity: Int) : this(
        NioByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    )

    constructor(data: FloatArray) : this(
        NioByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    ) {
        put(data)
        position = buffer.position()
    }

    override fun get(i: Int): Float {
        return buffer[i]
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer.put(i, value)
    }

    override fun put(data: FloatArray, offset: Int, len: Int): FloatBuffer {
        dirty = true
        buffer.put(data, offset, len)
        return this
    }

    override fun put(value: Float): FloatBuffer {
        dirty = true
        buffer.put(value)
        return this
    }

    override fun put(data: FloatBuffer): FloatBuffer {
        if (data is FloatBufferImpl) {
            dirty = true
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.put(data[i])
            }
        }
        return this
    }
}

/**
 * ByteBuffer implementation.
 */
class ByteBufferImpl(buffer: NioByteBuffer) : ByteBuffer, GenericBuffer<NioByteBuffer>(buffer.capacity(), buffer) {

    constructor(capacity: Int) : this(NioByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder()))
    constructor(data: ByteArray) : this(
        NioByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder())
    ) {
        putByte(data)
    }

    constructor(data: ByteArray, isBigEndian: Boolean) : this(
        NioByteBuffer.allocateDirect(data.size)
            .order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.nativeOrder())
    ) {
        putByte(data)
    }

    override fun get(i: Int): Byte {
        return buffer.get(i)
    }

    override fun set(i: Int, value: Byte) {
        dirty = true
        buffer.put(i, value)
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        buffer.putInt(i, value)
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        buffer.putShort(i, value)
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        buffer.putFloat(i, value)
    }

    override val readByte: Byte get() = readUByte

    override fun getByte(offset: Int): Byte {
        return getUByte(offset)
    }

    override fun getByteArray(startOffset: Int, endOffset: Int): ByteArray {
        return getUByteArray(startOffset, endOffset)
    }

    override val readShort: Short get() = readUShort

    override fun getShort(offset: Int): Short {
        return getUShort(offset)
    }

    override val readInt: Int
        get() = readUInt

    override fun getInt(offset: Int): Int {
        return getUInt(offset)
    }

    override val readUByte: Byte
        get() = buffer.get()

    override fun getUByte(offset: Int): Byte {
        return buffer.get(offset)
    }

    override fun getUByteArray(startOffset: Int, endOffset: Int): ByteArray {
        val dest = ByteArray(endOffset - startOffset)
        buffer.get(dest, startOffset, endOffset - startOffset)
        return dest
    }

    override fun putUByte(value: Byte): ByteBuffer {
        dirty = true
        buffer.put(value)
        return this
    }

    override fun putUByte(data: ByteArray, offset: Int, len: Int): ByteBuffer {
        dirty = true
        buffer.put(data, offset, len)
        return this
    }

    override fun putUByte(data: ByteBuffer): ByteBuffer {
        if (data is ByteBufferImpl) {
            dirty = true
            val dataPos = data.position
            buffer.put(data.buffer)
            data.position = dataPos
        } else {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.put(data.getByte(i))
            }
        }
        return this
    }

    override val readUShort: Short
        get() = buffer.short

    override fun getUShort(offset: Int): Short {
        return buffer.getShort(offset)
    }

    override fun putUShort(value: Short): ByteBuffer {
        dirty = true
        buffer.putShort(value)
        return this
    }

    override fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                dirty = true
                buffer.putShort(data[offset + i])
            }
        } else {
            dirty = true
            buffer.asShortBuffer().put(data, offset, len)
            position += len * 2
        }
        return this
    }

    override fun putUShort(data: ShortBuffer): ByteBuffer {
        val len = data.limit - data.position
        if (data !is ShortBufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.putShort(data[i])
            }
        } else {
            dirty = true
            val dataPos = data.position
            buffer.asShortBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 2
        }
        return this
    }

    override val readUInt: Int
        get() = buffer.int

    override fun getUInt(offset: Int): Int {
        return buffer.getInt(offset)
    }

    override fun putUInt(value: Int): ByteBuffer {
        dirty = true
        buffer.putInt(value)
        return this
    }

    override fun putUInt(offset: Int, value: Int): ByteBuffer {
        dirty = true
        buffer.putInt(offset, value)
        return this
    }

    override fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                dirty = true
                buffer.putInt(data[offset + i])
            }
        } else {
            dirty = true
            buffer.asIntBuffer().put(data, offset, len)
            position += len * 4
        }
        return this
    }

    override fun putUInt(data: IntBuffer): ByteBuffer {
        val len = data.limit - data.position
        if (data !is IntBufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.putInt(data[i])
            }
        } else {
            dirty = true
            val dataPos = data.position
            buffer.asIntBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 4
        }
        return this
    }

    override val readFloat: Float
        get() = buffer.float

    override fun getFloat(offset: Int): Float {
        return buffer.getFloat(offset)
    }

    override fun putFloat(value: Float): ByteBuffer {
        dirty = true
        buffer.putFloat(value)
        return this
    }

    override fun putFloat(data: FloatArray, offset: Int, len: Int): ByteBuffer {
        if (len <= BUFFER_CONV_THRESH) {
            for (i in 0 until len) {
                dirty = true
                buffer.putFloat(data[offset + i])
            }
        } else {
            dirty = true
            buffer.asFloatBuffer().put(data, offset, len)
            position += len * 4
        }
        return this
    }

    override fun putFloat(data: FloatBuffer): ByteBuffer {
        val len = data.limit - data.position
        if (data !is FloatBufferImpl || len <= BUFFER_CONV_THRESH) {
            for (i in data.position until data.limit) {
                dirty = true
                buffer.putFloat(data[i])
            }
        } else {
            dirty = true
            val dataPos = data.position
            buffer.asFloatBuffer().put(data.buffer)
            data.position = dataPos
            position += len * 4
        }
        return this
    }

    override fun getString(offset: Int, length: Int): String {
        var tag = ""
        for (i in offset until offset + length) {
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

actual fun createShortBuffer(capacity: Int): ShortBuffer = ShortBufferImpl(capacity)

actual fun createIntBuffer(capacity: Int): IntBuffer = IntBufferImpl(capacity)

actual fun createFloatBuffer(capacity: Int): FloatBuffer = FloatBufferImpl(capacity)
actual fun createFloatBuffer(array: FloatArray): FloatBuffer = FloatBufferImpl(array)

actual fun createByteBuffer(capacity: Int): ByteBuffer = ByteBufferImpl(capacity)
actual fun createByteBuffer(array: ByteArray): ByteBuffer = ByteBufferImpl(array)
actual fun createByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer = ByteBufferImpl(array, isBigEndian)