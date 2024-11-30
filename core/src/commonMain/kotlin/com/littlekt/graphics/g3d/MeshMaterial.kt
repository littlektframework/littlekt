package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/29/2024
 */
open class MeshMaterial(
    val baseColorFactor: Color = Color.WHITE,
    val baseColorTexture: Texture? = null,
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    val metallicRoughnessTexture: Texture? = null,
) : Releasable {
    private lateinit var paramBuffer: GPUBuffer
        private set

    lateinit var bindGroupLayout: BindGroupLayout
        private set

    lateinit var bindGroup: BindGroup
        private set

    open fun upload(device: Device) {
        val paramBuffer =
            device.createGPUFloatBuffer(
                "param buffer",
                floatArrayOf(
                    baseColorFactor.r,
                    baseColorFactor.g,
                    baseColorFactor.b,
                    baseColorFactor.a,
                    metallicFactor,
                    roughnessFactor,
                    // padding
                    0f,
                    0f,
                ),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )

        val bgLayoutEntries =
            mutableListOf(BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()))
        val bgEntries = mutableListOf(BindGroupEntry(0, BufferBinding(paramBuffer)))

        baseColorTexture?.let { baseColorTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(1, baseColorTexture.sampler)
            bgEntries += BindGroupEntry(2, baseColorTexture.view)
        }

        metallicRoughnessTexture?.let { metallicRoughnessTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(3, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(4, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(3, metallicRoughnessTexture.sampler)
            bgEntries += BindGroupEntry(4, metallicRoughnessTexture.view)
        }

        val bindGroupLayout =
            device.createBindGroupLayout(BindGroupLayoutDescriptor(bgLayoutEntries))
        val bindGroup = device.createBindGroup(BindGroupDescriptor(bindGroupLayout, bgEntries))

        this.paramBuffer = paramBuffer
        this.bindGroupLayout = bindGroupLayout
        this.bindGroup = bindGroup
    }

    override fun release() {
        paramBuffer.release()
        bindGroupLayout.release()
        bindGroup.release()
    }
}
