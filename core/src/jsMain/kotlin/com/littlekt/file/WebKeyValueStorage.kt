package com.littlekt.file

import com.littlekt.log.Logger
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * @author Colton Daily
 * @date 8/28/2024
 */
class WebKeyValueStorage(private val logger: Logger) : KeyValueStorage {

    override fun store(key: String, data: ByteArray): Boolean {
        return try {
            localStorage[key] = binToBase64(Uint8Array(data.toTypedArray()))
            true
        } catch (e: Exception) {
            logger.error { "Failed storing data '$key' to localStorage: $e" }
            false
        }
    }

    override fun store(key: String, data: String): Boolean {
        return try {
            localStorage[key] = data
            true
        } catch (e: Exception) {
            logger.error { "Failed storing string '$key' to localStorage: $e" }
            false
        }
    }

    override fun load(key: String): ByteBuffer? {
        return localStorage[key]?.let { ByteBufferImpl(base64ToBin(it)) }
    }

    override fun loadString(key: String): String? {
        return localStorage[key]
    }

    private val base64abc =
        arrayOf(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z",
            "a",
            "b",
            "c",
            "d",
            "e",
            "f",
            "g",
            "h",
            "i",
            "j",
            "k",
            "l",
            "m",
            "n",
            "o",
            "p",
            "q",
            "r",
            "s",
            "t",
            "u",
            "v",
            "w",
            "x",
            "y",
            "z",
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "+",
            "/"
        )

    private fun binToBase64(data: Uint8Array): String {
        var result = ""
        val l = data.length
        var j = 2
        for (i in 2 until l step 3) {
            j = i
            result += base64abc[data[i - 2].toInt() shr 2]
            result += base64abc[data[i - 2].toInt() and 0x03 shl 4 or (data[i - 1].toInt() shr 4)]
            result += base64abc[data[i - 1].toInt() and 0x0F shl 2 or (data[i].toInt() shr 6)]
            result += base64abc[data[i].toInt() and 0x3F]
        }
        if (j == l + 1) { // 1 octet yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4]
            result += "=="
        }
        if (j == l) { // 2 octets yet to write
            result += base64abc[data[j - 2].toInt() shr 2]
            result += base64abc[data[j - 2].toInt() and 0x03 shl 4 or (data[j - 1].toInt() shr 4)]
            result += base64abc[data[j - 1].toInt() and 0x0F shl 2]
            result += "="
        }
        return result
    }

    private fun base64ToBin(base64: String): Uint8Array {
        val binaryString = window.atob(base64)
        val bytes = Uint8Array(binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].digitToInt().toByte()
        }
        return bytes
    }
}
