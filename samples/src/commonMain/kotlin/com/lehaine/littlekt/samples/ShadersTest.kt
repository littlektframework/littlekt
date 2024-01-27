package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.generator.Precision
import com.lehaine.littlekt.graphics.shader.generator.type.sampler.Sampler2D
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec2
import com.lehaine.littlekt.graphics.shader.generator.type.vec.Vec4
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import com.lehaine.littlekt.input.Key

/**
 * @author Colton Daily
 * @date 1/27/2024
 */
class ShadersTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val vertexShader = DefaultVertexShader()
        val fragmentShader = TestMultiTargetFragmentShader()

        logger.info { vertexShader.generate(context) }
        logger.info { fragmentShader.generate(context) }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}

private class TestMultiTargetFragmentShader : FragmentShaderModel() {
    val uTexture get() = parameters["u_texture"] as ShaderParameter.UniformSample2D

    private val u_texture by uniform(::Sampler2D)

    private val v_color by varying(::Vec4, Precision.LOW)
    private val v_texCoords by varying(::Vec2)

    init {
        gl_FragData[0] = v_color * texture2D(u_texture, v_texCoords).lit
        gl_FragData[1] = v_color * texture2D(u_texture, v_texCoords).lit
        gl_FragData[14] = v_color * texture2D(u_texture, v_texCoords).lit
    }
}