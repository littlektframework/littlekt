package com.littlekt.file.font.ttf

import com.littlekt.file.ByteBuffer
import com.littlekt.file.font.ttf.table.KerningTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KerningTableTests {

    private class Writer {
        val bytes = mutableListOf<Byte>()
        val size: Int
            get() = bytes.size

        fun u16(value: Int) = apply {
            bytes += (value ushr 8 and 0xFF).toByte()
            bytes += (value and 0xFF).toByte()
        }

        fun u32(value: Int) = apply {
            u16(value ushr 16)
            u16(value and 0xFFFF)
        }

        fun tag(value: String) = apply { value.forEach { bytes += it.code.toByte() } }

        fun set16(offset: Int, value: Int) {
            bytes[offset] = (value ushr 8 and 0xFF).toByte()
            bytes[offset + 1] = (value and 0xFF).toByte()
        }

        fun buffer() = ByteBuffer(bytes.toByteArray(), isBigEndian = true)
    }

    @Test
    fun test_kern_table_format_0_pairs() {
        val w = Writer().u16(0).u16(1)
        w.u16(0).u16(6 + 8 + 2 * 6).u16(0x0001)
        w.u16(2).u16(12).u16(1).u16(0)
        w.u16(36).u16(57).u16(-80 and 0xFFFF)
        w.u16(55).u16(68).u16(-40 and 0xFFFF)
        val table = KerningTable().apply { parseKern(w.buffer(), 0) }
        assertEquals(-80, table[36, 57])
        assertEquals(-40, table[55, 68])
        assertEquals(0, table[57, 36])
    }

    @Test
    fun test_kern_table_skips_vertical_subtables() {
        val w = Writer().u16(0).u16(1)
        w.u16(0).u16(6 + 8 + 6).u16(0x0000)
        w.u16(1).u16(6).u16(0).u16(0)
        w.u16(1).u16(2).u16(-50 and 0xFFFF)
        val table = KerningTable().apply { parseKern(w.buffer(), 0) }
        assertTrue(table.isEmpty)
    }

    private fun gpos(subtable: Writer.() -> Unit): ByteBuffer {
        val w = Writer()
        w.u16(1).u16(0).u16(0).u16(10).u16(0)
        w.u16(1).tag("kern").u16(8)
        w.u16(0).u16(1).u16(0)
        val lookupList = w.size
        w.set16(8, lookupList)
        w.u16(1).u16(4)
        w.u16(2).u16(0).u16(1).u16(8)
        w.subtable()
        return w.buffer()
    }

    @Test
    fun test_gpos_pair_format_1() {
        val buffer = gpos {
            val start = size
            u16(1).u16(0).u16(0x0004).u16(0).u16(1).u16(0)
            set16(start + 2, size - start)
            u16(1).u16(1).u16(10)
            set16(start + 10, size - start)
            u16(1).u16(20).u16(-70 and 0xFFFF)
        }
        val table = KerningTable().apply { parseGpos(buffer, 0) }
        assertEquals(-70, table[10, 20])
        assertEquals(0, table[10, 21])
    }

    @Test
    fun test_gpos_pair_format_2_classes() {
        val buffer = gpos {
            val start = size
            u16(2).u16(0).u16(0x0004).u16(0).u16(0).u16(0).u16(2).u16(2)
            for (c1 in 0 until 2) for (c2 in 0 until 2) u16(if (c1 == 1 && c2 == 1) -90 and 0xFFFF else 0)
            set16(start + 2, size - start)
            u16(1).u16(2).u16(30).u16(31)
            set16(start + 8, size - start)
            u16(1).u16(30).u16(2).u16(1).u16(1)
            set16(start + 10, size - start)
            u16(2).u16(1).u16(40).u16(45).u16(1)
        }
        val table = KerningTable().apply { parseGpos(buffer, 0) }
        assertEquals(-90, table[30, 42])
        assertEquals(-90, table[31, 45])
        assertEquals(0, table[30, 50])
        assertEquals(0, table[29, 42])
    }
}
