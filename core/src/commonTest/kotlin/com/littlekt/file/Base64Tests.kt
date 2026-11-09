package com.littlekt.file

import com.littlekt.file.Base64.decodeFromBase64
import com.littlekt.file.Base64.encodeToBase64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Base64Tests {

    @Test
    fun test_encode_rfc4648_vectors() {
        assertEquals("", "".encodeToByteArray().encodeToBase64())
        assertEquals("Zg==", "f".encodeToByteArray().encodeToBase64())
        assertEquals("Zm8=", "fo".encodeToByteArray().encodeToBase64())
        assertEquals("Zm9v", "foo".encodeToByteArray().encodeToBase64())
        assertEquals("Zm9vYg==", "foob".encodeToByteArray().encodeToBase64())
        assertEquals("Zm9vYmE=", "fooba".encodeToByteArray().encodeToBase64())
        assertEquals("Zm9vYmFy", "foobar".encodeToByteArray().encodeToBase64())
    }

    @Test
    fun test_decode_rfc4648_vectors() {
        assertEquals("", "".decodeFromBase64().decodeToString())
        assertEquals("f", "Zg==".decodeFromBase64().decodeToString())
        assertEquals("fo", "Zm8=".decodeFromBase64().decodeToString())
        assertEquals("foo", "Zm9v".decodeFromBase64().decodeToString())
        assertEquals("foob", "Zm9vYg==".decodeFromBase64().decodeToString())
        assertEquals("fooba", "Zm9vYmE=".decodeFromBase64().decodeToString())
        assertEquals("foobar", "Zm9vYmFy".decodeFromBase64().decodeToString())
    }

    @Test
    fun test_decode_without_padding() {
        assertEquals("f", "Zg".decodeFromBase64().decodeToString())
        assertEquals("fo", "Zm8".decodeFromBase64().decodeToString())
    }

    @Test
    fun test_round_trip_all_byte_values() {
        val bytes = ByteArray(256) { it.toByte() }
        assertContentEquals(bytes, bytes.encodeToBase64().decodeFromBase64())
    }

    @Test
    fun test_encode_uses_plus_and_slash() {
        assertEquals("+/8=", byteArrayOf(0xFB.toByte(), 0xFF.toByte()).encodeToBase64())
    }

    @Test
    fun test_decode_illegal_character_throws() {
        assertFailsWith<IllegalArgumentException> { "Zm9v*mFy".decodeFromBase64() }
    }

    @Test
    fun test_decode_dangling_single_character_throws() {
        assertFailsWith<IllegalArgumentException> { "Zm9vY".decodeFromBase64() }
    }
}
