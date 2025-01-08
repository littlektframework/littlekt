package com.littlekt.graphics.g3d.material

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4
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
    val normalTexture: Texture = normalTexture ?: Textures.textureWhite
    val emissiveTexture: Texture = emissiveTexture ?: Textures.textureWhite
    val occlusionTexture: Texture = occlusionTexture ?: Textures.textureWhite

    private val modelFloatBuffer = FloatBuffer(16)
    private val materialFloatBuffer = FloatBuffer(8)

    /** The [GPUBuffer] that holds the model transform matrix data. */
    private val modelUniformBuffer =
        device.createGPUFloatBuffer(
            "model.transform",
            modelFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    private val materialUniformBuffer =
        device.createGPUFloatBuffer(
            "material buffer",
            materialFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [modelUniformBufferBinding]. */
    private val modelUniformBufferBinding =
        BufferBinding(modelUniformBuffer, size = Float.SIZE_BYTES * 16L)

    /** The [BufferBinding] for [modelUniformBufferBinding]. */
    private val materialUniformBufferBinding by lazy {
        BufferBinding(materialUniformBuffer, size = Float.SIZE_BYTES * 8L)
    }

    override val key: Int = 31 * super.key + isFullyRough.hashCode()

    override fun createBindGroups(layouts: List<BindGroupLayout>): List<BindGroup> {
        val bindGroups = mutableListOf<BindGroup>()
        bindGroups +=
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[1],
                    listOf(BindGroupEntry(0, modelUniformBufferBinding)),
                )
            )

        bindGroups +=
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[2],
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
        return bindGroups.toList()
    }

    override fun update(transform: Mat4) {
        materialFloatBuffer.apply {
            set(0, baseColorFactor.r)
            set(1, baseColorFactor.g)
            set(2, baseColorFactor.b)
            set(3, baseColorFactor.a)
            set(4, metallicFactor)
            set(5, roughnessFactor)
            set(6, occlusionStrength)
            set(7, emissiveFactor.x)
            set(8, emissiveFactor.y)
            set(9, emissiveFactor.z)
            set(10, alphaCutoff)
            // padding but reset it anyway
            set(11, 0f)
        }
        device.queue.writeBuffer(materialUniformBuffer, materialFloatBuffer)
        device.queue.writeBuffer(modelUniformBuffer, transform.toBuffer(modelFloatBuffer))
    }

    override fun release() {
        modelUniformBuffer.release()
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
