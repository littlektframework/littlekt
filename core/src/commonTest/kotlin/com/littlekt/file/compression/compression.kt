package com.littlekt.file.compression

import kotlin.test.BeforeTest
import com.littlekt.file.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * @author Colton Daily
 * @date 12/2/2024
 */
class CompressionGZIPTests {
    private var compression = CompressionGZIP()

    @BeforeTest
    fun setup() {
        compression = CompressionGZIP()
    }

    @Test
    fun test_gzip_compress_and_decompress_byte_array() = runTest {
        val string = buildString { repeat(5000) { append("mystring") } }
        val data = string.encodeToByteArray()
        val compressedData = compression.compress(data)
        val decompressedData = compression.decompress(compressedData)
        assertEquals(string, decompressedData.decodeToString())
    }

    @Test
    fun test_gzip_compress_writes_gzip_header() = runTest {
        val compressed = compression.compress("hello".encodeToByteArray())
        assertEquals(0x1F.toByte(), compressed[0])
        assertEquals(0x8B.toByte(), compressed[1])
    }

    @Test
    fun test_gzip_compress_shrinks_repetitive_data() = runTest {
        val data = ByteArray(10000) { 7 }
        assertTrue(compression.compress(data).size < data.size)
    }

    @Test
    fun test_gzip_round_trip_empty_byte_array() = runTest {
        val decompressed = compression.decompress(compression.compress(ByteArray(0)))
        assertEquals(0, decompressed.size)
    }

    @Test
    fun test_gzip_round_trip_binary_bytes() = runTest {
        val data = ByteArray(1024) { (it * 31).toByte() }
        assertContentEquals(data, compression.decompress(compression.compress(data)))
    }

    @Test
    fun test_gzip_round_trip_byte_buffer() = runTest {
        val data = ByteArray(300) { it.toByte() }
        val decompressed = compression.decompress(compression.compress(ByteBuffer(data)))
        assertEquals(data.size, decompressed.capacity)
        assertContentEquals(data, decompressed.toArray())
    }
}
