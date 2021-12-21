package com.lehaine.littlekt.file.font.ttf

import com.lehaine.littlekt.file.ByteBuffer

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

internal class Parser(private val buffer: ByteBuffer, offset: Int) {
    var offset = offset
        private set

    var relativeOffset = 0

    val parseUByte get() = buffer.getUByte(offset + relativeOffset++).toUByte().toInt()
    val parseByte get() = buffer.getUByte(offset + relativeOffset++)
    val parseChar get() = buffer.getByte(offset + relativeOffset++).toInt().toChar()
    val parseCard8 get() = parseUByte

    val parseUint16 get() = buffer.getUShort(offset + relativeOffset).also { relativeOffset += 2 }.toUShort().toInt()
    val parseCard16 get() = parseUint16
    val parseOffset16 get() = parseUint16

    val parseInt16 get() = buffer.getShort(offset + relativeOffset).also { relativeOffset += 2 }

    val parseF2Dot14 get() = (buffer.getShort(offset + relativeOffset) / 16384).also { relativeOffset += 2 }

    val parseUint32 get() = buffer.getUInt(offset + relativeOffset).also { relativeOffset += 4 }
    val parseOffset32 get() = parseUint32

    val parseFixed: Float
        get() {
            val decimal = buffer.getShort(offset)
            val fraction = buffer.getUShort(offset + 2)
            relativeOffset += 4
            return (decimal + fraction / 65535).toFloat()
        }

    fun parseString(length: Int): String {
        val offset = offset + relativeOffset
        var string = ""
        relativeOffset += length
        for (i in 0 until length) {
            string += buffer.getUByte(offset + i).toInt().toChar()
        }
        return string
    }

    val parseLongDateTime
        get() = buffer.getInt(offset + relativeOffset + 4).run { this - 2082844800 }.also { relativeOffset += 8 }

    fun parseVersion(minorBase: Int = 0x1000): Float {
        val major = buffer.getUShort(offset + relativeOffset)
        val minor = buffer.getUShort(offset + relativeOffset + 2).also { relativeOffset += 4 }
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
            offsets[i] = buffer.getUInt(offset)
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
            offsets[i] = buffer.getUShort(offset)
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
            offsets[i] = buffer.getShort(offset)
            offset += 2
        }

        relativeOffset += total * 2
        return offsets
    }

    fun parseByteList(count: Int): ByteArray {
        val list = ByteArray(count)
        var offset = offset + relativeOffset
        for (i in 0 until count) {
            list[i] = buffer.getUByte(offset++)
        }

        relativeOffset += count
        return list
    }

}