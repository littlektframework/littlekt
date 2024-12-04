package com.littlekt.file.compression

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
