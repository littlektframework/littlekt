package com.littlekt.graphics.g3d

import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
abstract class VisualInstance : Node3D() {

    abstract fun build(
        device: Device,
        shader: ShaderModule,
        bindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String = "vs_main",
        fragmentShaderEntryPoint: String = "fs_main",
    )

    abstract fun render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup)
}
