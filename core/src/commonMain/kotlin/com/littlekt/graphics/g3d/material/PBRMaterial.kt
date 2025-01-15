package com.littlekt.graphics.g3d.material

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f
import com.littlekt.resources.Textures

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
class PBRMaterial(
    val device: Device,
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    metallicRoughnessTexture: Texture? = null,
    normalTexture: Texture? = null,
    val emissiveFactor: Vec3f = Vec3f(0f),
    emissiveTexture: Texture? = null,
    occlusionTexture: Texture? = null,
    val occlusionStrength: Float = 1f,
    override val baseColorTexture: Texture,
    override val baseColorFactor: Color = Color.WHITE,
    override val transparent: Boolean = false,
    override val doubleSided: Boolean = false,
    override val alphaCutoff: Float = 0f,
    override val castShadows: Boolean = true,
    override val depthWrite: Boolean = true,
    override val depthCompareFunction: CompareFunction = CompareFunction.LESS,
) : Material() {
    private val isFullyRough: Boolean = roughnessFactor == 1f && metallicRoughnessTexture == null

    val metallicRoughnessTexture: Texture = metallicRoughnessTexture ?: Textures.textureWhite
    val normalTexture: Texture = normalTexture ?: Textures.textureNormal
    val emissiveTexture: Texture = emissiveTexture ?: Textures.textureWhite
    val occlusionTexture: Texture = occlusionTexture ?: Textures.textureWhite

    private val materialFloatBuffer = FloatBuffer(12)

    private val materialUniformBuffer =
        device.createGPUFloatBuffer(
            "material buffer",
            materialFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [modelUniformBufferBinding]. */
    private val materialUniformBufferBinding by lazy { BufferBinding(materialUniformBuffer) }

    override val key: Int = 31 * super.key + isFullyRough.hashCode()

    override fun createBindGroup(shader: Shader): BindGroup {
        return device.createBindGroup(
            BindGroupDescriptor(
                shader.getBindGroupLayoutByUsage(BindingUsage.MATERIAL),
                listOf(
                    BindGroupEntry(0, materialUniformBufferBinding),
                    BindGroupEntry(1, baseColorTexture.view),
                    BindGroupEntry(2, baseColorTexture.sampler),
                    BindGroupEntry(3, normalTexture.view),
                    BindGroupEntry(4, normalTexture.sampler),
                    BindGroupEntry(5, metallicRoughnessTexture.view),
                    BindGroupEntry(6, metallicRoughnessTexture.sampler),
                    BindGroupEntry(7, occlusionTexture.view),
                    BindGroupEntry(8, occlusionTexture.sampler),
                    BindGroupEntry(9, emissiveTexture.view),
                    BindGroupEntry(10, emissiveTexture.sampler),
                ),
            )
        )
    }

    override fun update() {
        materialFloatBuffer.apply {
            set(0, baseColorFactor.r)
            set(1, baseColorFactor.g)
            set(2, baseColorFactor.b)
            set(3, baseColorFactor.a)
            set(4, metallicFactor)
            set(5, roughnessFactor)
            set(6, occlusionStrength)

            // padding but reset it anyway
            set(7, 0f)

            set(8, emissiveFactor.x)
            set(9, emissiveFactor.y)
            set(10, emissiveFactor.z)
            set(11, alphaCutoff)
        }
        device.queue.writeBuffer(materialUniformBuffer, materialFloatBuffer)
    }

    override fun release() {
        materialUniformBuffer.release()
    }

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
        result = 31 * result + (metallicRoughnessTexture.hashCode() ?: 0)
        result = 31 * result + (normalTexture.hashCode() ?: 0)
        result = 31 * result + emissiveFactor.hashCode()
        result = 31 * result + (emissiveTexture.hashCode() ?: 0)
        result = 31 * result + (occlusionTexture.hashCode() ?: 0)
        result = 31 * result + occlusionStrength.hashCode()
        return result
    }
}
