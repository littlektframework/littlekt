package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graphics.shader.shaders.GpuTextVertexShader

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class ShaderTest(context: Context) : ContextListener(context) {

    init {
        println(GpuTextVertexShader().generate(context))
    }
}