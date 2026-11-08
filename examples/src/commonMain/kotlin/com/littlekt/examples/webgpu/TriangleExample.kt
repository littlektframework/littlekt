package com.littlekt.examples.webgpu

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*
import com.littlekt.renderFrame

/**
 * An example rendering a simple triangle in pure WebGPU.
 *
 * @author Colton Daily
 * @date 4/4/2024
 */
class TriangleExample(context: Context) : ContextListener(context) {
    // language=wgsl
    private val shaderSrc =
        """
            @vertex
            fn vs_main(@builtin(vertex_index) in_vertex_index: u32) -> @builtin(position) vec4<f32> {
                let x = f32(i32(in_vertex_index) - 1);
                let y = f32(i32(in_vertex_index & 1u) * 2 - 1);
                return vec4<f32>(x, y, 0.0, 1.0);
            }
            
            @fragment
            fn fs_main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        configureSurfaceOnResize()
        val device = graphics.device
        val shader = device.createShaderModule(shaderSrc)
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor())
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = VertexState(module = shader, entryPoint = "vs_main"),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = "fs_main",
                        target =
                            ColorTargetState(
                                format = graphics.preferredFormat,
                                blendState = BlendState.Opaque,
                                writeMask = ColorWriteMask.ALL,
                            ),
                    ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                depthStencil = null,
                multisample =
                    MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false),
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)

        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPassEncoder =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, Color.CLEAR))
                    )
                renderPassEncoder.setPipeline(renderPipeline)
                renderPassEncoder.draw(3, 1, 0, 0)
                renderPassEncoder.end()
                renderPassEncoder.release()
            }
        }

        onRelease {
            renderPipeline.release()
            pipelineLayout.release()
            shader.release()
        }
    }
}
