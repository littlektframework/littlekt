package com.lehaine.littlekt.io

/**
 * @author Colton Daily
 * @date 11/19/2021
 */

interface Buffer {
    var limit: Int
    val remaining: Int
    val capacity: Int
    var position: Int
    val hasRemaining: Boolean
    fun <T : Buffer> T.flip(): T
    fun <T : Buffer> T.mark(): T
    fun <T : Buffer> T.reset(): T
}

enum class ByteOrder {
    LITTLE_ENDIAN, BIG_ENDIAN
}

expect class ByteBuffer : Buffer {

    override var limit: Int
    override val remaining: Int
    override val capacity: Int
    override var position: Int
    override val hasRemaining: Boolean

    override fun <T : Buffer> T.flip(): T
    override fun <T : Buffer> T.mark(): T
    override fun <T : Buffer> T.reset(): T

    fun order(order: ByteOrder): ByteBuffer
    fun clear(): ByteBuffer

    fun get(): Byte
    fun get(index: Int): Byte
    fun get(dst: ByteArray, offset: Int, cnt: Int): Unit
    fun getChar(): Char
    fun getChar(index: Int): Char
    fun getShort(): Short
    fun getShort(index: Int): Short
    fun getInt(): Int
    fun getInt(index: Int): Int
    fun getLong(): Long
    fun getLong(index: Int): Long
    fun getFloat(): Float
    fun getFloat(index: Int): Float
    fun getDouble(): Double
    fun getDouble(index: Int): Double

    fun put(value: Byte): ByteBuffer
    fun put(value: Byte, index: Int): ByteBuffer
    fun put(src: ByteArray): ByteBuffer
    fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer

    fun putChar(value: Char): ByteBuffer
    fun putChar(value: Char, index: Int): ByteBuffer
    fun putShort(value: Short): ByteBuffer
    fun putShort(value: Short, index: Int): ByteBuffer
    fun putInt(value: Int): ByteBuffer
    fun putInt(value: Int, index: Int): ByteBuffer
    fun putLong(value: Long): ByteBuffer
    fun putLong(value: Long, index: Int): ByteBuffer
    fun putFloat(value: Float): ByteBuffer
    fun putFloat(value: Float, index: Int): ByteBuffer
    fun putDouble(value: Double): ByteBuffer
    fun putDouble(value: Double, index: Int): ByteBuffer

    fun array(): ByteArray


    companion object {
        fun allocate(capacity: Int): ByteBuffer
    }
}

expect class FloatBuffer : Buffer {
    override var limit: Int
    override val remaining: Int
    override val capacity: Int
    override var position: Int
    override val hasRemaining: Boolean
    override fun <T : Buffer> T.flip(): T
    override fun <T : Buffer> T.mark(): T
    override fun <T : Buffer> T.reset(): T

    fun clear(): FloatBuffer

    fun get(): Float
    fun get(index: Int): Float
    fun get(dst: FloatArray, offset: Int = 0, cnt: Int = dst.size): FloatBuffer

    fun put(value: Float): FloatBuffer
    fun put(value: Float, index: Int): FloatBuffer
    fun put(src: FloatArray): FloatBuffer
    fun put(src: FloatArray, offset: Int = 0, cnt: Int = src.size): FloatBuffer

    fun array(): FloatArray

    companion object {
        fun allocate(capacity: Int): FloatBuffer
    }
}