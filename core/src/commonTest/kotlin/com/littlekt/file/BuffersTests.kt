package com.littlekt.file

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuffersTests {

    @Test
    fun test_byte_array_put_int_is_little_endian() {
        val bytes = ByteArray(4).put(0x01020304, 0)
        assertContentEquals(byteArrayOf(4, 3, 2, 1), bytes)
    }

    @Test
    fun test_byte_array_put_short_is_little_endian() {
        val bytes = ByteArray(3).put(0x0102.toShort(), 1)
        assertContentEquals(byteArrayOf(0, 2, 1), bytes)
    }

    @Test
    fun test_byte_array_put_float_writes_raw_bits() {
        val bytes = ByteArray(4).put(1f, 0)
        assertContentEquals(byteArrayOf(0, 0, 0x80.toByte(), 0x3F), bytes)
    }

    @Test
    fun test_byte_array_to_int_array() {
        val bytes = byteArrayOf(4, 3, 2, 1, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        assertContentEquals(intArrayOf(0x01020304, -1), bytes.toIntArray())
    }

    @Test
    fun test_byte_array_to_int_array_high_bit_in_low_byte() {
        val bytes = byteArrayOf(0x81.toByte(), 0, 0, 0, 0x01, 0x80.toByte(), 0, 0x80.toByte())
        assertContentEquals(intArrayOf(0x81, 0x80008001.toInt()), bytes.toIntArray())
    }

    @Test
    fun test_byte_array_to_int_array_ignores_trailing_bytes() {
        assertContentEquals(intArrayOf(1), byteArrayOf(1, 0, 0, 0, 9, 9).toIntArray())
    }

    @Test
    fun test_int_array_byte_array_round_trip() {
        val ints = intArrayOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE, 0x7F80FF01)
        assertContentEquals(ints, ints.toByteArray().toIntArray())
    }

    @Test
    fun test_float_array_byte_array_round_trip() {
        val floats = floatArrayOf(0f, -1.5f, 3.25f, Float.MAX_VALUE, Float.MIN_VALUE)
        assertContentEquals(floats, floats.toByteArray().toFloatArray())
    }

    @Test
    fun test_short_array_to_byte_array() {
        val bytes = shortArrayOf(0x0102, -1).toByteArray()
        assertContentEquals(byteArrayOf(2, 1, 0xFF.toByte(), 0xFF.toByte()), bytes)
    }

    @Test
    fun test_byte_buffer_put_advances_position() {
        val buffer = ByteBuffer(16)
        buffer.putByte(1)
        buffer.putShort(2)
        buffer.putInt(3)
        buffer.putFloat(4f)
        assertEquals(11, buffer.position)
        assertEquals(5, buffer.remaining)
    }

    @Test
    fun test_byte_buffer_absolute_reads() {
        val buffer = ByteBuffer(16)
        buffer.putByte(-2)
        buffer.putShort(-300)
        buffer.putInt(123456789)
        buffer.putFloat(2.5f)
        assertEquals((-2).toByte(), buffer.getByte(0))
        assertEquals(254.toUByte(), buffer.getUByte(0))
        assertEquals((-300).toShort(), buffer.getShort(1))
        assertEquals((-300).toUShort(), buffer.getUShort(1))
        assertEquals(123456789, buffer.getInt(3))
        assertEquals(2.5f, buffer.getFloat(7))
    }

    @Test
    fun test_byte_buffer_relative_reads_after_flip() {
        val buffer = ByteBuffer(10)
        buffer.putByte(7)
        buffer.putShort(513)
        buffer.putInt(-5)
        buffer.flip()
        assertEquals(7.toByte(), buffer.readByte)
        assertEquals(513.toShort(), buffer.readShort)
        assertEquals(-5, buffer.readInt)
        assertEquals(0, buffer.remaining)
    }

    @Test
    fun test_byte_buffer_from_array_is_little_endian() {
        val buffer = ByteBuffer(byteArrayOf(4, 3, 2, 1))
        assertEquals(0x01020304, buffer.getInt(0))
        assertEquals(0x0304, buffer.getShort(0).toInt())
    }

    @Test
    fun test_byte_buffer_unaligned_reads() {
        val buffer = ByteBuffer(byteArrayOf(0, 4, 3, 2, 1, 0, 0, 0x80.toByte(), 0x3F))
        assertEquals(0x01020304, buffer.getInt(1))
        assertEquals(0x0304.toShort(), buffer.getShort(1))
        assertEquals(1f, buffer.getFloat(5))
    }

    @Test
    fun test_byte_buffer_from_array_sets_position_to_end() {
        val buffer = ByteBuffer(byteArrayOf(1, 2, 3))
        assertEquals(3, buffer.capacity)
        assertEquals(3, buffer.position)
    }

    @Test
    fun test_byte_buffer_set_int_at_offset() {
        val buffer = ByteBuffer(8)
        buffer[4] = 0x01020304
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 4, 3, 2, 1), buffer.toArray())
    }

    @Test
    fun test_byte_buffer_put_offset_sets_position_after_value() {
        val buffer = ByteBuffer(8)
        buffer.putInt(2, 9)
        assertEquals(6, buffer.position)
        assertEquals(9, buffer.getInt(2))
    }

    @Test
    fun test_byte_buffer_put_byte_array_with_offset_and_len() {
        val buffer = ByteBuffer(4)
        buffer.putByte(byteArrayOf(1, 2, 3, 4, 5), srcOffset = 1, len = 3)
        assertEquals(3, buffer.position)
        assertContentEquals(byteArrayOf(2, 3, 4, 0), buffer.toArray())
    }

    @Test
    fun test_byte_buffer_put_byte_array_at_dst_offset() {
        val buffer = ByteBuffer(4)
        buffer.putByte(byteArrayOf(8, 9), dstOffset = 2)
        assertContentEquals(byteArrayOf(0, 0, 8, 9), buffer.toArray())
    }

    @Test
    fun test_byte_buffer_put_int_array_with_offset() {
        val buffer = ByteBuffer(8)
        buffer.putInt(intArrayOf(1, 2, 3), 1, 2)
        assertEquals(2, buffer.getInt(0))
        assertEquals(3, buffer.getInt(4))
    }

    @Test
    fun test_byte_buffer_put_float_array_with_offset() {
        val buffer = ByteBuffer(8)
        buffer.putFloat(floatArrayOf(1f, 2f, 3f), 1, 2)
        assertEquals(2f, buffer.getFloat(0))
        assertEquals(3f, buffer.getFloat(4))
    }

    @Test
    fun test_byte_buffer_put_short_array() {
        val buffer = ByteBuffer(4)
        buffer.putShort(shortArrayOf(1, -2))
        assertEquals(1.toShort(), buffer.getShort(0))
        assertEquals((-2).toShort(), buffer.getShort(2))
    }

    @Test
    fun test_byte_buffer_put_other_byte_buffer_copies_position_to_limit() {
        val src = ByteBuffer(byteArrayOf(1, 2, 3, 4))
        src.position = 1
        src.limit = 3
        val dst = ByteBuffer(4)
        dst.putByte(src)
        assertEquals(2, dst.position)
        assertContentEquals(byteArrayOf(2, 3, 0, 0), dst.toArray())
    }

    @Test
    fun test_byte_buffer_get_byte_array() {
        val buffer = ByteBuffer(byteArrayOf(1, 2, 3, 4))
        assertContentEquals(byteArrayOf(2, 3), buffer.getByteArray(1, 3))
    }

    @Test
    fun test_byte_buffer_get_string() {
        val buffer = ByteBuffer("xxglyf".encodeToByteArray())
        assertEquals("glyf", buffer.getString(2, 4))
    }

    @Test
    fun test_byte_buffer_big_endian_reads() {
        val buffer = ByteBuffer(byteArrayOf(0x01, 0x02, 0x03, 0x04), isBigEndian = true)
        assertEquals(0x0102, buffer.getUShort(0).toInt())
        assertEquals(0x0304.toShort(), buffer.getShort(2))
        assertEquals(0x01020304, buffer.getInt(0))
    }

    @Test
    fun test_byte_buffer_get_offset_reads_big_endian() {
        val buffer = ByteBuffer(byteArrayOf(0x01, 0x02, 0x03))
        assertEquals(0x0102, buffer.getOffset(0, 2))
        assertEquals(0x010203, buffer.getOffset(0, 3))
        assertEquals(0x03, buffer.getOffset(2, 1))
    }

    @Test
    fun test_byte_buffer_get_offset_treats_bytes_as_unsigned() {
        val buffer = ByteBuffer(byteArrayOf(0x01, 0xFF.toByte()))
        assertEquals(0x01FF, buffer.getOffset(0, 2))
    }

    @Test
    fun test_byte_buffer_dirty_after_put() {
        val buffer = ByteBuffer(4)
        buffer.dirty = false
        buffer.putByte(1)
        assertTrue(buffer.dirty)
    }

    @Test
    fun test_buffer_flip() {
        val buffer = IntBuffer(8)
        buffer.put(1).put(2).put(3)
        buffer.flip()
        assertEquals(0, buffer.position)
        assertEquals(3, buffer.limit)
        assertEquals(3, buffer.remaining)
    }

    @Test
    fun test_buffer_clear_resets_position_and_limit() {
        val buffer = IntBuffer(8)
        buffer.put(1).put(2)
        buffer.flip()
        buffer.clear()
        assertEquals(0, buffer.position)
        assertEquals(8, buffer.limit)
        assertEquals(1, buffer[0])
    }

    @Test
    fun test_buffer_limit_clamps_position() {
        val buffer = FloatBuffer(8)
        buffer.position = 6
        buffer.limit = 4
        assertEquals(4, buffer.position)
    }

    @Test
    fun test_int_buffer_put_array_with_offset_and_len() {
        val buffer = IntBuffer(4)
        buffer.put(intArrayOf(1, 2, 3, 4, 5), srcOffset = 1, len = 2)
        assertEquals(2, buffer.position)
        assertContentEquals(intArrayOf(2, 3, 0, 0), buffer.toArray())
    }

    @Test
    fun test_short_buffer_put_array_with_offset_and_len() {
        val buffer = ShortBuffer(4)
        buffer.put(shortArrayOf(1, 2, 3, 4, 5), srcOffset = 2, len = 2)
        assertEquals(2, buffer.position)
        assertContentEquals(shortArrayOf(3, 4, 0, 0), buffer.toArray())
    }

    @Test
    fun test_float_buffer_put_array_at_dst_offset() {
        val buffer = FloatBuffer(4)
        buffer.put(floatArrayOf(1f, 2f), dstOffset = 1)
        assertEquals(3, buffer.position)
        assertContentEquals(floatArrayOf(0f, 1f, 2f, 0f), buffer.toArray())
    }

    @Test
    fun test_float_buffer_put_buffer_range() {
        val src = FloatBuffer(floatArrayOf(1f, 2f, 3f, 4f))
        val dst = FloatBuffer(4)
        dst.put(src, dstOffset = 1, srcOffset = 2, len = 2)
        assertEquals(3, dst.position)
        assertContentEquals(floatArrayOf(0f, 3f, 4f, 0f), dst.toArray())
    }

    @Test
    fun test_float_buffer_put_buffer_copies_position_to_limit() {
        val src = FloatBuffer(floatArrayOf(1f, 2f, 3f))
        src.flip()
        src.position = 1
        val dst = FloatBuffer(3)
        dst.put(src)
        assertContentEquals(floatArrayOf(2f, 3f, 0f), dst.toArray())
    }

    @Test
    fun test_buffer_from_array_copies_values() {
        val buffer = IntBuffer(intArrayOf(5, 6, 7))
        assertEquals(3, buffer.capacity)
        assertEquals(3, buffer.position)
        assertContentEquals(intArrayOf(5, 6, 7), buffer.toArray())
    }

    @Test
    fun test_int_buffer_remove_at_shifts_elements() {
        val buffer = IntBuffer(8)
        buffer.put(intArrayOf(1, 2, 3, 4))
        buffer.removeAt(1)
        assertEquals(3, buffer.position)
        assertContentEquals(intArrayOf(1, 3, 4), buffer.toArray(0, 3))
    }

    @Test
    fun test_int_buffer_remove_at_when_full() {
        val buffer = IntBuffer(intArrayOf(1, 2, 3))
        buffer.removeAt(0)
        assertEquals(2, buffer.position)
        assertEquals(2, buffer.limit)
        assertContentEquals(intArrayOf(2, 3), buffer.toArray(0, 2))
    }

    @Test
    fun test_short_buffer_remove_at_when_full() {
        val buffer = ShortBuffer(shortArrayOf(1, 2, 3))
        buffer.removeAt(2)
        assertEquals(2, buffer.position)
        assertContentEquals(shortArrayOf(1, 2), buffer.toArray(0, 2))
    }

    @Test
    fun test_float_buffer_remove_at_when_full() {
        val buffer = FloatBuffer(floatArrayOf(1f, 2f, 3f))
        buffer.removeAt(1)
        assertEquals(2, buffer.position)
        assertContentEquals(floatArrayOf(1f, 3f), buffer.toArray(0, 2))
    }

    @Test
    fun test_buffer_remove_at_after_position_keeps_position() {
        val buffer = IntBuffer(8)
        buffer.put(1).put(2)
        buffer.removeAt(5)
        assertEquals(2, buffer.position)
        assertEquals(7, buffer.limit)
    }

    @Test
    fun test_to_stream_reads_little_endian() {
        val bytes = ByteArray(10)
        bytes.put(0x0102.toShort(), 0)
        bytes.put(-7, 2)
        bytes.put(1.5f, 6)
        val stream = ByteBuffer(bytes).toStream()
        assertEquals(0x0102, stream.readUShort())
        assertEquals(-7, stream.readInt())
        assertEquals(1.5f, stream.readFloat())
        assertFalse(stream.hasRemaining())
    }
}
