package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.TextureFormat

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
interface MaterialPipelineProvider {
    fun getMaterialPipeline(
        device: Device,
        cameraBuffers: CameraBuffers,
        meshNode: MeshNode,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline?
}
