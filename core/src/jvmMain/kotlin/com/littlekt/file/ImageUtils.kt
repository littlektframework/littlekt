package com.littlekt.file

import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
internal object ImageUtils {
    private val BufferedImage.format: String
        get() {
            val alpha =
                transparency == Transparency.TRANSLUCENT || transparency == Transparency.BITMASK
            return if (alpha) "RGBA" else "RGB"
        }

    private val String.channels: Int
        get() =
            when (this) {
                "RED " -> 1
                "RG" -> 2
                "RGB" -> 3
                "RGBA" -> 4
                else -> 0
            }

    private fun chooseDstFormat(srcFormat: String, preferredFormat: String?): String {
        return when {
            preferredFormat == null -> srcFormat
            preferredFormat == "RGBA" && srcFormat != "RGBA" -> srcFormat
            else -> preferredFormat
        }
    }

    fun bufferedImageToBuffer(
        image: BufferedImage,
        dstFmt: String?,
        width: Int,
        height: Int
    ): ImageLoadResult {
        val srcFormat = image.format
        val dstFormat = chooseDstFormat(srcFormat, dstFmt)
        val w =
            if (width == 0) {
                image.width
            } else {
                width
            }
        val h =
            if (height == 0) {
                image.height
            } else {
                height
            }
        val stride = srcFormat.channels

        check(stride > 0) { "Invalid output format: $srcFormat" }

        val buffer = com.littlekt.file.ByteBuffer(w * h * stride)

        var copied = false
        if (w == image.width && h == image.height) {
            // Images loaded via ImageIO usually are of type 4BYTE_ABGR or 3BYTE_BGR, we can load
            // them in a optimized way...
            when {
                image.type == BufferedImage.TYPE_4BYTE_ABGR && dstFormat == "RGBA" -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
                image.type == BufferedImage.TYPE_3BYTE_BGR && dstFormat == "RGB" -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
                image.type == BufferedImage.TYPE_BYTE_GRAY && dstFormat == "RED" -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
            }
        }

        if (!copied) {
            // fallback to slow copy
            slowCopyImage(image, buffer, dstFormat, w, h)
        }

        buffer.flip()
        return ImageLoadResult(width, height, buffer, dstFormat)
    }

    private fun fastCopyImage(
        image: BufferedImage,
        target: ByteBuffer,
        dstFormat: String
    ): Boolean {
        val imgBuf = image.data.dataBuffer as? DataBufferByte ?: return false
        val bytes = imgBuf.bankData[0]
        val nPixels = image.width * image.height * dstFormat.channels

        if (dstFormat == "RGBA" && bytes.size == nPixels) {
            for (i in 0 until nPixels step 4) {
                // swap byte order (abgr -> rgba)
                val a = bytes[i]
                val b = bytes[i + 1]
                bytes[i] = bytes[i + 3]
                bytes[i + 1] = bytes[i + 2]
                bytes[i + 2] = b
                bytes[i + 3] = a
            }
            target.putByte(bytes)
            return true
        } else if (dstFormat == "RGB" && bytes.size == nPixels) {
            for (i in 0 until nPixels step 3) {
                // swap byte order (bgr -> rgb)
                val b = bytes[i]
                bytes[i] = bytes[i + 2]
                bytes[i + 2] = b
            }
            target.putByte(bytes)
            return true
        } else if (dstFormat == "RED" && bytes.size == nPixels) {
            target.putByte(bytes)
            return true
        }
        return false
    }

    private fun slowCopyImage(
        image: BufferedImage,
        target: ByteBuffer,
        dstFormat: String,
        width: Int,
        height: Int,
    ) {
        val pixel = IntArray(4)
        val model = image.colorModel
        val sizes =
            IntArray(4) { i -> (1 shl model.componentSize[i % model.componentSize.size]) - 1 }
        val raster = image.data
        val alpha =
            image.transparency == Transparency.TRANSLUCENT ||
                image.transparency == Transparency.BITMASK
        val indexed =
            image.type == BufferedImage.TYPE_BYTE_BINARY ||
                image.type == BufferedImage.TYPE_BYTE_INDEXED

        for (y in 0 until height) {
            for (x in 0 until width) {
                raster.getPixel(x, y, pixel)

                if (indexed) {
                    val p = pixel[0]
                    pixel[0] = model.getRed(p)
                    pixel[1] = model.getGreen(p)
                    pixel[2] = model.getBlue(p)
                    pixel[3] = model.getAlpha(p)
                }
                if (!alpha) {
                    pixel[3] = 255
                }

                val r = pixel[0] / sizes[0].toFloat()
                val g = pixel[1] / sizes[1].toFloat()
                val b = pixel[2] / sizes[2].toFloat()
                val a = pixel[3] / sizes[3].toFloat()

                // copy bytes to target buf
                when (dstFormat) {
                    "RED" -> {
                        target.putByte((r * 255f).toInt().toByte())
                    }
                    "RG" -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                    }
                    "RGB" -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                        target.putByte((b * 255f).toInt().toByte())
                    }
                    "RGBA" -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                        target.putByte((b * 255f).toInt().toByte())
                        target.putByte((a * 255f).toInt().toByte())
                    }
                    else ->
                        throw IllegalArgumentException("TexFormat not yet implemented: $dstFormat")
                }
            }
        }
    }

    internal data class ImageLoadResult(
        val width: Int,
        val height: Int,
        val buffer: ByteBuffer,
        val format: String
    )
}
