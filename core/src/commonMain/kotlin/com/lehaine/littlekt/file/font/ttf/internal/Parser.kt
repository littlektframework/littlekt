package com.lehaine.littlekt.file.font.ttf.internal

import com.lehaine.littlekt.file.MixedBuffer

/**
 * Parsing Util functions
 * @author Colton Daily
 * @date 11/30/2021
 */

internal enum class Type(val size: Int) {
    BYTE(1),
    SHORT(2),
    INT(4),
    FLOAT(4),
    LONG_DATE_TIME(8),
    TAG(4)
}

internal class Parser(private val buffer: MixedBuffer, offset: Int) {
    var offset = offset
        private set

    var relativeOffset = 0

    val parseByte get() = buffer.getUint8(offset + relativeOffset++)
    val parseChar get() = buffer.getInt8(offset + relativeOffset++).toInt().toChar()
    val parseCard8 get() = parseByte

    val parseUint16 get() = buffer.getUint16(offset + relativeOffset).also { relativeOffset += 2 }
    val parseCard16 get() = parseUint16
    val parseOffset16 get() = parseUint16

    val parseInt16 get() = buffer.getInt16(offset + relativeOffset).also { relativeOffset += 2 }

    val parseF2Dot14 get() = (buffer.getInt16(offset + relativeOffset) / 16384).also { relativeOffset += 2 }

    val parseUint32 get() = buffer.getUint32(offset + relativeOffset).also { relativeOffset += 4 }
    val parseOffset32 get() = parseUint32

    val parseFloat32 get() = buffer.getFloat32(offset + relativeOffset).also { relativeOffset += 4 }

    fun parseString(length: Int): String {
        val offset = offset + relativeOffset
        var string = ""
        relativeOffset += length
        for (i in 0 until length) {
            string += buffer.getUint8(offset + i).toInt().toChar()
        }
        return string
    }

    val parseLongDateTime
        get() = buffer.getInt32(offset + relativeOffset + 4).run { this - 2082844800 }.also { relativeOffset += 8 }

    fun parseVersion(minorBase: Int = 0x1000): Float {
        val major = buffer.getUint16(offset + relativeOffset)
        val minor = buffer.getUint16(offset + relativeOffset + 2).also { relativeOffset += 4 }
        return major + minor / minorBase / 10f
    }

    fun skip(type: Type, amount: Int = 1) {
        relativeOffset += type.size * amount
    }

    fun parseUInt32List(count: Int? = null): IntArray {
        val total = count ?: parseUint32
        val offsets = IntArray(total)
        var offset = offset + relativeOffset
        for (i in 0 until total) {
            offsets[i] = buffer.getUint32(offset)
            offset += 4
        }

        relativeOffset += total * 4
        return offsets
    }

    fun parseUint16List(count: Int? = null): ShortArray {
        val total = count ?: parseUint32
        val offsets = ShortArray(total)
        var offset = offset + relativeOffset
        for (i in 0 until total) {
            offsets[i] = buffer.getUint16(offset)
            offset += 2
        }

        relativeOffset += total * 2
        return offsets
    }

    fun parseOffset16List(count: Int? = null) = parseUint16List(count)

    fun parseInt16List(count: Int? = null): ShortArray {
        val total = count ?: parseUint32
        val offsets = ShortArray(total)
        var offset = offset + relativeOffset
        for (i in 0 until total) {
            offsets[i] = buffer.getInt16(offset)
            offset += 2
        }

        relativeOffset += total * 2
        return offsets
    }

    fun parseByteList(count: Int): ByteArray {
        val list = ByteArray(count)
        var offset = offset + relativeOffset
        for (i in 0 until count) {
            list[i] = buffer.getUint8(offset++)
        }

        relativeOffset += count
        return list
    }

}