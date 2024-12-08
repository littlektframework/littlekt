package com.littlekt.graphics.g3d.material

import com.littlekt.Releasable
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.webgpu.RenderPipeline
import com.littlekt.graphics.webgpu.RenderPipelineDescriptor

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
open class Material(
    var pipeline: RenderPipeline? = null,
    var pipelineDesc: RenderPipelineDescriptor? = null,
    var attributes: List<VertexAttribute>? = null,
) : Releasable {
    override fun release() = Unit
}
