package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.Device

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
interface ShaderProvider {
    fun getShader(device: Device, attributes: List<VertexAttribute>): Shader
}
