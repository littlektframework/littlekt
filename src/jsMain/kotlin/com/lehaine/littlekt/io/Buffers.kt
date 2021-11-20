package com.lehaine.littlekt.io

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
actual class ByteBuffer private constructor(actual override val capacity: Int) : Buffer {

    init {
        require(capacity >= 0)
    }

    private val dw = DataView(ArrayBuffer(capacity), 0, capacity)
    private var mark = UNSET_MARK

    actual override val hasRemaining: Boolean
        get() = position < limit

    actual override val remaining: Int
        get() = limit - position

    actual override var limit: Int = capacity
        set(value) {
            require(value in 0..capacity)
            field = value
            if (position > value) {
                position = value
            }
            if (mark != UNSET_MARK && mark > value) {
                mark = UNSET_MARK
            }
        }

    actual override var position: Int = 0
        set(newPosition) {
            require(newPosition in 0..limit)
            field = newPosition
            if (mark != UNSET_MARK && mark > position) {
                mark = UNSET_MARK
            }
        }

    actual fun clear(): ByteBuffer {
        position = 0
        limit = capacity
        mark = UNSET_MARK
        return this
    }

    actual fun flip(): ByteBuffer {
        limit = position
        position = 0
        mark = UNSET_MARK
        return this
    }

    actual fun mark(): ByteBuffer {
        mark = position
        return this
    }

    actual fun reset(): ByteBuffer {
        if (mark == UNSET_MARK) {
            throw InvalidMarkException()
        }
        position = mark
        return this
    }

    fun rewind(): ByteBuffer {
        position = 0
        mark = UNSET_MARK
        return this
    }

    var order: ByteOrder = ByteOrder.BIG_ENDIAN
    actual fun order(order: ByteOrder): ByteBuffer {
        this.order = order
        return this
    }

    private fun idx(index: Int, size: Int): Int {
        val i = if (index == -1) {
            position += size
            position - size
        } else index
        if (i > limit) throw IllegalArgumentException()
        return i
    }

    actual fun get(): Byte = get(-1)
    actual fun get(index: Int): Byte {
        val i = idx(index, 1)
        return dw.getInt8(i)
    }

    actual fun get(dst: ByteArray, offset: Int, cnt: Int): Unit {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dst[offset + i] = dw.getInt8(pos + i)
        }
    }

    actual fun getChar() = getChar(-1)
    actual fun getChar(index: Int): Char {
        val i = idx(index, 2)
        return dw.getUint16(i, order == ByteOrder.LITTLE_ENDIAN).toChar()
    }

    actual fun getShort() = getShort(-1)
    actual fun getShort(index: Int): Short {
        val i = idx(index, 2)
        return dw.getInt16(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    actual fun getInt() = getInt(-1)
    actual fun getInt(index: Int): Int {
        val i = idx(index, 4)
        return dw.getInt32(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    actual fun getLong() = getLong(-1)
    actual fun getLong(index: Int): Long {
        val low: Int
        val high: Int
        val scndIdx = if (index == -1) -1 else index + 4
        if (order == ByteOrder.LITTLE_ENDIAN) {
            low = getInt(index)
            high = getInt(scndIdx)
        } else {
            high = getInt(index)
            low = getInt(scndIdx)
        }
        return ((high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFF))
    }

    actual fun getFloat() = getFloat(-1)
    actual fun getFloat(index: Int): Float {
        val i = idx(index, 4)
        return dw.getFloat32(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    actual fun getDouble() = getDouble(-1)
    actual fun getDouble(index: Int): Double {
        val i = idx(index, 8)
        return dw.getFloat64(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    actual fun put(value: Byte): ByteBuffer = put(value, -1)
    actual fun put(value: Byte, index: Int): ByteBuffer {
        val i = idx(index, 1)
        dw.setInt8(i, value)
        return this
    }

    actual fun put(src: ByteArray) = put(src, 0, src.size)
    actual fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dw.setInt8(pos + i, src[offset + i])
        }
        return this
    }

    actual fun putChar(value: Char) = putChar(value, -1)
    actual fun putChar(value: Char, index: Int): ByteBuffer {
        val i = idx(index, 2)
        dw.setUint16(i, value.code.toShort(), order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    actual fun putShort(value: Short) = putShort(value, -1)
    actual fun putShort(value: Short, index: Int): ByteBuffer {
        val i = idx(index, 2)
        dw.setInt16(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    actual fun putInt(value: Int) = putInt(value, -1)
    actual fun putInt(value: Int, index: Int): ByteBuffer {
        val i = idx(index, 4)
        dw.setInt32(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    actual fun putLong(value: Long) = putLong(value, -1)
    actual fun putLong(value: Long, index: Int): ByteBuffer {
        val high = (value shr 32).toInt()
        val low = (value and 0xFFFFFFFFL).toInt()
        val scndIdx = if (index == -1) -1 else index + 4
        if (order == ByteOrder.LITTLE_ENDIAN) {
            putInt(low, index)
            putInt(high, scndIdx)
        } else {
            putInt(high, index)
            putInt(low, scndIdx)
        }
        return this
    }

    actual fun putFloat(value: Float) = putFloat(value, -1)
    actual fun putFloat(value: Float, index: Int): ByteBuffer {
        val i = idx(index, 4)
        dw.setFloat32(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    actual fun putDouble(value: Double) = putDouble(value, -1)
    actual fun putDouble(value: Double, index: Int): ByteBuffer {
        val i = idx(index, 8)
        dw.setFloat64(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    actual fun array(): ByteArray {
        val out = ByteArray(limit)
        for (i in 0 until limit) {
            out[i] = dw.getInt8(i)
        }
        return out
    }

    actual fun asFloatBuffer(): FloatBuffer = FloatBuffer.createFrom(dw)

    actual companion object {
        const val UNSET_MARK = -1
        actual fun allocate(capacity: Int) = ByteBuffer(capacity)
    }
}

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
actual class FloatBuffer private constructor(
    actual override val capacity: Int,
    private val dw: DataView = DataView(ArrayBuffer(capacity), 0, capacity)
) : Buffer {

    init {
        require(capacity >= 0)
    }

    private var mark = UNSET_MARK

    actual override val hasRemaining: Boolean
        get() = position < limit

    actual override val remaining: Int
        get() = limit - position

    actual override var limit: Int = capacity
        set(value) {
            require(value in 0..capacity)
            field = value
            if (position > value) {
                position = value
            }
            if (mark != UNSET_MARK && mark > value) {
                mark = UNSET_MARK
            }
        }

    actual override var position: Int = 0
        set(newPosition) {
            require(newPosition in 0..limit)
            field = newPosition
            if (mark != UNSET_MARK && mark > position) {
                mark = UNSET_MARK
            }
        }

    actual fun clear(): FloatBuffer {
        position = 0
        limit = capacity
        mark = UNSET_MARK
        return this
    }

    actual fun flip(): FloatBuffer {
        limit = position
        position = 0
        mark = UNSET_MARK
        return this
    }

    actual fun mark(): FloatBuffer {
        mark = position
        return this
    }

    actual fun reset(): FloatBuffer {
        if (mark == UNSET_MARK) {
            throw InvalidMarkException()
        }
        position = mark
        return this
    }

    fun rewind(): FloatBuffer {
        position = 0
        mark = UNSET_MARK
        return this
    }

    private fun idx(index: Int, size: Int): Int {
        val i = if (index == -1) {
            position += size
            position - size
        } else index
        if (i > limit) throw IllegalArgumentException()
        return i
    }

    actual fun get(): Float = get(-1)
    actual fun get(index: Int): Float {
        val i = idx(index, 1)
        return dw.getFloat32(i)
    }

    actual fun get(dst: FloatArray, offset: Int, cnt: Int): FloatBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dst[offset + i] = dw.getFloat32(pos + i)
        }
        return this
    }

    actual fun put(value: Float): FloatBuffer = put(value, -1)
    actual fun put(value: Float, index: Int): FloatBuffer {
        val i = idx(index, 1)
        dw.setFloat32(i, value)
        return this
    }

    actual fun put(src: FloatArray) = put(src, 0, src.size)

    actual fun put(src: FloatArray, offset: Int, cnt: Int): FloatBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dw.setFloat32(pos + i, src[offset + i])
        }
        return this
    }

    actual fun array(): FloatArray {
        val out = FloatArray(limit)
        for (i in 0 until limit) {
            out[i] = dw.getFloat32(i)
        }
        return out
    }

    actual companion object {
        const val UNSET_MARK = -1
        fun createFrom(dw: DataView) =
            FloatBuffer(dw.byteLength, DataView(dw.buffer.slice(0, dw.buffer.byteLength), dw.byteOffset, dw.byteLength))

        actual fun allocate(capacity: Int) = FloatBuffer(capacity)
    }
}

/**
 * @author Colton Daily
 * @date 11/19/2021
 */
actual class ShortBuffer private constructor(
    actual override val capacity: Int,
    private val dw: DataView = DataView(ArrayBuffer(capacity), 0, capacity)
) : Buffer {

    init {
        require(capacity >= 0)
    }

    private var mark = UNSET_MARK

    actual override val hasRemaining: Boolean
        get() = position < limit

    actual override val remaining: Int
        get() = limit - position

    actual override var limit: Int = capacity
        set(value) {
            require(value in 0..capacity)
            field = value
            if (position > value) {
                position = value
            }
            if (mark != UNSET_MARK && mark > value) {
                mark = UNSET_MARK
            }
        }

    actual override var position: Int = 0
        set(newPosition) {
            require(newPosition in 0..limit)
            field = newPosition
            if (mark != UNSET_MARK && mark > position) {
                mark = UNSET_MARK
            }
        }

    actual fun clear(): ShortBuffer {
        position = 0
        limit = capacity
        mark = UNSET_MARK
        return this
    }

    actual fun flip(): ShortBuffer {
        limit = position
        position = 0
        mark = UNSET_MARK
        return this
    }

    actual fun mark(): ShortBuffer {
        mark = position
        return this
    }

    actual fun reset(): ShortBuffer {
        if (mark == UNSET_MARK) {
            throw InvalidMarkException()
        }
        position = mark
        return this
    }

    fun rewind(): ShortBuffer {
        position = 0
        mark = UNSET_MARK
        return this
    }

    private fun idx(index: Int, size: Int): Int {
        val i = if (index == -1) {
            position += size
            position - size
        } else index
        if (i > limit) throw IllegalArgumentException()
        return i
    }

    actual fun get(): Short = get(-1)
    actual fun get(index: Int): Short {
        val i = idx(index, 1)
        return dw.getInt16(i)
    }

    actual fun get(dst: ShortArray, offset: Int, cnt: Int): ShortBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dst[offset + i] = dw.getInt16(pos + i)
        }
        return this
    }

    actual fun put(value: Short): ShortBuffer = put(value, -1)
    actual fun put(value: Short, index: Int): ShortBuffer {
        val i = idx(index, 1)
        dw.setInt16(i, value)
        return this
    }

    actual fun put(src: ShortArray) = put(src, 0, src.size)

    actual fun put(src: ShortArray, offset: Int, cnt: Int): ShortBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dw.setUint16(pos + i, src[offset + i])
        }
        return this
    }

    actual fun array(): ShortArray {
        val out = ShortArray(limit)
        for (i in 0 until limit) {
            out[i] = dw.getInt16(i)
        }
        return out
    }

    actual companion object {
        const val UNSET_MARK = -1
        actual fun allocate(capacity: Int) = ShortBuffer(capacity)
    }
}

class InvalidMarkException : IllegalStateException()