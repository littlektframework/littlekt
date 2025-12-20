package com.littlekt.file

import ffi.JnaArena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.nio.ByteOrder
import java.nio.ByteBuffer as NioByteBuffer

internal abstract class GenericBuffer<T : ValueLayout>(
    final override val capacity: Int,
    val segment: MemorySegment,
    val layout: T,
) : Buffer {

    override var dirty: Boolean = false
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
internal class ShortBufferImpl(capacity: Int) :
    ShortBuffer,
    GenericBuffer<ValueLayout.OfShort>(
        capacity,
        JnaArena().let { arena ->
            MemorySegment(arena.allocate(capacity * Short.SIZE_BYTES.toLong()), capacity * Short.SIZE_BYTES.toLong())
        },
        ValueLayout.JAVA_SHORT,
    ) {
    override var dirty: Boolean = false

    constructor(data: ShortArray) : this(data.size) {
        put(data)
    }

    private fun MemorySegment.set(i: Int, value: Short) = set(layout, i.toLong(), value)

    override fun get(i: Int): Short {
        return segment.get(layout, i.toLong())
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        segment.set(i, value)
    }

    override fun put(data: ShortArray, srcOffset: Int, len: Int): ShortBuffer {
        for (i in srcOffset until len) {
            dirty = true
            segment.set(position++, data[i])
        }
        return this
    }

    override fun put(data: ShortArray, dstOffset: Int, srcOffset: Int, len: Int): ShortBuffer {
        dirty = true
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Short): ShortBuffer {
        dirty = true
        segment.set(position++, value)
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
internal class IntBufferImpl(capacity: Int) :
    IntBuffer,
    GenericBuffer<ValueLayout.OfInt>(
        capacity,
        JnaArena().let { arena ->
            MemorySegment(arena.allocate(capacity * Int.SIZE_BYTES.toLong()), capacity * Int.SIZE_BYTES.toLong())
        },
        ValueLayout.JAVA_INT,
    ) {
    override var dirty: Boolean = false

    constructor(data: IntArray) : this(data.size) {
        put(data)
    }

    private fun MemorySegment.set(i: Int, value: Int) = set(layout, i.toLong(), value)

    override fun get(i: Int): Int {
        return segment.get(layout, i.toLong())
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        segment.set(i, value)
    }

    override fun put(data: IntArray, srcOffset: Int, len: Int): IntBuffer {
        dirty = true
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            segment.set(position++, data[i])
        }
        return this
    }

    override fun put(data: IntArray, dstOffset: Int, srcOffset: Int, len: Int): IntBuffer {
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Int): IntBuffer {
        dirty = true
        segment.set(position++, value)
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
internal class FloatBufferImpl(capacity: Int) :
    FloatBuffer,
    GenericBuffer<ValueLayout.OfFloat>(
        capacity,
        JnaArena().let { arena ->
            MemorySegment(arena.allocate(capacity * Float.SIZE_BYTES.toLong()), capacity * Float.SIZE_BYTES.toLong())
        },
        ValueLayout.JAVA_FLOAT,
    ) {

    override var dirty: Boolean = false

    constructor(data: FloatArray) : this(data.size) {
        put(data)
    }

    private fun MemorySegment.set(i: Int, value: Float) = set(layout, i.toLong(), value)

    override fun get(i: Int): Float {
        return segment.get(layout, i.toLong())
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        segment.set(i, value)
    }

    override fun put(data: FloatArray, srcOffset: Int, len: Int): FloatBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            segment.set(position++, data[i])
        }
        return this
    }

    override fun put(data: FloatArray, dstOffset: Int, srcOffset: Int, len: Int): FloatBuffer {
        position = dstOffset
        return put(data, srcOffset, len)
    }

    override fun put(value: Float): FloatBuffer {
        dirty = true
        segment.set(position++, value)
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
            segment.set(position++, data[i])
        }
        return this
    }
}

/** ByteBuffer implementation. */
internal class ByteBufferImpl(
    capacity: Int,
    layout: ValueLayout.OfByte = ValueLayout.JAVA_BYTE,
    val shortLayout: ValueLayout.OfShort = ValueLayout.JAVA_SHORT,
    val intLayout: ValueLayout.OfInt = ValueLayout.JAVA_INT,
    val floatLayout: ValueLayout.OfFloat = ValueLayout.JAVA_FLOAT,
    segment: MemorySegment? = null,
) : ByteBuffer, GenericBuffer<ValueLayout.OfByte>(
    capacity,
    segment ?: JnaArena().let { arena ->
        MemorySegment(arena.allocate(capacity.toLong()), capacity.toLong())
    },
    layout,
) {

    constructor(data: ByteArray) : this(data.size, ValueLayout.JAVA_BYTE) {
        putByte(data)
    }

    constructor(
        data: ByteArray,
        isBigEndian: Boolean,
    ) : this(
        data.size,
        ValueLayout.JAVA_BYTE,
        ValueLayout.JAVA_SHORT,
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_FLOAT,
    ) {
        putByte(data)
    }

    private fun MemorySegment.setByte(i: Int, value: Byte) = pointer.setByte(i.toLong(), value)

    private fun MemorySegment.setInt(offset: Int, value: Int) = pointer.setInt(offset.toLong(), value)

    private fun MemorySegment.setFloat(offset: Int, value: Float) = pointer.setFloat(offset.toLong(), value)

    private fun MemorySegment.setShort(offset: Int, value: Short) = pointer.setShort(offset.toLong(), value)

    override fun get(i: Int): Byte {
        return segment.pointer.getByte(i.toLong())
    }

    override fun set(i: Int, value: Byte) {
        dirty = true
        segment.setByte(i, value)
    }

    override fun set(i: Int, value: Int) {
        dirty = true
        segment.setInt(i, value)
    }

    override fun set(i: Int, value: Short) {
        dirty = true
        segment.setShort(i, value)
    }

    override fun set(i: Int, value: Float) {
        dirty = true
        segment.setFloat(i, value)
    }

    override val readByte: Byte
        get() = readUByte

    override fun getByte(offset: Int): Byte {
        return getUByte(offset).toByte()
    }

    override fun getByteArray(startOffset: Int, endOffset: Int): ByteArray {
        return getUByteArray(startOffset, endOffset)
    }

    override val readShort: Short
        get() = readUShort

    override fun getShort(offset: Int): Short {
        return getUShort(offset).toShort()
    }

    override val readInt: Int
        get() = readUInt

    override fun getInt(offset: Int): Int {
        return getUInt(offset).toInt()
    }

    override val readUByte: Byte
        get() {
            val result = segment.pointer.getByte(position.toLong())
            position++
            return result
        }

    override fun getUByte(offset: Int): UByte {
        return segment.pointer.getByte(offset.toLong()).toUByte()
    }

    override fun getUByteArray(startOffset: Int, endOffset: Int): ByteArray {
        check(endOffset >= startOffset) { "endOffset must be >= the startOffset!" }
        val bytes = ByteArray(endOffset - startOffset)
        for (i in startOffset until endOffset) {
            bytes[i - startOffset] = segment.pointer.getByte(i.toLong())
        }
        position += endOffset - startOffset
        return bytes
    }

    override fun putUByte(value: UByte): ByteBuffer {
        dirty = true
        segment.setByte(position++, value.toByte())
        return this
    }

    override fun putUByte(offset: Int, value: UByte): ByteBuffer {
        dirty = true
        segment.setByte(offset, value.toByte())
        position = offset + 1
        return this
    }

    override fun putUByte(data: ByteArray, srcOffset: Int, len: Int): ByteBuffer {
        for (i in srcOffset until srcOffset + len) {
            dirty = true
            segment.setByte(position++, data[i])
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
            segment.setByte(position++, data.getUByte(i).toByte())
        }
        return this
    }

    fun putUByte(data: NioByteBuffer): ByteBuffer {
        for (i in data.position() until data.limit()) {
            dirty = true
            segment.setByte(position++, data.get(i))
        }
        return this
    }

    fun putByte(data: NioByteBuffer): ByteBuffer = putUByte(data)

    override val readUShort: Short
        get() {
            val result = segment.pointer.getShort(position.toLong())
            position += 2
            return result
        }

    override fun getUShort(offset: Int): UShort {
        return segment.pointer.getShort(offset.toLong()).toUShort()
    }

    override fun putUShort(value: UShort): ByteBuffer {
        dirty = true
        this[position + 1] = (value.toInt() shr 8).toByte()
        this[position + 0] = value.toByte()
        position += 2
        return this
    }

    override fun putUShort(offset: Int, value: UShort): ByteBuffer {
        dirty = true
        this[offset + 1] = (value.toInt() shr 8).toByte()
        this[offset + 0] = value.toByte()
        position = offset + 2
        return this
    }

    override fun putUShort(data: ShortArray, offset: Int, len: Int): ByteBuffer {
        for (i in 0 until len) {
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
            val result = segment.pointer.getInt(position.toLong())
            position += 4
            return result
        }

    override fun getUInt(offset: Int): UInt {
        return segment.pointer.getInt(offset.toLong()).toUInt()
    }

    override fun putUInt(value: UInt): ByteBuffer {
        dirty = true
        this[position + 3] = (value shr 24).toByte()
        this[position + 2] = (value shr 16).toByte()
        this[position + 1] = (value shr 8).toByte()
        this[position + 0] = value.toByte()
        position += 4
        return this
    }

    override fun putUInt(offset: Int, value: UInt): ByteBuffer {
        dirty = true
        this[offset + 3] = (value shr 24).toByte()
        this[offset + 2] = (value shr 16).toByte()
        this[offset + 1] = (value shr 8).toByte()
        this[offset + 0] = value.toByte()
        position = offset + 4
        return this
    }

    override fun putUInt(data: IntArray, offset: Int, len: Int): ByteBuffer {
        for (i in 0 until len) {
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
            segment.setInt(position, data[i])
            position += 4
        }
        return this
    }

    override val readFloat: Float
        get() {
            val result = segment.pointer.getFloat(position.toLong())
            position += 4
            return result
        }

    override fun getFloat(offset: Int): Float {
        return segment.pointer.getFloat(offset.toLong())
    }

    override fun putFloat(value: Float): ByteBuffer {
        dirty = true
        val bits = value.toRawBits()
        this[position + 3] = (bits shr 24).toByte()
        this[position + 2] = (bits shr 16).toByte()
        this[position + 1] = (bits shr 8).toByte()
        this[position + 0] = bits.toByte()
        position += 4
        return this
    }

    override fun putFloat(offset: Int, value: Float): ByteBuffer {
        dirty = true
        val bits = value.toRawBits()
        this[offset + 3] = (bits shr 24).toByte()
        this[offset + 2] = (bits shr 16).toByte()
        this[offset + 1] = (bits shr 8).toByte()
        this[offset + 0] = bits.toByte()
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
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            bytes[i] = segment.pointer.getByte((offset + i).toLong())
        }
        return String(bytes)
    }

    override fun getOffset(offset: Int, offSize: Int): Int {
        var v = 0
        for (i in 0 until offSize) {
            v = v shl 8
            v += segment.pointer.getByte((offset + i).toLong()).toInt() and 0xFF
        }
        return v
    }
}

actual fun ShortBuffer(capacity: Int): ShortBuffer = ShortBufferImpl(capacity)

actual fun ShortBuffer(array: ShortArray): ShortBuffer = ShortBufferImpl(array)

actual fun IntBuffer(capacity: Int): IntBuffer = IntBufferImpl(capacity)

actual fun IntBuffer(array: IntArray): IntBuffer = IntBufferImpl(array)

actual fun FloatBuffer(capacity: Int): FloatBuffer = FloatBufferImpl(capacity)

actual fun FloatBuffer(array: FloatArray): FloatBuffer = FloatBufferImpl(array)

actual fun ByteBuffer(capacity: Int): ByteBuffer = ByteBufferImpl(capacity)

actual fun ByteBuffer(array: ByteArray): ByteBuffer = ByteBufferImpl(array)

actual fun ByteBuffer(array: ByteArray, isBigEndian: Boolean): ByteBuffer =
    ByteBufferImpl(array, isBigEndian)
