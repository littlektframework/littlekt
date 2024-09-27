package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import io.ygdrasil.wgpu.ColorWriteMask
import io.ygdrasil.wgpu.LoadOp
import io.ygdrasil.wgpu.PipelineLayoutDescriptor
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.PrimitiveTopology
import io.ygdrasil.wgpu.RenderPassDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor
import io.ygdrasil.wgpu.ShaderModuleDescriptor
import io.ygdrasil.wgpu.StoreOp
import io.ygdrasil.wgpu.SurfaceTextureStatus
import io.ygdrasil.wgpu.TextureUsage

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
        val device = graphics.device
        val queue = device.queue
        val shader = device.createShaderModule(ShaderModuleDescriptor(shaderSrc))
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor())
        val preferredFormat = graphics.preferredFormat
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = RenderPipelineDescriptor.VertexState(module = shader, entryPoint = "vs_main"),
                fragment =
                RenderPipelineDescriptor.FragmentState(
                    module = shader,
                    entryPoint = "fs_main",
                    targets = listOf(
                        RenderPipelineDescriptor.FragmentState.ColorTargetState(
                            format = preferredFormat,
                            blend = BlendStates.Opaque,
                            writeMask = ColorWriteMask.all
                        )
                    )
                ),
                primitive = RenderPipelineDescriptor.PrimitiveState(topology = PrimitiveTopology.triangleList),
                depthStencil = null,
                multisample =
                RenderPipelineDescriptor.MultisampleState(count = 1, mask = 0xFFFFFFFu, alphaToCoverageEnabled = false)
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)
        graphics.configureSurface(
            setOf(TextureUsage.renderAttachment),
            preferredFormat,
            PresentMode.fifo,
            graphics.surface.supportedAlphaMode.first()
        )

        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                SurfaceTextureStatus.success -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                SurfaceTextureStatus.timeout,
                SurfaceTextureStatus.outdated,
                SurfaceTextureStatus.lost -> {
                    surfaceTexture.texture.close()
                    graphics.configureSurface(
                        setOf(TextureUsage.renderAttachment),
                        preferredFormat,
                        PresentMode.fifo,
                        graphics.surface.supportedAlphaMode.first()
                    )
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }
                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                }
            }
            val texture = checkNotNull(surfaceTexture.texture)
            val frame = texture.createView()

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(
                                RenderPassDescriptor.ColorAttachment(
                                    view = frame,
                                    loadOp = LoadOp.clear,
                                    storeOp = StoreOp.store,
                                    clearValue = Color.CLEAR.toWebGPUColor()
                                )
                            )
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.draw(3, 1, 0, 0)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            queue.submit(listOf(commandBuffer))
            graphics.surface.present()

            commandBuffer.close()
            commandEncoder.close()
            frame.close()
            texture.close()
        }

        onRelease {
            renderPipeline.close()
            pipelineLayout.close()
            shader.close()
        }
    }
}
