package com.lehaine.littlekt.file

import com.lehaine.littlekt.graphics.gl.TextureFormat
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
internal object ImageUtils {
    private val BufferedImage.format: TextureFormat
        get() {
            val alpha = transparency == Transparency.TRANSLUCENT || transparency == Transparency.BITMASK
            return if (alpha) TextureFormat.RGBA else TextureFormat.RGB
        }

    private fun chooseDstFormat(srcFormat: TextureFormat, preferredFormat: TextureFormat?): TextureFormat {
        return when {
            preferredFormat == null -> srcFormat
            preferredFormat.hasAlpha && !srcFormat.hasAlpha -> srcFormat
            else -> preferredFormat
        }
    }


    private fun BufferedImage.toBuffer(dstFormat: TextureFormat?): ByteBuffer {
        return bufferedImageToBuffer(this, dstFormat, 0, 0)
    }

    fun bufferedImageToBuffer(image: BufferedImage, dstFmt: TextureFormat?, width: Int, height: Int): ByteBuffer {
        val srcFormat = image.format
        val dstFormat = chooseDstFormat(srcFormat, dstFmt)

        val w = if (width == 0) {
            image.width
        } else {
            width
        }
        val h = if (height == 0) {
            image.height
        } else {
            height
        }
        val stride = when (srcFormat) {
            TextureFormat.RED -> 1
            TextureFormat.RG -> 2
            TextureFormat.RGB -> 3
            TextureFormat.RGBA -> 4
            else -> throw RuntimeException("Invalid output format $srcFormat")
        }

        val buffer = createByteBuffer(w * h * stride)

        var copied = false
        if (w == image.width && h == image.height) {
            // Images loaded via ImageIO usually are of type 4BYTE_ABGR or 3BYTE_BGR, we can load them in a optimized way...
            when {
                image.type == BufferedImage.TYPE_4BYTE_ABGR && dstFormat == TextureFormat.RGBA -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
                image.type == BufferedImage.TYPE_3BYTE_BGR && dstFormat == TextureFormat.RGB -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
                image.type == BufferedImage.TYPE_BYTE_GRAY && dstFormat == TextureFormat.RED -> {
                    copied = fastCopyImage(image, buffer, dstFormat)
                }
            }
        }

        if (!copied) {
            // fallback to slow copy
            slowCopyImage(image, buffer, dstFormat, w, h)
        }

        buffer.flip()
        return buffer
    }

    private fun fastCopyImage(image: BufferedImage, target: ByteBuffer, dstFormat: TextureFormat): Boolean {
        val imgBuf = image.data.dataBuffer as? DataBufferByte ?: return false
        val bytes = imgBuf.bankData[0]
        val nPixels = image.width * image.height * dstFormat.channels

        if (dstFormat == TextureFormat.RGBA && bytes.size == nPixels) {
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

        } else if (dstFormat == TextureFormat.RGB && bytes.size == nPixels) {
            for (i in 0 until nPixels step 3) {
                // swap byte order (bgr -> rgb)
                val b = bytes[i]
                bytes[i] = bytes[i + 2]
                bytes[i + 2] = b
            }
            target.putByte(bytes)
            return true

        } else if (dstFormat == TextureFormat.RED && bytes.size == nPixels) {
            target.putByte(bytes)
            return true
        }
        return false
    }

    private fun slowCopyImage(
        image: BufferedImage,
        target: ByteBuffer,
        dstFormat: TextureFormat,
        width: Int,
        height: Int
    ) {
        val pixel = IntArray(4)
        val model = image.colorModel
        val sizes = IntArray(4) { i -> (1 shl model.componentSize[i % model.componentSize.size]) - 1 }
        val raster = image.data
        val alpha = image.transparency == Transparency.TRANSLUCENT || image.transparency == Transparency.BITMASK
        val indexed = image.type == BufferedImage.TYPE_BYTE_BINARY || image.type == BufferedImage.TYPE_BYTE_INDEXED

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
                    TextureFormat.RED -> {
                        target.putByte((r * 255f).toInt().toByte())
                    }
                    TextureFormat.RG -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                    }
                    TextureFormat.RGB -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                        target.putByte((b * 255f).toInt().toByte())
                    }
                    TextureFormat.RGBA -> {
                        target.putByte((r * 255f).toInt().toByte())
                        target.putByte((g * 255f).toInt().toByte())
                        target.putByte((b * 255f).toInt().toByte())
                        target.putByte((a * 255f).toInt().toByte())
                    }
                    else -> throw IllegalArgumentException("TexFormat not yet implemented: $dstFormat")
                }
            }
        }
    }
}