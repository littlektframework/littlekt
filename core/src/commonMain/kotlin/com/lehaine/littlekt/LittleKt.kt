package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.shader.FragmentShader
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import kotlin.time.Duration

/**
 * A [LittleKt] is the base of an [Application] when it is created, resumed, rendering, paused, or destroyed.
 * @author Colton Daily
 * @date 9/29/2021
 */
open class LittleKt(val application: Application) : Application by application {

    open fun render(dt: Duration) {}

    open fun resize(width: Int, height: Int) {}

    open fun resume() {}

    open fun pause() {}

    open fun dispose() {}
}

fun <T : Application> T.loadTexture(assetPath: String, onLoad: (Texture) -> Unit) = fileHandler.launch {
    onLoad(loadTexture(assetPath))
}

/**
 * Creates a new [ShaderProgram] for the specified shaders.
 * @param vertexShader the vertex shader to use. Defaults to [DefaultVertexShader].
 * @param fragmentShader the fragment shader to use. Defaults to [DefaultFragmentShader].
 */
fun <T : Application> T.createShader(
    vertexShader: VertexShader = DefaultVertexShader(),
    fragmentShader: FragmentShader = DefaultFragmentShader()
) =
    ShaderProgram(gl, vertexShader, fragmentShader)