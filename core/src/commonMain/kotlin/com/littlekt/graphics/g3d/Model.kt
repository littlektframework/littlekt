package com.littlekt.graphics.g3d

import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
open class Model : VisualInstance() {
    val nodes = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, MeshNode>()
    val skins = mutableListOf<Skin>()

    override fun build(
        device: Device,
        shader: ShaderModule,
        bindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String,
        fragmentShaderEntryPoint: String,
    ) {
        meshes.values.forEach {
            it.build(
                device,
                shader,
                bindGroupLayout,
                colorFormat,
                depthFormat,
                vertexShaderEntryPoint,
                fragmentShaderEntryPoint,
            )
        }
    }

    override fun render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup) {
        meshes.values.forEach { it.render(renderPassEncoder, bindGroup) }
    }
}
