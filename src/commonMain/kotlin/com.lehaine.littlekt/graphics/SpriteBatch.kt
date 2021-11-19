package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.GL
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.shader.ShaderProgram
import com.lehaine.littlekt.shader.fragment.TexturedFragmentShader
import com.lehaine.littlekt.shader.vertex.TexturedQuadShader

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class SpriteBatch(
    val gl: GL,
    val size: Int = 1000,
    val defaultShader: ShaderProgram = ShaderProgram(gl, TexturedQuadShader(), TexturedFragmentShader())
) {

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