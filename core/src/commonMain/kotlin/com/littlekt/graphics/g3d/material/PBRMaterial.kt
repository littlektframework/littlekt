package com.littlekt.graphics.g3d.material

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.TextureFormat
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
    baseColorTexture: Texture,
    baseColorFactor: Color = Color.WHITE,
    transparent: Boolean = false,
    doubleSided: Boolean = false,
    alphaCutoff: Float = 0f,
    castShadows: Boolean = true,
    textureFormat: TextureFormat = TextureFormat.RGBA8_UNORM,
    depthFormat: TextureFormat = TextureFormat.DEPTH24_PLUS_STENCIL8,
) :
    UnlitMaterial(
        baseColorTexture = baseColorTexture,
        baseColorFactor = baseColorFactor,
        transparent = transparent,
        doubleSided = doubleSided,
        alphaCutoff = alphaCutoff,
        castShadows = castShadows,
        textureFormat = textureFormat,
        depthFormat = depthFormat,
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as PBRMaterial

        if (metallicFactor != other.metallicFactor) return false
        if (roughnessFactor != other.roughnessFactor) return false
        if (metallicRoughnessTexture != other.metallicRoughnessTexture) return false
        if (normalTexture != other.normalTexture) return false
        if (emissiveFactor != other.emissiveFactor) return false
        if (emissiveTexture != other.emissiveTexture) return false
        if (occlusionTexture != other.occlusionTexture) return false
        if (occlusionStrength != other.occlusionStrength) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + metallicFactor.hashCode()
        result = 31 * result + roughnessFactor.hashCode()
        result = 31 * result + (metallicRoughnessTexture?.hashCode() ?: 0)
        result = 31 * result + (normalTexture?.hashCode() ?: 0)
        result = 31 * result + emissiveFactor.hashCode()
        result = 31 * result + (emissiveTexture?.hashCode() ?: 0)
        result = 31 * result + (occlusionTexture?.hashCode() ?: 0)
        result = 31 * result + occlusionStrength.hashCode()
        return result
    }
    //    override fun upload(device: Device) {
    //        val paramBuffer =
    //            device.createGPUFloatBuffer(
    //                "param buffer",
    //                floatArrayOf(
    //                    baseColorFactor.r,
    //                    baseColorFactor.g,
    //                    baseColorFactor.b,
    //                    baseColorFactor.a,
    //                    metallicFactor,
    //                    roughnessFactor,
    //                    occlusionStrength,
    //                    alphaCutoff,
    //                    emissiveFactor.x,
    //                    emissiveFactor.y,
    //                    emissiveFactor.z,
    //                    // padding
    //                    0f,
    //                ),
    //                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
    //            )
    //
    //        val bgLayoutEntries =
    //            mutableListOf(BindGroupLayoutEntry(0, ShaderStage.FRAGMENT,
    // BufferBindingLayout()))
    //        val bgEntries = mutableListOf(BindGroupEntry(0, BufferBinding(paramBuffer)))
    //
    //        baseColorTexture?.let { baseColorTexture ->
    //            bgLayoutEntries += BindGroupLayoutEntry(1, ShaderStage.FRAGMENT,
    // SamplerBindingLayout())
    //            bgLayoutEntries += BindGroupLayoutEntry(2, ShaderStage.FRAGMENT,
    // TextureBindingLayout())
    //            bgEntries += BindGroupEntry(1, baseColorTexture.sampler)
    //            bgEntries += BindGroupEntry(2, baseColorTexture.view)
    //        }
    //        normalTexture?.let { normalTexture ->
    //            bgLayoutEntries += BindGroupLayoutEntry(2, ShaderStage.FRAGMENT,
    // SamplerBindingLayout())
    //            bgLayoutEntries += BindGroupLayoutEntry(3, ShaderStage.FRAGMENT,
    // TextureBindingLayout())
    //            bgEntries += BindGroupEntry(2, normalTexture.sampler)
    //            bgEntries += BindGroupEntry(3, normalTexture.view)
    //        }
    //        metallicRoughnessTexture?.let { metallicRoughnessTexture ->
    //            bgLayoutEntries += BindGroupLayoutEntry(4, ShaderStage.FRAGMENT,
    // SamplerBindingLayout())
    //            bgLayoutEntries += BindGroupLayoutEntry(5, ShaderStage.FRAGMENT,
    // TextureBindingLayout())
    //            bgEntries += BindGroupEntry(4, metallicRoughnessTexture.sampler)
    //            bgEntries += BindGroupEntry(5, metallicRoughnessTexture.view)
    //        }
    //        emissiveTexture?.let { emissiveTexture ->
    //            bgLayoutEntries += BindGroupLayoutEntry(6, ShaderStage.FRAGMENT,
    // SamplerBindingLayout())
    //            bgLayoutEntries += BindGroupLayoutEntry(7, ShaderStage.FRAGMENT,
    // TextureBindingLayout())
    //            bgEntries += BindGroupEntry(6, emissiveTexture.sampler)
    //            bgEntries += BindGroupEntry(7, emissiveTexture.view)
    //        }
    //        occlusionTexture?.let { occlusionTexture ->
    //            bgLayoutEntries += BindGroupLayoutEntry(8, ShaderStage.FRAGMENT,
    // SamplerBindingLayout())
    //            bgLayoutEntries += BindGroupLayoutEntry(9, ShaderStage.FRAGMENT,
    // TextureBindingLayout())
    //            bgEntries += BindGroupEntry(8, occlusionTexture.sampler)
    //            bgEntries += BindGroupEntry(9, occlusionTexture.view)
    //        }
    //
    //        val bindGroupLayout =
    //            device.createBindGroupLayout(BindGroupLayoutDescriptor(bgLayoutEntries))
    //        val bindGroup = device.createBindGroup(BindGroupDescriptor(bindGroupLayout,
    // bgEntries))
    //
    //        this.paramBuffer = paramBuffer
    //        // this.bindGroupLayout = bindGroupLayout
    //        // this.bindGroup = bindGroup
    //    }
}
