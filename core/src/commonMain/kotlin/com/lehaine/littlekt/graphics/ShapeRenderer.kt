package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.*

/**
 * Renders primitive shapes using an existing [Batch].
 * @param batch the batch used to batch draw calls with
 * @param slice a 1x1 slice of a texture. Generally a single white pixel.
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRenderer(val batch: Batch, val slice: TextureSlice = Textures.white) {
    var snap = false

    private val vertices = FloatArrayList(100)

    private val invTexWidth = 1f / slice.texture.width
    private val invTexHeight = 1f / slice.texture.height
    private val u = slice.x * invTexWidth
    private val v = slice.y * invTexHeight
    private val u2 = (slice.x + slice.width) * invTexWidth
    private val v2 = (slice.y + slice.height) * invTexHeight

    /**
     * Draws a line from point A to point B.
     * @param x starting x-coord
     * @param y starting y-coord
     * @param x2 ending x-coord
     * @param y2 ending y-coord
     * @param color color of the line
     * @param thickness the thickness of the line in pixels
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        x: Float, y: Float, x2: Float, y2: Float, color: Color = Color.WHITE, thickness: Int = 2,
        snap: Boolean = this.snap,
    ) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        batchLine(x, y, x2, y2, colorPacked, thickness, snap)

        batch.draw(slice.texture, vertices.data, 0, 5 * 4)
    }

    private fun snapPixel(a: Float, pixelSize: Float, halfPixelSize: Float) =
        (round(a / pixelSize) * pixelSize) + halfPixelSize

    private fun batchLine(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        colorPacked: Float,
        thickness: Int = 2,
        snap: Boolean = this.snap,
    ) {
        val dx = x2 - x
        val dy = y2 - y

        val offset = 0.001f
        val pixelSize = 1f
        val halfPixelSize = pixelSize * 0.5f

        @Suppress("NAME_SHADOWING")
        val x = if (snap) snapPixel(x, pixelSize, halfPixelSize) - sign(dx) * offset else x

        @Suppress("NAME_SHADOWING")
        val y = if (snap) snapPixel(y, pixelSize, halfPixelSize) - sign(dy) * offset else y

        @Suppress("NAME_SHADOWING")
        val x2 = if (snap) snapPixel(x2, pixelSize, halfPixelSize) - sign(dx) * offset else x2

        @Suppress("NAME_SHADOWING")
        val y2 = if (snap) snapPixel(y2, pixelSize, halfPixelSize) - sign(dy) * offset else y2

        var px = thickness * 0.5f
        var py = thickness * 0.5f

        if (x != x2 && y != y2) {
            val scale = 1f / sqrt(dx * dx + dy * dy) * thickness * 0.5f

            px = -dy * scale
            py = dx * scale
        }

        // bottom left
        vertices += x - px
        vertices += y + py
        vertices += colorPacked
        vertices += u
        vertices += if (slice.rotated) v2 else v

        // top left
        vertices += x + px
        vertices += y - py
        vertices += colorPacked
        vertices += if (slice.rotated) u2 else u
        vertices += v2

        // top right
        vertices += x2 + px
        vertices += y2 - py
        vertices += colorPacked
        vertices += u2
        vertices += if (slice.rotated) v else v2

        // bottom right
        vertices += x2 - px
        vertices += y2 + py
        vertices += colorPacked
        vertices += if (slice.rotated) u else u2
        vertices += v
    }

    /**
     * Draws a rectangle outline with the given dimensions.
     * @param x the x-coord of the rectangle
     * @param y the y-coord of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param color the color of the outline
     * @param thickness the thickness of the outline in pixels
     */
    fun rect(x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE, thickness: Int = 2) {
        val x2 = x + width
        val y2 = y + height
        val colorPacked = color.toFloatBits()
        vertices.clear()

        batchLine(x, y, x, y2, colorPacked, thickness)
        batchLine(x, y2, x2, y2, colorPacked, thickness)
        batchLine(x2, y2, x2, y, colorPacked, thickness)
        batchLine(x2, y, x, y, colorPacked, thickness)

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * 4)
    }

    /**
     * Draws a filled rectangle with the given dimensions.
     * @param x the x-coord of the rectangle
     * @param y the y-coord of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param color the color of the fill
     */
    fun rectFilled(x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE) {
        batch.draw(slice, x, y, width = width, height = height, colorBits = color.toFloatBits())
    }

    /**
     * Draws a triangle outline with equal side lengths.
     * @param x the x-coord of the triangle
     * @param y the y-coord of the triangle
     * @param length the length of each side of the triangle
     * @param color the color of the outline
     * @param thickness the thickness of the outline in pixels
     */
    fun triangle(x: Float, y: Float, length: Float, color: Color = Color.WHITE, thickness: Int = 2) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        batchLine(x, y, x + length * 0.5f, y - length, colorPacked, thickness)
        batchLine(x + length * 0.5f, y - length, x + length, y, colorPacked, thickness)
        batchLine(x + length, y, x, y, colorPacked, thickness)

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * 3)
    }

    /**
     * Draws a triangle outline base on the three given points.
     * @param x the x-coord of the first point
     * @param y the y-coord of the first point
     * @param x2 the x-coord of the second point
     * @param y2 the y-coord of the second point
     * @param x3 the x-coord of the third point
     * @param y3 the y-coord of the third point
     * @param color the color of the outline
     * @param thickness the thickness of the outline in pixels
     */
    fun triangle(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: Color = Color.WHITE,
        thickness: Int = 2,
    ) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        batchLine(x, y, x2, y2, colorPacked, thickness)
        batchLine(x2, y2, x3, y3, colorPacked, thickness)
        batchLine(x3, y3, x, y, colorPacked, thickness)

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * 3)
    }

    /**
     * Draws a filled triangle with equal side lengths.
     * @param x the x-coord of the triangle
     * @param y the y-coord of the triangle
     * @param length the length of each side of the triangle
     * @param color the color of the fill
     */
    fun triangleFilled(x: Float, y: Float, length: Float, color: Color = Color.WHITE) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        // bottom left
        vertices += x
        vertices += y
        vertices += colorPacked
        vertices += u
        vertices += if (slice.rotated) v2 else v

        // top left (middle point)
        vertices += x + length * 0.5f
        vertices += y - length
        vertices += colorPacked
        vertices += if (slice.rotated) u2 else u
        vertices += v2

        // top right (middle point)
        vertices += x + length * 0.5f
        vertices += y - length
        vertices += colorPacked
        vertices += u2
        vertices += if (slice.rotated) v else v2

        // bottom right
        vertices += x + length
        vertices += y
        vertices += colorPacked
        vertices += if (slice.rotated) u else u2
        vertices += v

        batch.draw(slice.texture, vertices.data, 0, 5 * 4)
    }

    /**
     * Draws a filled triangle with equal side lengths.
     * @param x the x-coord of the first point
     * @param y the y-coord of the first point
     * @param x2 the x-coord of the second point
     * @param y2 the y-coord of the second point
     * @param x3 the x-coord of the third point
     * @param y3 the y-coord of the third point
     * @param color the color of the fill
     */
    fun triangleFilled(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: Color = Color.WHITE,
    ) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        // bottom left
        vertices += x
        vertices += y
        vertices += colorPacked
        vertices += u
        vertices += if (slice.rotated) v2 else v

        // top left (middle point)
        vertices += x2
        vertices += y2
        vertices += colorPacked
        vertices += if (slice.rotated) u2 else u
        vertices += v2

        // top right (middle point)
        vertices += x2
        vertices += y2
        vertices += colorPacked
        vertices += u2
        vertices += if (slice.rotated) v else v2

        // bottom right
        vertices += x3
        vertices += y3
        vertices += colorPacked
        vertices += if (slice.rotated) u else u2
        vertices += v

        batch.draw(slice.texture, vertices.data, 0, 5 * 4)
    }


    /**
     * Draws a circle outline with the given radius's.
     * @param x the x-coord for center of the circle
     * @param y the y-coord for center of the circle
     * @param radius the radius of the circle
     * @param color the color of the outline
     * @param segments the number of segments to render the circle with. The higher the segments the smoother it looks
     * but at the cost of more vertices.
     * @param thickness the thickness of the outline
     */
    fun circle(
        x: Float,
        y: Float,
        radius: Float,
        color: Color = Color.WHITE,
        segments: Int = 32,
        thickness: Int = 2,
    ) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        val pi2 = (PI * 2f).toFloat()

        for (i in 0..segments) {
            val x1 = x + (radius * cos(i * pi2 / segments))
            val y1 = y + (radius * sin(i * pi2 / segments))
            val x2 = x + (radius * cos((i + 1) * pi2 / segments))
            val y2 = y + (radius * sin((i + 1) * pi2 / segments))
            batchLine(x1, y1, x2, y2, colorPacked, thickness)
        }

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * segments)
    }

    /**
     * Draws a filled circle with a given radius.
     * @param x the x-coord for center of the circle
     * @param y the y-coord for center of the circle
     * @param radius the radius of the circle
     * @param color the color of the fill
     * @param segments the number of segments to render the circle with. The higher the segments the smoother it looks
     * but at the cost of more vertices.
     */
    fun circleFilled(x: Float, y: Float, radius: Float, color: Color = Color.WHITE, segments: Int = 32) {
        return ellipseFilled(x, y, radius, radius, color, segments)
    }


    /**
     * Draws an ellipse outline with the given radius's.
     * @param x the x-coord for center of the ellipse
     * @param y the y-coord for center of the ellipse
     * @param rx the horizontal radius of the ellipse
     * @param ry the vertical radius of the ellipse
     * @param color the color of the outline
     * @param segments the number of segments to render the ellipse with. The higher the segments the smoother it looks
     * but at the cost of more vertices.
     * @param thickness the thickness of the outline
     */
    fun ellipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float,
        color: Color = Color.WHITE,
        segments: Int = 32,
        thickness: Int = 2,
    ) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        val pi2 = (PI * 2f).toFloat()

        for (i in 0..segments) {
            val x1 = x + (rx * cos(i * pi2 / segments))
            val y1 = y + (ry * sin(i * pi2 / segments))
            val x2 = x + (rx * cos((i + 1) * pi2 / segments))
            val y2 = y + (ry * sin((i + 1) * pi2 / segments))
            batchLine(x1, y1, x2, y2, colorPacked, thickness)
        }

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * segments)
    }

    /**
     * Draws a filled ellipse with the given radius's.
     * @param x the x-coord for center of the ellipse
     * @param y the y-coord for center of the ellipse
     * @param rx the horizontal radius of the ellipse
     * @param ry the vertical radius of the ellipse
     * @param color the color of the fill
     * @param segments the number of segments to render the ellipse with. The higher the segments the smoother it looks
     * but at the cost of more vertices.
     */
    fun ellipseFilled(x: Float, y: Float, rx: Float, ry: Float, color: Color = Color.WHITE, segments: Int = 32) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        val pi2 = (PI * 2f).toFloat()

        for (i in 0..segments) {
            vertices += x
            vertices += y
            vertices += colorPacked
            vertices += u
            vertices += if (slice.rotated) v2 else v

            vertices += x + (rx * cos(i * pi2 / segments))
            vertices += y + (ry * sin(i * pi2 / segments))
            vertices += colorPacked
            vertices += if (slice.rotated) u else u2
            vertices += v

            vertices += x + (rx * cos((i + 1) * pi2 / segments))
            vertices += y + (ry * sin((i + 1) * pi2 / segments))
            vertices += colorPacked
            vertices += u2
            vertices += if (slice.rotated) v else v2

            vertices += x
            vertices += y
            vertices += colorPacked
            vertices += if (slice.rotated) u2 else u
            vertices += v2
        }

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 * segments)
    }
}