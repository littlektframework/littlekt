package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.BindGroup
import com.littlekt.graphics.webgpu.RenderPipeline

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
data class MaterialPipeline(
    val shader: Shader,
    val renderOrder: RenderOrder,
    val layout: VertexBufferLayout,
    val renderPipeline: RenderPipeline,
    val bindGroups: List<BindGroup>,
)
