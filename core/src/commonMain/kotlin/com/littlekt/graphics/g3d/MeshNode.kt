package com.littlekt.graphics.g3d

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshNode(val mesh: Mesh<*>) : VisualInstance() {
    val textures = mutableMapOf<String, Texture>()
    private var renderPipeline: RenderPipeline? = null
    private val modelFloatBuffer = FloatBuffer(16)
    private lateinit var modelUniformBuffer: GPUBuffer
    private lateinit var modelBindGroup: BindGroup

    override fun build(
        device: Device,
        shader: ShaderModule,
        uniformsBindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String,
        fragmentShaderEntryPoint: String,
    ) {
        super.build(
            device,
            shader,
            uniformsBindGroupLayout,
            colorFormat,
            depthFormat,
            vertexShaderEntryPoint,
            fragmentShaderEntryPoint,
        )
        globalTransform.toBuffer(modelFloatBuffer)
        modelUniformBuffer =
            device.createGPUFloatBuffer(
                "model uniform buffer",
                modelFloatBuffer.toArray(),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )
        val modelBindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout()))
                )
            )
        modelBindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    modelBindGroupLayout,
                    listOf(BindGroupEntry(0, BufferBinding(modelUniformBuffer))),
                )
            )
        val pipelineLayout =
            device.createPipelineLayout(
                PipelineLayoutDescriptor(listOf(uniformsBindGroupLayout, modelBindGroupLayout))
            )
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                    VertexState(
                        module = shader,
                        entryPoint = vertexShaderEntryPoint,
                        mesh.geometry.layout.gpuVertexBufferLayout,
                    ),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = fragmentShaderEntryPoint,
                        target =
                            ColorTargetState(
                                format = colorFormat,
                                blendState = BlendState.NonPreMultiplied,
                                writeMask = ColorWriteMask.ALL,
                            ),
                    ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                depthStencil =
                    DepthStencilState(
                        depthFormat,
                        depthWriteEnabled = true,
                        depthCompare = CompareFunction.LESS,
                    ),
                multisample =
                    MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false),
            )
        renderPipeline = device.createRenderPipeline(renderPipelineDesc)
    }

    override fun update(device: Device) {
        globalTransform.toBuffer(modelFloatBuffer)
        device.queue.writeBuffer(modelUniformBuffer, modelFloatBuffer)
        super.update(device)
    }

    override fun render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup) {
        super.render(renderPassEncoder, bindGroup)
        val renderPipeline =
            renderPipeline
                ?: error("$name hasn't been built yet! Call 'build()' once before rendering.")
        renderPassEncoder.setPipeline(renderPipeline)
        renderPassEncoder.setBindGroup(0, bindGroup)
        renderPassEncoder.setBindGroup(1, modelBindGroup)
        renderPassEncoder.setVertexBuffer(0, mesh.vbo)
        if (mesh is IndexedMesh<*>) {
            renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
            renderPassEncoder.drawIndexed(mesh.geometry.numIndices, 1)
        } else {
            renderPassEncoder.draw(mesh.geometry.numVertices, 1)
        }
    }
}
