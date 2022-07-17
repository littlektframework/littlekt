package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
     * Draws a rectangle with the given dimensions.
     * @param x the x-coord of the rectangle
     * @param y the y-coord of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     */
    fun rect(x: Float, y: Float, width: Float, height: Float, color: Color = Color.WHITE) {
        batch.draw(slice, x, y, width = width, height = height, colorBits = color.toFloatBits())
    }

    /**
     * Draws a triangle with equal side lengths.
     * @param x the x-coord of the triangle
     * @param y the y-coord of the triangle
     * @param length the length of each side of the triangle
     */
    fun triangle(x: Float, y: Float, length: Float, color: Color = Color.WHITE) {
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
        vertices += y - length * 0.5f
        vertices += colorPacked
        vertices += if (slice.rotated) u2 else u
        vertices += v2

        // top right (middle point)
        vertices += x + length * 0.5f
        vertices += y - length * 0.5f
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
     * Draws a circle with a given radius.
     * @param x the x-coord for center of the circle
     * @param y the y-coord for center of the circle
     * @param radius the radius of the circle
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

        batch.draw(slice.texture, vertices.data, 0, 5 * 4 *  segments)
    }
}