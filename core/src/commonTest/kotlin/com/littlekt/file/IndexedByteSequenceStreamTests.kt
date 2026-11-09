package com.littlekt.file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IndexedByteSequenceStreamTests {

    private fun bytes(vararg values: Int) = ByteBuffer(ByteArray(values.size) { values[it].toByte() })

    @Test
    fun test_read_byte_and_ubyte() {
        val stream = IndexedByteSequenceStream(bytes(0xFF, 0xFF))
        assertEquals(-1, stream.readByte())
        assertEquals(255, stream.readUByte())
        assertEquals(2, stream.index)
    }

    @Test
    fun test_read_short_is_little_endian_and_signed() {
        val stream = IndexedByteSequenceStream(bytes(0x02, 0x01, 0xFE, 0xFF))
        assertEquals(0x0102, stream.readShort())
        assertEquals(-2, stream.readShort())
    }

    @Test
    fun test_read_ushort() {
        val stream = IndexedByteSequenceStream(bytes(0xFE, 0xFF))
        assertEquals(65534, stream.readUShort())
    }

    @Test
    fun test_read_int_is_little_endian() {
        val stream = IndexedByteSequenceStream(bytes(0x04, 0x03, 0x02, 0x01, 0xFF, 0xFF, 0xFF, 0xFF))
        assertEquals(0x01020304, stream.readInt())
        assertEquals(-1, stream.readUInt())
    }

    @Test
    fun test_read_float() {
        val stream = IndexedByteSequenceStream(ByteBuffer(ByteArray(4).put(-0.75f, 0)))
        assertEquals(-0.75f, stream.readFloat())
    }

    @Test
    fun test_step_offsets_reads() {
        val stream = IndexedByteSequenceStream(bytes(9, 9, 0x05, 0x00), step = 2)
        assertEquals(5, stream.readUShort())
    }

    @Test
    fun test_index_can_be_set_directly() {
        val stream = IndexedByteSequenceStream(bytes(1, 2, 3, 4))
        stream.index = 3
        assertEquals(4, stream.readByte())
    }

    @Test
    fun test_skip_advances_index() {
        val stream = IndexedByteSequenceStream(bytes(1, 2, 3, 4))
        stream.skip(2)
        assertEquals(3, stream.readByte())
    }

    @Test
    fun test_reset_rewinds_index() {
        val stream = IndexedByteSequenceStream(bytes(7, 8))
        stream.readByte()
        stream.reset()
        assertEquals(0, stream.index)
        assertEquals(7, stream.readByte())
    }

    @Test
    fun test_negative_step_throws() {
        assertFailsWith<IllegalArgumentException> { IndexedByteSequenceStream(bytes(1), step = -1) }
    }

    @Test
    fun test_close_twice_throws() {
        val stream = IndexedByteSequenceStream(bytes(1))
        stream.close()
        assertFailsWith<IllegalStateException> { stream.close() }
    }
}
