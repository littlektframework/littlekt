package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.RenderPipeline

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
class BaseMaterialProvider : MaterialProvider {
    private val pipelineCache = mutableListOf<RenderPipeline>()

    override fun getMaterial(device: Device, attributes: List<VertexAttribute>): Material {
        TODO()
    }
}
