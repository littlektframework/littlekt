package com.littlekt.graphics.shader

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Texture
import io.ygdrasil.wgpu.Device
import com.littlekt.math.Mat4
import com.littlekt.util.align
import io.ygdrasil.wgpu.BindGroup
import io.ygdrasil.wgpu.BindGroupDescriptor.BufferBinding
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor
import io.ygdrasil.wgpu.BufferDescriptor
import io.ygdrasil.wgpu.BufferUsage

/**
 * A base shader class to handle creating a camera uniform [GPUBuffer] and expecting a texture to
 * passed in when invoking [createBindGroups].
 *
 * @param device the current [Device]
 * @param src the WGSL shader source code
 * @param layout a list of [BindGroupLayoutDescriptor] in order to create [BindGroupLayout]s for the
 *   [PipelineLayout]. The order should match the index of the [BindGroupLayout].
 * @param cameraDynamicSize the size in which the underlying [cameraUniformBuffer] should be
 *   multiplied by to handle dynamic camera uniform values.
 * @author Colton Daily
 * @date 4/14/2024
 */
abstract class SpriteShader(
    device: Device,
    src: String,
    layout: List<BindGroupLayoutDescriptor>,
    cameraDynamicSize: Int = 50
) : Shader(device, src, layout) {

    private val camFloatBuffer = FloatBuffer(16)

    /**
     * The [GPUBuffer] that holds the matrix data.
     *
     * @see updateCameraUniform
     */
    protected val cameraUniformBuffer = run {
        val buffer =
            device.createBuffer(
                BufferDescriptor(
                    label = "viewProj",
                    size = (Float.SIZE_BYTES * 16)
                        .align(device.limits.minUniformBufferOffsetAlignment)
                        .toLong() * cameraDynamicSize,
                    usage = setOf(BufferUsage.uniform, BufferUsage.copydst),
                    mappedAtCreation = true
                )
            )
        buffer.mapFrom(camFloatBuffer.toArray())
        buffer.unmap()

        buffer
    }

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    protected val cameraUniformBufferBinding =
        BufferBinding(
            buffer = cameraUniformBuffer,
            size =
            (Float.SIZE_BYTES * 16)
                .align(device.limits.minUniformBufferOffsetAlignment)
                .toLong(),
        )

    /** @see [createBindGroupsWithTexture] to override. */
    final override fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>) {
        val texture =
            data[TEXTURE] as? Texture
                ?: error(
                    "${this::class.simpleName} requires data[\"texture\", texture] to be set. No texture was found! Ensure the name is correct by using SpriteShader.TEXTURE."
                )
        createBindGroupsWithTexture(texture, data)
    }

    /**
     * Add the newly created bind groups to the given list.
     *
     * ```
     * fun MutableList<BindGroup>.createBindGroupsWithTexture(texture: Texture, data: Map<String, Any>): List<BindGroup> {
     *     add(device.createBindGroup(descriptor)
     *     add(device.createBindGroup(anotherDescriptor)
     * }
     * ```
     *
     * @param data the data needed in order to create the bind groups.
     */
    protected open fun MutableList<BindGroup>.createBindGroupsWithTexture(
        texture: Texture,
        data: Map<String, Any>
    ) = Unit

    /**
     * Checks for a view-projection matrix via `data[VIEW_PROJECTION]` and calls
     * [updateCameraUniform] internally.
     */
    override fun update(data: Map<String, Any>) {
        val viewProjectionMatrix =
            data[VIEW_PROJECTION] as? Mat4
                ?: error(
                    "${this::class.simpleName} requires data[\"${VIEW_PROJECTION}\", mat4] to be set. No matrix was found! Ensure the name is correct by using SpriteShader.VIEW_PROJECTION."
                )
        val dynamicOffset =
            data[CAMERA_UNIFORM_DYNAMIC_OFFSET] as? Long
                ?: (data[CAMERA_UNIFORM_DYNAMIC_OFFSET] as? Int)?.toLong()
                ?: 0L
        updateCameraUniform(viewProjectionMatrix, dynamicOffset)
    }

    /**
     * Update this [cameraUniformBuffer] with the given view-projection matrix.
     *
     * @param viewProjection the matrix to update the camera
     */
    fun updateCameraUniform(viewProjection: Mat4, dynamicOffset: Long = 0) =
        device.queue.writeBuffer(
            cameraUniformBuffer,
            dynamicOffset * device.limits.minUniformBufferOffsetAlignment.toLong(),
            viewProjection.toBuffer(camFloatBuffer).toArray()
        )

    override fun release() {
        super.release()
        cameraUniformBuffer.close()
    }

    companion object {
        const val TEXTURE = "texture"
        const val VIEW_PROJECTION = "viewProjection"
        const val CAMERA_UNIFORM_DYNAMIC_OFFSET = "cameraUniformDynamicOffset"
    }
}
