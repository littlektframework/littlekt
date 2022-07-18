package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders primitive shapes using an existing [Batch].
 * @param batch the batch used to batch draw calls with
 * @param slice a 1x1 slice of a texture. Generally a single white pixel.
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRenderer(val batch: Batch, val slice: TextureSlice = Textures.white) {
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
     */
    fun line(x: Float, y: Float, x2: Float, y2: Float, color: Color = Color.WHITE, thickness: Int = 2) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        batchLine(x, y, x2, y2, colorPacked, thickness)

        batch.draw(slice.texture, vertices.data, 0, 5 * 4)
    }

    private fun batchLine(x: Float, y: Float, x2: Float, y2: Float, colorPacked: Float, thickness: Int = 2) {
        val dx = x2 - x
        val dy = y2 - y

        val sqrt = sqrt(dx * dx + dy * dy)

        val px = -dy / sqrt * thickness * 0.5f
        val py = dx / sqrt * thickness * 0.5f

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
     * Draws a circle with a given radius.
     * @param x the x-coord for center of the circle
     * @param y the y-coord for center of the circle
     * @param radius the radius of the circle
     * @param color the color of the fill
     * @param segments the number of segments to render the circle with. The higher the segments the smoother it looks
     * but at the cost of more vertices.
     */
    fun circle(x: Float, y: Float, radius: Float, color: Color = Color.WHITE, segments: Int = 32) {
        val colorPacked = color.toFloatBits()
        vertices.clear()

        val pi2 = (PI * 2f).toFloat()

        for (i in 0..segments) {
            vertices += x
            vertices += y
            vertices += colorPacked
            vertices += u
            vertices += if (slice.rotated) v2 else v

            vertices += x + (radius * cos(i * pi2 / segments))
            vertices += y + (radius * sin(i * pi2 / segments))
            vertices += colorPacked
            vertices += if (slice.rotated) u else u2
            vertices += v

            vertices += x + (radius * cos((i + 1) * pi2 / segments))
            vertices += y + (radius * sin((i + 1) * pi2 / segments))
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