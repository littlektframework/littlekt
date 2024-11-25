package com.littlekt.graphics.g3d

import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshNode(val mesh: Mesh<*>) : VisualInstance() {
    private var renderPipeline: RenderPipeline? = null

    override fun build(
        device: Device,
        shader: ShaderModule,
        bindGroupLayout: BindGroupLayout,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        vertexShaderEntryPoint: String,
        fragmentShaderEntryPoint: String,
    ) {
        val pipelineLayout =
            device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout)))
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

    override fun render(renderPassEncoder: RenderPassEncoder, bindGroup: BindGroup) {
        val renderPipeline =
            renderPipeline
                ?: error("$name hasn't been built yet! Call 'build()' once before rendering.")
        renderPassEncoder.setPipeline(renderPipeline)
        renderPassEncoder.setBindGroup(0, bindGroup)
        renderPassEncoder.setVertexBuffer(0, mesh.vbo)
        if (mesh is IndexedMesh<*>) {
            renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
            renderPassEncoder.drawIndexed(mesh.geometry.numIndices, 1)
        } else {
            renderPassEncoder.draw(mesh.geometry.numVertices, 1)
        }
    }
}
