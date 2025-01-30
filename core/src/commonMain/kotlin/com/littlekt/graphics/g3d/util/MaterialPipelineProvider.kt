package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.graphics.webgpu.TextureFormat

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
interface MaterialPipelineProvider : Releasable {
    fun getMaterialPipeline(
        device: Device,
        material: Material,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        stripIndexFormat: IndexFormat?,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline?
}
