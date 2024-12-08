package com.littlekt.graphics.g3d

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
open class PBRMaterial(
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    val metallicRoughnessTexture: Texture? = null,
    val normalTexture: Texture? = null,
    val emissiveFactor: Vec3f = Vec3f(0f),
    val emissiveTexture: Texture? = null,
    val occlusionTexture: Texture? = null,
    val occlusionStrength: Float = 1f,
    baseColorFactor: Color = Color.WHITE,
    baseColorTexture: Texture? = null,
    transparent: Boolean = false,
    doubleSided: Boolean = false,
    alphaCutoff: Float = 0f,
    castShadows: Boolean = true,
) :
    UnlitMaterial(
        baseColorFactor,
        baseColorTexture,
        transparent,
        doubleSided,
        alphaCutoff,
        castShadows,
    ) {

    override fun upload(device: Device) {
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
                    occlusionStrength,
                    alphaCutoff,
                    emissiveFactor.x,
                    emissiveFactor.y,
                    emissiveFactor.z,
                    // padding
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
        normalTexture?.let { normalTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(3, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(2, normalTexture.sampler)
            bgEntries += BindGroupEntry(3, normalTexture.view)
        }
        metallicRoughnessTexture?.let { metallicRoughnessTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(4, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(5, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(4, metallicRoughnessTexture.sampler)
            bgEntries += BindGroupEntry(5, metallicRoughnessTexture.view)
        }
        emissiveTexture?.let { emissiveTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(6, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(7, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(6, emissiveTexture.sampler)
            bgEntries += BindGroupEntry(7, emissiveTexture.view)
        }
        occlusionTexture?.let { occlusionTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(8, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(9, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(8, occlusionTexture.sampler)
            bgEntries += BindGroupEntry(9, occlusionTexture.view)
        }

        val bindGroupLayout =
            device.createBindGroupLayout(BindGroupLayoutDescriptor(bgLayoutEntries))
        val bindGroup = device.createBindGroup(BindGroupDescriptor(bindGroupLayout, bgEntries))

        this.paramBuffer = paramBuffer
        this.bindGroupLayout = bindGroupLayout
        this.bindGroup = bindGroup
    }
}
