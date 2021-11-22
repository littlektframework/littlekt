package com.lehaine.littlekt.graphics

import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.Image
import org.w3c.dom.ImageData

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
class WebPixmap(
    width: Int, height: Int,
    val context: CanvasRenderingContext2D,
    val image: Image
) : Pixmap(width, height, byteArrayOf()) {

    private val clearColor = Color.WHITE.rgbaString
    private var imagePixels: Uint8ClampedArray? = null

    private val tmpColor = Color()

    override fun draw(
        pixmap: Pixmap,
        x: Int,
        y: Int,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        filtering: Boolean,
        blending: Boolean
    ) {
        if (pixmap is WebPixmap) {
            context.run {
                fillStyle = clearColor
                strokeStyle = clearColor
                globalCompositeOperation = "destination-out"
                beginPath()
                rect(x.toDouble(), y.toDouble(), dstWidth.toDouble(), dstHeight.toDouble())
                fillOrStroke(DrawType.FILL)
                closePath()
                globalCompositeOperation = "source-over"
            }
            if (srcWidth != 0 && srcHeight != 0 && dstWidth != 0 && dstHeight != 0) {
                context.drawImage(
                    pixmap.image,
                    srcX.toDouble(),
                    srcY.toDouble(),
                    srcWidth.toDouble(),
                    srcHeight.toDouble(),
                    x.toDouble(),
                    y.toDouble(),
                    dstWidth.toDouble(),
                    dstHeight.toDouble()
                )
            }
        } else {
            val imgData = ImageData(Uint8ClampedArray(pixmap.pixels.toTypedArray()), pixmap.width, pixmap.height)
            context.putImageData(imgData, 0.0, 0.0)
        }
        imagePixels = null
    }


    override fun fill(color: Color) {
        context.clearRect(0.0, 0.0, width.toDouble(), height.toDouble())
        rectangle(0, 0, width, height, DrawType.FILL, color)
    }

    override fun set(x: Int, y: Int, color: Int, force: Boolean) {
        if (contains(x, y) || force) {
            tmpColor.setRgba8888(color)
            rectangle(x, y, 1, 1, DrawType.FILL, tmpColor)
        }
    }

    override fun get(x: Int, y: Int, force: Boolean): Int {
        if (imagePixels == null) {
            imagePixels = context.getImageData(0.0, 0.0, width.toDouble(), height.toDouble()).data
        }
        return imagePixels?.let {
            val i = x * 4 + y * width * 4
            val r = it[i + 0].toInt() and 0xff
            val g = it[i + 1].toInt() and 0xff
            val b = it[i + 2].toInt() and 0xff
            val a = it[i + 3].toInt() and 0xff
            return r shl 24 or (g shl 16) or (b shl 8) or a
        } ?: 0

    }

    private fun rectangle(x: Int, y: Int, width: Int, height: Int, drawType: DrawType, color: Color) {
        val rgbaString = color.rgbaString
        // if no blending
        context.run {
            fillStyle = clearColor
            strokeStyle = clearColor
            globalCompositeOperation = "destination-out"
            beginPath()
            rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
            fillOrStroke(drawType)
            closePath()
            fillStyle = rgbaString
            strokeStyle = rgbaString
            globalCompositeOperation = "source-over"
        }
        // end if no blending
        context.run {
            beginPath()
            rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
            fillOrStroke(drawType)
            context.closePath()
        }
        imagePixels = null
    }

    private enum class DrawType {
        FILL, STROKE
    }

    private fun CanvasRenderingContext2D.fillOrStroke(drawType: DrawType) =
        if (drawType == DrawType.FILL) fill() else stroke()

    private val Color.rgbaString
        get() = "rgba${(r * 255).toInt()}, ${(g * 255).toInt()}, ${(b * 255).toInt()}, ${a.toDouble()}"
}
