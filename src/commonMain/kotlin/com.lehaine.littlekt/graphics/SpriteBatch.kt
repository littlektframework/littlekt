package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.GL
import com.lehaine.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class SpriteBatch(val gl: GL) {

    private var drawing = false
    private var renderCalls = 0

    private var transformMatrix = Mat4()
    private var projectionMatrix = Mat4()
    private var combinedMatrix = Mat4()

    fun begin() {
        if (drawing) {
            throw IllegalStateException("SpriteBatch.end must be called before begin")
        }
        renderCalls = 0

        gl.disable(GL.DEPTH_BUFFER_BIT)

        setupMatrices()

        drawing = true
    }

    private fun setupMatrices() {
        combinedMatrix = projectionMatrix * transformMatrix
    }
}