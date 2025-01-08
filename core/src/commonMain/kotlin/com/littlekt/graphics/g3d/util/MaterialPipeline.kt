package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.RenderPipeline

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
data class MaterialPipeline(
    val shader: Shader,
    val environment: Environment,
    val renderOrder: RenderOrder,
    val layout: VertexBufferLayout,
    val renderPipeline: RenderPipeline,
) : Comparable<MaterialPipeline> {
    override fun compareTo(other: MaterialPipeline): Int {
        return renderOrder.order - other.renderOrder.order
    }
}
