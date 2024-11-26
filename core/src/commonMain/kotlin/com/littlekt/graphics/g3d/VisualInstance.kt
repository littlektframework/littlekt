package com.littlekt.graphics.g3d

import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
abstract class VisualInstance : Node3D() {

    open fun build(
        device: Device,
        shader: ShaderModule,
        uniformsBindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String = "vs_main",
        fragmentShaderEntryPoint: String = "fs_main",
    ) {
        children.forEach {
            it.build(
                device,
                shader,
                uniformsBindGroupLayout,
                colorFormat,
                depthFormat,
                vertexShaderEntryPoint,
                fragmentShaderEntryPoint,
            )
        }
    }

    open fun render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup) {
        children.forEach { it.render(renderPassEncoder, bindGroup) }
    }

    protected fun Node3D.build(
        device: Device,
        shader: ShaderModule,
        uniformsBindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String = "vs_main",
        fragmentShaderEntryPoint: String = "fs_main",
    ) {
        if (this is VisualInstance) {
            build(
                device,
                shader,
                uniformsBindGroupLayout,
                colorFormat,
                depthFormat,
                vertexShaderEntryPoint,
                fragmentShaderEntryPoint,
            )
        } else {
            children.forEach {
                it.build(
                    device,
                    shader,
                    uniformsBindGroupLayout,
                    colorFormat,
                    depthFormat,
                    vertexShaderEntryPoint,
                    fragmentShaderEntryPoint,
                )
            }
        }
    }

    protected fun Node3D.render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup) {
        if (this is VisualInstance) {
            render(renderPassEncoder, bindGroup)
        } else {
            children.forEach { it.render(renderPassEncoder, bindGroup) }
        }
    }
}
