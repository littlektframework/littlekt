package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShader

/**
 * A [ContextListener] is the base of an [Context] when it is created, rendering, or destroyed.
 * @author Colton Daily
 * @date 9/29/2021
 */
abstract class ContextListener(val context: Context) {

    /**
     * Invoked once the [Context] is ready. Add all the rendering, updating, dispose, and other game logic here.
     * @see [Context.onRender]
     * @see [Context.onPostRender]
     * @see [Context.onResize]
     * @see [Context.onDispose]
     */
    open suspend fun Context.start() {}
}


/**
 * Creates a new [ShaderProgram] for the specified shaders.
 * @param vertexShader the vertex shader to use.
 * @param fragmentShader the fragment shader to use.
 */
fun <T : ContextListener, V : VertexShader, F : FragmentShader> T.createShader(
    vertexShader: V,
    fragmentShader: F,
) = ShaderProgram(vertexShader, fragmentShader).also { it.prepare(context) }