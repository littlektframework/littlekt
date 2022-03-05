package com.lehaine.littlekt.tools.texturepacker

import com.lehaine.littlekt.tools.FileNameComparator
import com.lehaine.littlekt.tools.texturepacker.template.createAtlasPage
import com.lehaine.littlekt.util.packer.Bin
import com.lehaine.littlekt.util.packer.MaxRectsPacker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
class TexturePacker(val config: TexturePackerConfig) {

    private val extensions = listOf("png", "jpg", "jpeg")
    private var files: Sequence<File> = sequenceOf()
    private val imageProcessor = ImageProcessor(config)
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun process() {
        files = File(config.inputDir).walkTopDown()
            .onEnter {
                !config.ignoreDirs.contains(it.invariantSeparatorsPath)
            }
            .filter {
                extensions.contains(it.extension) && !config.ignoreFiles.contains(
                    it.invariantSeparatorsPath
                )
            }
            .sortedWith(FileNameComparator())
    }

    fun pack() {
        val outputDir = File(config.outputDir)
        files.forEach { imageProcessor.addImage(it) }
        val packer = MaxRectsPacker(config.packingOptions)
        packer.add(imageProcessor.data)

        writeImages(outputDir, packer.bins)
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeImages(outputDir: File, bins: List<Bin>) {
        outputDir.mkdirs()

        bins.forEachIndexed { index, bin ->
            var canvas = BufferedImage(bin.width, bin.height, BufferedImage.TYPE_INT_ARGB)
            bin.rects.forEach { rect ->
                rect as ImageRectData
                val image = rect.loadImage()
                image.copyTo(
                    rect.offsetX,
                    rect.offsetY,
                    rect.regionWidth,
                    rect.regionHeight,
                    canvas,
                    rect.x,
                    rect.y,
                    rect.isRotated
                )
            }
            val imageName = if (bins.size > 1) "${config.outputName}-$index.png" else "${config.outputName}.png"
            val jsonName = if (bins.size > 1) "${config.outputName}-$index.json" else "${config.outputName}.json"
            val relatedMultiPacks = if (bins.size > 1) {
                val relatedMultiPacks = mutableListOf<String>()
                var i = 0
                while (i < index) {
                    relatedMultiPacks += "${config.outputName}-$i.json"
                    i++
                }
                i = index
                while (i < bins.lastIndex) {
                    i++
                    relatedMultiPacks += "${config.outputName}-$i.json"
                }
                relatedMultiPacks
            } else {
                listOf()
            }
            if (config.packingOptions.bleed) {
                canvas = ColorBleedEffect().processImage(canvas, config.packingOptions.bleedIterations)
            }
            val page = createAtlasPage(canvas, imageName, bin.rects as List<ImageRectData>, relatedMultiPacks)

            ImageIO.write(canvas, "png", File(outputDir, imageName))
            val json = json.encodeToString(page)
            val jsonFile = File(outputDir, jsonName).also { it.createNewFile() }
            FileOutputStream(jsonFile).use {
                it.write(json.toByteArray())
            }
        }
    }

    private fun BufferedImage.copyTo(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        dst: BufferedImage,
        dx: Int,
        dy: Int,
        rotated: Boolean
    ) {
        if (rotated) {
            for (i in 0 until width) {
                for (j in 0 until height) {
                    dst.plot(dx + j, dy + width - i - 1, getRGB(x + i, y + j))
                }
            }
        } else {
            for (i in 0 until width) {
                for (j in 0 until height) {
                    dst.plot(dx + i, dy + j, getRGB(x + i, y + j))
                }
            }
        }
    }

    private fun BufferedImage.plot(x: Int, y: Int, argb: Int) {
        if (x in 0 until width && y in 0 until height) {
            setRGB(x, y, argb)
        }
    }
}

