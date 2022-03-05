package com.lehaine.littlekt.tools.texturepacker

import java.awt.image.BufferedImage

/**
 * @author Colton Daily
 * @date 3/5/2022
 */
class ColorBleedEffect {
    private val offsets = intArrayOf(-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1)

    fun processImage(image: BufferedImage, maxIterations: Int): BufferedImage {
        val width = image.width
        val height = image
            .height
        val processedImage: BufferedImage = if (image.type == BufferedImage.TYPE_INT_ARGB) {
            image
        } else {
            BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        }

        val rgb = image.getRGB(0, 0, width, height, null, 0, width)
        val mask = Mask(rgb)
        var iterations = 0
        var lastPending = -1
        while (mask.pendingSize > 0 && mask.pendingSize != lastPending && iterations < maxIterations) {
            lastPending = mask.pendingSize
            executeIteration(rgb, mask, width, height)
            iterations++
        }

        processedImage.setRGB(0, 0, width, height, rgb, 0, width)
        return processedImage
    }

    private fun executeIteration(rgb: IntArray, mask: Mask, width: Int, height: Int) {
        val iterator = mask.MaskIterator()
        while (iterator.hasNext()) {
            val pixelIndex = iterator.next()
            val x = pixelIndex % width
            val y = pixelIndex / width
            var r = 0
            var g = 0
            var b = 0
            var count = 0

            for (i in offsets.indices step 2) {
                var column = x + offsets[i]
                var row = y + offsets[i + 1]
                if (column < 0 || column >= width || row < 0 || row >= height) {
                    column = x
                    row = y
                    continue
                }
                val currentPixelIndex = getPixelIndex(width, column, row)
                if (!mask.isBlank(currentPixelIndex)) {
                    val argb = rgb[currentPixelIndex]
                    r += red(argb)
                    g += green(argb)
                    b += blue(argb)
                    count++
                }
            }
            if (count != 0) {
                rgb[pixelIndex] = argb(0, r / count, g / count, b / count)
                iterator.markAsInProgress()
            }
        }
        iterator.reset()
    }

    private fun getPixelIndex(width: Int, x: Int, y: Int) = y * width + x
    private fun red(argb: Int) = (argb shr 16) and 0xff
    private fun green(argb: Int) = (argb shr 8) and 0xff
    private fun blue(argb: Int) = (argb shr 0) and 0xff
    private fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        check(a in 0..255 && r in 0..255 && g in 0..255 && b in 0..255) {
            "Invalid RGBA: $r,$g,$b,$a"
        }

        return ((a and 0xff) shl 24) or ((r and 0xff) shl 16) or ((g and 0xff) shl 8) or ((b and 0xff) shl 0)
    }

    private class Mask(val rgb: IntArray) {
        val blank = BooleanArray(rgb.size)
        val pending = IntArray(rgb.size)
        val changing = IntArray(rgb.size)
        var pendingSize: Int = 0
        var changingSize: Int = 0

        init {
            rgb.forEachIndexed { index, pixel ->
                if (alpha(pixel) == 0) {
                    blank[index] = true
                    pending[pendingSize] = index
                    pendingSize++
                }
            }
        }

        fun isBlank(index: Int) = blank[index]

        fun removeIndex(index: Int): Int {
            check(index < pendingSize) { "IndexOutOfBounds: $index" }
            val value = pending[index]
            pendingSize--
            pending[index] = pending[pendingSize]
            return value
        }

        inner class MaskIterator {
            private var index: Int = 0

            fun hasNext() = index < pendingSize

            fun next(): Int {
                check(index < pendingSize) { "NoSuchElement: $index" }
                return pending[index++]
            }

            fun markAsInProgress() {
                index--
                changing[changingSize] = removeIndex(index)
                changingSize++
            }

            fun reset() {
                index = 0
                changing.forEach {
                    blank[it] = false
                }
                changingSize = 0
            }
        }

        private fun alpha(argb: Int) = (argb shr 24) and 0xff
    }
}