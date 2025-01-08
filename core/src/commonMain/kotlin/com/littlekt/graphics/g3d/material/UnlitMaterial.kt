package com.littlekt.graphics.g3d.material

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/29/2024
 */
open class UnlitMaterial(
    val device: Device,
    override val baseColorTexture: Texture,
    override val baseColorFactor: Color = Color.WHITE,
    override val transparent: Boolean = false,
    override val doubleSided: Boolean = false,
    override val alphaCutoff: Float = 0f,
    override val castShadows: Boolean = true,
    override val depthWrite: Boolean = true,
    override val depthCompareFunction: CompareFunction = CompareFunction.LESS,
) : Material() {

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

    override fun createBindGroups(layouts: List<BindGroupLayout>): List<BindGroup> {
        // we are assuming the camera bind group will be set externally
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
            // padding but reset it anyway
            set(4, 0f)
            set(5, 0f)
            set(6, 0f)
            set(7, 0f)
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

        other as UnlitMaterial

        if (baseColorTexture != other.baseColorTexture) return false
        if (baseColorFactor != other.baseColorFactor) return false
        if (transparent != other.transparent) return false
        if (doubleSided != other.doubleSided) return false
        if (alphaCutoff != other.alphaCutoff) return false
        if (castShadows != other.castShadows) return false
        if (depthWrite != other.depthWrite) return false
        if (depthCompareFunction != other.depthCompareFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseColorTexture.hashCode()
        result = 31 * result + baseColorFactor.hashCode()
        result = 31 * result + transparent.hashCode()
        result = 31 * result + doubleSided.hashCode()
        result = 31 * result + alphaCutoff.hashCode()
        result = 31 * result + castShadows.hashCode()
        result = 31 * result + depthWrite.hashCode()
        result = 31 * result + depthCompareFunction.hashCode()
        return result
    }
}
