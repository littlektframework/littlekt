package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.ShaderSrc

/**
 * @author Colton Daily
 * @date 2/9/2025
 */
class ShaderTestSrc(raw: String) : ShaderSrc() {
    override val src: String = raw.trim().format()
}
