package com.littlekt.graphics.g3d

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshNode(
    val mesh: Mesh<*>,
    val material: Material,
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
) : VisualInstance() {
    private val indexedMesh = mesh as? IndexedMesh<*>
    private var renderPipeline: RenderPipeline? = null
    private val modelFloatBuffer = FloatBuffer(16)
    private lateinit var modelUniformBuffer: GPUBuffer
    private lateinit var modelBindGroup: BindGroup

    init {
        if (
            topology == PrimitiveTopology.TRIANGLE_STRIP || topology == PrimitiveTopology.LINE_STRIP
        ) {
            check(stripIndexFormat != null) {
                error("MeshNode.stripIndexFormat is required to be set for strip topologies!")
            }
        }
    }

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
        material.upload(device)
        globalTransform.toBuffer(modelFloatBuffer)
        modelUniformBuffer =
            device.createGPUFloatBuffer(
                label = "model uniform buffer",
                data = modelFloatBuffer.toArray(),
                usage = BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )
        val modelBindGroupLayout =
            device.createBindGroupLayout(
                desc =
                    BindGroupLayoutDescriptor(
                        entries =
                            listOf(
                                BindGroupLayoutEntry(
                                    binding = 0,
                                    visibility = ShaderStage.VERTEX,
                                    bindingLayout = BufferBindingLayout(),
                                )
                            )
                    )
            )
        modelBindGroup =
            device.createBindGroup(
                desc =
                    BindGroupDescriptor(
                        layout = modelBindGroupLayout,
                        entries =
                            listOf(
                                BindGroupEntry(
                                    binding = 0,
                                    resource = BufferBinding(modelUniformBuffer),
                                )
                            ),
                    )
            )
        val pipelineLayout =
            device.createPipelineLayout(
                desc =
                    PipelineLayoutDescriptor(
                        bindGroupLayouts =
                            listOf(
                                uniformsBindGroupLayout,
                                modelBindGroupLayout,
                                material.bindGroupLayout,
                            )
                    )
            )
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                    VertexState(
                        module = shader,
                        entryPoint = vertexShaderEntryPoint,
                        buffer = mesh.geometry.layout.gpuVertexBufferLayout,
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
                primitive =
                    PrimitiveState(
                        topology = topology,
                        stripIndexFormat =
                            if (topology.isStripTopology()) stripIndexFormat else null,
                        cullMode = if (material.doubleSided) CullMode.NONE else CullMode.BACK,
                    ),
                depthStencil = DepthStencilState.depthWrite(depthFormat),
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
        renderPassEncoder.setBindGroup(2, material.bindGroup)
        renderPassEncoder.setVertexBuffer(0, mesh.vbo)
        // TODO check for any MeshInstance children
        if (indexedMesh != null) {
            renderPassEncoder.setIndexBuffer(
                indexedMesh.ibo,
                stripIndexFormat ?: IndexFormat.UINT16,
            )
            renderPassEncoder.drawIndexed(indexedMesh.geometry.numIndices, 1)
        } else {
            renderPassEncoder.draw(mesh.geometry.numVertices, 1)
        }
    }
}
