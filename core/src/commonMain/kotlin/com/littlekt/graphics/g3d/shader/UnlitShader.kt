package com.littlekt.graphics.g3d.shader

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.ModelShaderUtils
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 12/10/2024
 */
open class UnlitShader(
    device: Device,
    layout: List<VertexAttribute>,
    val baseColorTexture: Texture,
    val baseColorFactor: Color = Color.WHITE,
    val transparent: Boolean = false,
    val doubleSided: Boolean = false,
    val alphaCutoff: Float = 0f,
    val castShadows: Boolean = true,
    val depthWrite: Boolean = true,
    val depthCompareFunction: CompareFunction = CompareFunction.LESS,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String = ModelShaderUtils.createVertexSource(layout, vertexEntryPoint),
    fragmentSrc: String = ModelShaderUtils.Unlit.createFragmentSource(layout, fragmentEntryPoint),
    bindGroupLayout: List<BindGroupLayoutDescriptor> =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // model
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // material uniform
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                    // baseColorTexture
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // baseColorSampler
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                )
            ),
        ),
) :
    Shader(
        device = device,
        src = "$vertexSrc\n$fragmentSrc",
        layout = bindGroupLayout,
        vertexEntryPoint = vertexEntryPoint,
        fragmentEntryPoint = fragmentEntryPoint,
    ) {
    private val camFloatBuffer = FloatBuffer(16)
    private val modelFloatBuffer = FloatBuffer(16)

    open val key: Int =
        kotlin.run {
            var result = layout.hashCode()
            result = 31 * result + transparent.hashCode()
            result = 31 * result + doubleSided.hashCode()
            result = 31 * result + alphaCutoff.hashCode()
            result = 31 * result + castShadows.hashCode()
            result = 31 * result + depthWrite.hashCode()
            result = 31 * result + depthCompareFunction.hashCode()
            result
        }

    /**
     * The [GPUBuffer] that holds the camera view-projection matrix data.
     *
     * @see updateCameraUniform
     */
    protected val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera.viewProj",
            camFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [GPUBuffer] that holds the model transform matrix data. */
    protected val modelUniformBuffer =
        device.createGPUFloatBuffer(
            "model.transform",
            modelFloatBuffer.toArray(),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    protected val materialUniformBuffer =
        device.createGPUFloatBuffer(
            "material buffer",
            floatArrayOf(
                baseColorFactor.r,
                baseColorFactor.g,
                baseColorFactor.b,
                baseColorFactor.a,
                alphaCutoff,
                // padding
                0f,
                0f,
                0f,
            ),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    protected val cameraUniformBufferBinding =
        BufferBinding(cameraUniformBuffer, size = Float.SIZE_BYTES * 16L)

    /** The [BufferBinding] for [modelUniformBufferBinding]. */
    protected val modelUniformBufferBinding =
        BufferBinding(modelUniformBuffer, size = Float.SIZE_BYTES * 16L)

    /** The [BufferBinding] for [modelUniformBufferBinding]. */
    protected val materialUniformBufferBinding =
        BufferBinding(materialUniformBuffer, size = Float.SIZE_BYTES * 8L)

    override fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>) {
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[0],
                    listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
                )
            )
        )
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[1],
                    listOf(BindGroupEntry(0, modelUniformBufferBinding)),
                )
            )
        )
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[2],
                    listOf(
                        BindGroupEntry(0, materialUniformBufferBinding),
                        BindGroupEntry(1, baseColorTexture.view),
                        BindGroupEntry(1, baseColorTexture.sampler),
                    ),
                )
            )
        )
    }

    override fun setBindGroups(
        encoder: RenderPassEncoder,
        bindGroups: List<BindGroup>,
        dynamicOffsets: List<Long>,
    ) {
        val firstBindGroupIndex = 1
        var i = firstBindGroupIndex
        bindGroups.forEach { bindGroup -> encoder.setBindGroup(i++, bindGroup) }
    }

    /**
     * Updates either the cameras view-projection matrix, or the model transform matrix, or both.
     *
     * ```
     * data[VIEW_PROJECTION] = camera.viewProj
     * data[MODEL] = mesh.globalTransform
     * update(data)
     * ```
     *
     * @see [VIEW_PROJECTION]
     * @see [MODEL]
     */
    override fun update(data: Map<String, Any>) {
        (data[VIEW_PROJECTION] as? Mat4)?.let { viewProjectionMatrix ->
            updateCameraUniform(viewProjectionMatrix)
        }

        (data[MODEL] as? Mat4)?.let { modelMatrix -> updateModelUniform(modelMatrix) }
    }

    /**
     * Update this [cameraUniformBuffer] with the given view-projection matrix.
     *
     * @param viewProjection the matrix to update the camera
     */
    fun updateCameraUniform(viewProjection: Mat4) =
        device.queue.writeBuffer(cameraUniformBuffer, viewProjection.toBuffer(camFloatBuffer))

    /**
     * Update this [modelUniformBufferBinding] with the given transform matrix.
     *
     * @param transform the matrix to update the model transform
     */
    fun updateModelUniform(transform: Mat4) =
        device.queue.writeBuffer(modelUniformBuffer, transform.toBuffer(modelFloatBuffer))

    override fun release() {
        super.release()
        cameraUniformBuffer.release()
        modelUniformBuffer.release()
        materialUniformBuffer.release()
    }

    companion object {
        const val VIEW_PROJECTION = "viewProjection"
        const val MODEL = "model"
    }
}
