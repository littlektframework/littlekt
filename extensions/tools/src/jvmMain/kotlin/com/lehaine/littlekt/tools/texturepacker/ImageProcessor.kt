package com.lehaine.littlekt.tools.texturepacker

import com.lehaine.littlekt.util.packer.BinRect
import com.lehaine.littlekt.util.packer.PackingOptions
import java.awt.image.BufferedImage
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
class ImageProcessor(val config: TexturePackerConfig) {

    private val _data = mutableListOf<ImageRectData>()
    val data: List<ImageRectData> get() = _data

    private val indexPattern = "(.+)_?(\\d+)$".toRegex().toPattern()

    private val crcs = mutableMapOf<String, ImageRectData>()

    fun addImage(file: File): ImageRectData? {
        val image = ImageIO.read(file) ?: error("Unable to read image: $file")
        return addImage(image, file.nameWithoutExtension)?.also { it.unloadImage(file) }
    }

    fun addImage(image: BufferedImage, name: String): ImageRectData? {
        val imageRectData = processImage(image, name) ?: return null
        val crc = hash(imageRectData.loadImage())
        val existing = crcs[crc]
        if (existing != null) {
            existing.aliases += ImageAlias(imageRectData, imageRectData.name, imageRectData.index)
            return null
        }
        crcs[crc] = imageRectData
        _data += imageRectData
        return imageRectData
    }

    fun processImage(input: BufferedImage, inputName: String): ImageRectData? {
        var image = input
        val width = input.width
        val height = input.height

        if (image.type != BufferedImage.TYPE_INT_ARGB) {
            image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                graphics.drawImage(image, 0, 0, null)
            }
        }

        val matcher = indexPattern.matcher(inputName)
        var index = -1
        if (matcher.matches()) {
            index = matcher.group(2).toInt()
        }

        val extrude = config.packingOptions.extrude
        check(extrude >= 0) { "Extrude must be >= 0!" }

        if (extrude > 0) {
            image = extrude(image)
        }

        return if (config.trim) {
            trim(image, inputName).also { it?.index = index }
        } else {
            ImageRectData(
                0,
                0,
                image.width,
                image.height,
                regionWidth = image.width - extrude * 2,
                regionHeight = image.height - extrude * 2,
                offsetX = 0,
                offsetY = 0,
                originalWidth = image.width - extrude * 2,
                originalHeight = image.height - extrude * 2,
                image = image,
                name = inputName,
                index = index,
                extrude = extrude
            )
        }
    }

    private fun trim(image: BufferedImage, name: String): ImageRectData? {
        val extrude = config.packingOptions.extrude

        val alphaRaster = image.alphaRaster ?: return ImageRectData(
            0,
            0,
            image.width,
            image.height,
            regionWidth = image.width - extrude * 2,
            regionHeight = image.height - extrude * 2,
            offsetX = 0,
            offsetY = 0,
            originalWidth = image.width - extrude * 2,
            originalHeight = image.height - extrude * 2,
            image = image,
            name = name,
            extrude = extrude
        )

        val a = IntArray(1)
        var left = 0
        var top = 0
        var right = image.width
        var bottom = image.height

        run top@{
            for (y in extrude until image.height - extrude) {
                for (x in extrude until image.width - extrude) {
                    alphaRaster.getPixel(x, y, a)
                    var alpha = a[0]
                    if (alpha < 0) alpha += 256
                    if (alpha > 0) return@top
                }
                top++
            }
        }

        run bottom@{
            for (y in image.height - 1 - extrude downTo top) {
                for (x in extrude until image.width - extrude) {
                    alphaRaster.getPixel(x, y, a)
                    var alpha = a[0]
                    if (alpha < 0) alpha += 256
                    if (alpha > 0) return@bottom
                }
                bottom--
            }
        }

        run left@{
            for (x in extrude until image.width - extrude) {
                for (y in top until bottom) {
                    alphaRaster.getPixel(x, y, a)
                    var alpha = a[0]
                    if (alpha < 0) alpha += 256
                    if (alpha > 0) return@left
                }
                left++
            }
        }
        run right@{
            for (x in image.width - 1 - extrude downTo left) {
                for (y in top until bottom) {
                    alphaRaster.getPixel(x, y, a)
                    var alpha = a[0]
                    if (alpha < 0) alpha += 256
                    if (alpha > 0) return@right
                }
                right--
            }
        }

        for (i in 0 until config.trimMargin) {
            if (left > extrude) left--
            if (right < image.width - extrude) right++
            if (top > extrude) top--
            if (bottom < image.height - extrude) bottom++
        }

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            return null
        }

        return ImageRectData(
            0,
            0,
            width,
            height,
            regionWidth = width - extrude * 2,
            regionHeight = height - extrude * 2,
            offsetX = left,
            offsetY = top,
            originalWidth = if (config.crop == TexturePackerConfig.CropType.NONE) image.width - extrude * 2 else width - extrude * 2,
            originalHeight = if (config.crop == TexturePackerConfig.CropType.NONE) image.height - extrude * 2 else height - extrude * 2,
            image = image,
            name = name,
            extrude = extrude
        )
    }

    private fun extrude(image: BufferedImage): BufferedImage {
        val extrude = config.packingOptions.extrude
        val out = BufferedImage(
            image.width + extrude * 2,
            image.height + extrude * 2,
            image.type
        ).apply {
            graphics.drawImage(image, extrude, extrude, null)
        }

        return out
    }

    private fun hash(input: BufferedImage): String {
        val digest = MessageDigest.getInstance("SHA1")

        var image = input
        val width = image.width
        val height = image.height

        if (image.type != BufferedImage.TYPE_INT_ARGB) {
            image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                graphics.drawImage(image, 0, 0, null)
            }
        }

        val raster = image.raster
        val pixels = IntArray(width)
        for (y in 0 until height) {
            raster.getDataElements(0, y, width, 1, pixels)
            for (x in 0 until width) {
                hash(digest, pixels[x])
            }
        }
        hash(digest, width)
        hash(digest, height)
        return BigInteger(1, digest.digest()).toString(16)
    }

    private fun hash(digest: MessageDigest, value: Int) {
        digest.update((value shr 24).toByte())
        digest.update((value shr 16).toByte())
        digest.update((value shr 8).toByte())
        digest.update(value.toByte())
    }
}

class ImageRectData(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    var offsetX: Int = 0,
    var offsetY: Int = 0,
    var regionWidth: Int = 0,
    var regionHeight: Int = 0,
    var originalWidth: Int = 0,
    var originalHeight: Int = 0,
    var file: File? = null,
    var image: BufferedImage? = null,
    var name: String = "",
    var index: Int = 0,
    val aliases: MutableList<ImageAlias> = mutableListOf(),
    var extrude: Int = 0,
) : BinRect(x, y, width, height) {

    fun unloadImage(file: File) {
        this.file = file
        image = null
    }

    fun loadImage(): BufferedImage {
        image?.let { return it }

        return ImageIO.read(file) ?: error("Unable to read image: $file")
    }

    override fun toString(): String {
        return "ImageRectData(offsetX=$offsetX, offsetY=$offsetY, regionWidth=$regionWidth, regionHeight=$regionHeight, originalWidth=$originalWidth, originalHeight=$originalHeight, file=$file, image=$image, name='$name', index=$index, aliases=$aliases)"
    }
}

data class ImageAlias(val rect: ImageRectData, var name: String = "", var index: Int = 0)