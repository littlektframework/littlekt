package com.littlekt.file.hdr

/**
 * A high dynamic range image with linear `RGB` float pixels, such as one loaded from a Radiance
 * `.hdr` file via [decodeRadianceHdr].
 *
 * @param width the width of the image in pixels.
 * @param height the height of the image in pixels.
 * @param data the `RGB` pixels, row by row starting from the top. Size of `width * height * 3`.
 * @author Colton Daily
 * @date 11/8/2026
 */
class HdrImage(val width: Int, val height: Int, val data: FloatArray) {

    init {
        require(data.size == width * height * 3) {
            "HdrImage.data must have a size of width * height * 3!"
        }
    }

    /**
     * @return the pixels as `RGBA` half floats, in little endian, ready to be written to a
     *   [com.littlekt.graphics.webgpu.TextureFormat.RGBA16_FLOAT] texture.
     */
    fun toRgba16Float(): ByteArray {
        val bytes = ByteArray(width * height * 8)
        val one = floatToHalf(1f)
        for (i in 0 until width * height) {
            val dst = i * 8
            for (c in 0 until 3) {
                val half = floatToHalf(data[i * 3 + c])
                bytes[dst + c * 2] = half.toByte()
                bytes[dst + c * 2 + 1] = (half shr 8).toByte()
            }
            bytes[dst + 6] = one.toByte()
            bytes[dst + 7] = (one shr 8).toByte()
        }
        return bytes
    }

    companion object {
        private const val MAX_HALF = 65504f

        /** @return the IEEE 754 half float bits of [value], clamped to the max half value. */
        fun floatToHalf(value: Float): Int {
            val clamped = if (value.isNaN()) 0f else value.coerceIn(-MAX_HALF, MAX_HALF)
            val bits = clamped.toRawBits()
            val sign = (bits ushr 16) and 0x8000
            val exponent = ((bits ushr 23) and 0xff) - 127 + 15
            var mantissa = bits and 0x7fffff
            if (exponent <= 0) {
                if (exponent < -10) return sign
                mantissa = mantissa or 0x800000
                val shift = 14 - exponent
                val half = mantissa ushr shift
                val round = (mantissa ushr (shift - 1)) and 1
                return sign or (half + round)
            }
            val half = sign or (exponent shl 10) or (mantissa ushr 13)
            return half + ((mantissa ushr 12) and 1)
        }
    }
}

/**
 * Decodes a Radiance `.hdr` (`RGBE`) file. Supports flat & run length encoded scanlines.
 *
 * @return the decoded [HdrImage].
 */
fun decodeRadianceHdr(bytes: ByteArray): HdrImage {
    var pos = 0

    fun readLine(): String {
        val start = pos
        while (pos < bytes.size && bytes[pos] != '\n'.code.toByte()) pos++
        val line = bytes.decodeToString(start, pos)
        pos++
        return line.trimEnd('\r')
    }

    val magic = readLine()
    require(magic.startsWith("#?")) { "Not a Radiance HDR file!" }
    while (true) {
        val line = readLine()
        if (line.isEmpty()) break
        if (line.startsWith("FORMAT=")) {
            require(line == "FORMAT=32-bit_rle_rgbe") { "Unsupported HDR format: $line" }
        }
        require(pos < bytes.size) { "Unexpected end of HDR header!" }
    }
    val resolution = readLine().split(' ').filter { it.isNotEmpty() }
    require(resolution.size == 4 && resolution[2] == "+X") {
        "Unsupported HDR resolution: ${resolution.joinToString(" ")}"
    }
    val flipY =
        when (resolution[0]) {
            "-Y" -> false
            "+Y" -> true
            else -> error("Unsupported HDR resolution: ${resolution.joinToString(" ")}")
        }
    val height = resolution[1].toInt()
    val width = resolution[3].toInt()

    val exponents = FloatArray(256) { if (it == 0) 0f else pow2(it - 136) }
    val data = FloatArray(width * height * 3)
    val scanline = ByteArray(width * 4)

    fun byte(): Int = bytes[pos++].toInt() and 0xff

    for (y in 0 until height) {
        val rle =
            width in 8..0x7fff &&
                pos + 4 <= bytes.size &&
                bytes[pos].toInt() == 2 &&
                bytes[pos + 1].toInt() == 2 &&
                (bytes[pos + 2].toInt() and 0x80) == 0
        if (rle) {
            pos += 4
            for (channel in 0 until 4) {
                var x = 0
                while (x < width) {
                    var count = byte()
                    if (count > 128) {
                        count -= 128
                        val value = bytes[pos++]
                        repeat(count) { scanline[(x++) * 4 + channel] = value }
                    } else {
                        repeat(count) { scanline[(x++) * 4 + channel] = bytes[pos++] }
                    }
                }
            }
        } else {
            bytes.copyInto(scanline, 0, pos, pos + width * 4)
            pos += width * 4
        }
        val row = if (flipY) height - 1 - y else y
        for (x in 0 until width) {
            val e = exponents[scanline[x * 4 + 3].toInt() and 0xff]
            val dst = (row * width + x) * 3
            data[dst] = (scanline[x * 4].toInt() and 0xff) * e
            data[dst + 1] = (scanline[x * 4 + 1].toInt() and 0xff) * e
            data[dst + 2] = (scanline[x * 4 + 2].toInt() and 0xff) * e
        }
    }
    return HdrImage(width, height, data)
}

private fun pow2(exponent: Int): Float {
    var result = 1f
    if (exponent >= 0) repeat(exponent) { result *= 2f } else repeat(-exponent) { result *= 0.5f }
    return result
}
