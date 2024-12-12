package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.webgpu.Device

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
interface MaterialPipelineProvider {
    fun getMaterialPipeline(device: Device, meshNode: MeshNode): MaterialPipeline?
}
