package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.GL

/**
 * @author Colton Daily
 * @date 11/18/2021
 */
class Pixmap(val width: Int, val height: Int, val pixels: ByteArray) {

    enum class Format(val glType: Int, val glFormat: Int) {
        ALPHA(GL.UNSIGNED_BYTE, GL.ALPHA),
        INTENSITY(GL.UNSIGNED_BYTE, GL.ALPHA),
        LUMINANCE_ALPHA(GL.UNSIGNED_BYTE, GL.LUMINANCE_ALPHA),
        RGB565(GL.UNSIGNED_SHORT_5_6_5, GL.RGB),
        RGBA4444(GL.UNSIGNED_SHORT_4_4_4_4, GL.RGBA),
        RGB8888(GL.UNSIGNED_BYTE, GL.RGB),
        RGBA8888(GL.UNSIGNED_BYTE, GL.RGBA);

        companion object {
            val ALL = values()
        }
    }

    val glFormat = GL.RGBA
    val glType = GL.UNSIGNED_BYTE

    fun fill(color: Int) {
        val length = width * height * 4
        for (i in 0 until length) {
            pixels[i] = color.toByte()
        }
    }

    fun fill(color: Color) = fill(color.rgba())

    fun drawLine(x: Int, y: Int, x2: Int, y2: Int, color: Int) {
        var tx = x
        var ty = y
        var dy = y - y2
        var dx = x - x2
        var fraction: Int
        val stepX: Int
        val stepY: Int

        if (dy < 0) {
            dy = -dy
            stepY = -1
        } else {
            stepY = 1
        }
        if (dx < 0) {
            dx = -dx
            stepX = -1
        } else {
            stepX = 1
        }
        dy = dy shl 1
        dx = dx shl 1

        set(x, y, color)
        if (dx > dy) {
            fraction = dy - (dx shr 1)
            while (tx != x2) {
                if (fraction >= 0) {
                    ty += stepY
                    fraction -= dx
                }
                tx += stepX
                fraction += dy
                set(tx, ty, color)
            }
        } else {
            fraction = dx - (dy shr 1)
            while (ty != y2) {
                if (fraction >= 0) {
                    tx += stepX
                    fraction -= dy
                }
                ty += stepY
                fraction += dx
                set(tx, ty, color)
            }
        }
    }


    fun hline(x1: Int, x2: Int, y: Int, color: Int) {
        if (y < 0 || y >= height) return
        var tx1 = x1
        var tx2 = x2
        if (tx1 > tx2) {
            tx1 = x2
            tx2 = x1
        }
        if (tx1 >= width) return
        if (tx2 < 0) return

        if (tx1 < 0) {
            tx1 = 0
        }
        if (tx2 >= width) {
            tx2 = width - 1
        }
        tx2++
        while (tx1 != tx2) {
            set(tx1++, y, color, true)
        }

    }

    fun vline(y1: Int, y2: Int, x: Int, color: Int) {
        if (x < 0 || x >= width) return
        var ty1 = y1
        var ty2 = y2
        if (ty1 > ty2) {
            ty1 = y2
            ty2 = y1
        }
        if (ty1 >= height) return
        if (ty2 < 0) return

        if (ty1 < 0) {
            ty1 = 0
        }
        if (ty2 >= height) {
            ty2 = height - 1
        }
        ty2++
        while (ty1 != ty2) {
            set(x, ty1++, color, true)
        }
    }

    fun set(x: Int, y: Int, color: Int, force: Boolean = false) {
        if (force || contains(x, y)) {
            pixels[(x + y * width) * 4] = color.toByte()
        }
    }

    fun contains(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x < width && y < width
    }
}