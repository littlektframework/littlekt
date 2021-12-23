package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import kotlin.time.Duration

/**
 * A [ContextListener] is the base of an [Context] when it is created, resumed, rendering, paused, or destroyed.
 * @author Colton Daily
 * @date 9/29/2021
 */
abstract class ContextListener(val context: Context) : Context by context {

    open fun render(dt: Duration) {}

    open fun resize(width: Int, height: Int) {}

    open fun resume() {}

    open fun pause() {}

    open fun dispose() {}
}

/**
 * Creates a new [ShaderProgram] for the specified shaders.
 * @param vertexShader the vertex shader to use. Defaults to [DefaultVertexShader].
 * @param fragmentShader the fragment shader to use. Defaults to [DefaultFragmentShader].
 */
fun <T : Context> T.createShader(
    vertexShader: VertexShader = DefaultVertexShader(),
    fragmentShader: FragmentShader = DefaultFragmentShader()
) =
    ShaderProgram(vertexShader, fragmentShader).also { it.prepare(this) }