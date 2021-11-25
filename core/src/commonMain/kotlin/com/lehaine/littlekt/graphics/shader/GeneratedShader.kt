package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.graphics.shader.generator.GlslGenerator

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
open class GeneratedShader : GlslGenerator() {
    override var source: String = ""
        get() {
            if (field.isBlank()) {
                field = generate()
            }
            return field
        }
}