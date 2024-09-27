package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import com.littlekt.graphics.createGPUByteBuffer
import com.littlekt.graphics.createGPUFloatBuffer
import com.littlekt.graphics.createGPUShortBuffer
import io.ygdrasil.wgpu.BindGroupDescriptor
import io.ygdrasil.wgpu.BindGroupDescriptor.*
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.*
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.Entry.*
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.ColorWriteMask
import io.ygdrasil.wgpu.ImageCopyBuffer
import io.ygdrasil.wgpu.ImageCopyTexture
import io.ygdrasil.wgpu.IndexFormat
import io.ygdrasil.wgpu.LoadOp
import io.ygdrasil.wgpu.PipelineLayoutDescriptor
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.PrimitiveTopology
import io.ygdrasil.wgpu.RenderPassDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor.*
import io.ygdrasil.wgpu.RenderPipelineDescriptor.FragmentState.*
import io.ygdrasil.wgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout
import io.ygdrasil.wgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout.*
import io.ygdrasil.wgpu.SamplerDescriptor
import io.ygdrasil.wgpu.ShaderModuleDescriptor
import io.ygdrasil.wgpu.ShaderStage
import io.ygdrasil.wgpu.Size3D
import io.ygdrasil.wgpu.StoreOp
import io.ygdrasil.wgpu.SurfaceTextureStatus
import io.ygdrasil.wgpu.TextureDataLayout
import io.ygdrasil.wgpu.TextureDescriptor
import io.ygdrasil.wgpu.TextureDimension
import io.ygdrasil.wgpu.TextureFormat
import io.ygdrasil.wgpu.TextureUsage
import io.ygdrasil.wgpu.VertexFormat
import io.ygdrasil.wgpu.VertexStepMode

/**
 * An example rendering a texture using a [CommandEncoder] in pure WebGPU.
 *
 * @author Colton Daily
 * @date 4/5/2024
 */
class TextureViaCommandEncoderExample(context: Context) : ContextListener(context) {
    // language=wgsl
    private val textureShader =
        """
            struct VertexOutput {
                @location(0) uv: vec2<f32>,
                @builtin(position) position: vec4<f32>,
            };
                       
            
            @vertex
            fn vs_main(
                @location(0) pos: vec2<f32>,
                @location(1) uvs: vec2<f32>) -> VertexOutput {
                
                var output: VertexOutput;
                output.position = vec4<f32>(pos.x, pos.y, 0, 1);
                output.uv = uvs;
                
                return output;
            }
            
            @group(0) @binding(0)
            var my_texture: texture_2d<f32>;
            @group(0) @binding(1)
            var my_sampler: sampler;
            
            @fragment
            fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
                return textureSample(my_texture, my_sampler, in.uv);
            }
        """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        // @formatter:off
        val vertices =
            floatArrayOf(
                -0.5f,
                -0.5f,
                0f,
                1f,
                -0.5f,
                0.5f,
                0f,
                0f,
                0.5f,
                0.5f,
                1f,
                0f,
                0.5f,
                -0.5f,
                1f,
                1f,
            )
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        // @formatter:on
        val image = resourcesVfs["pika.png"].readPixmap()
        val device = graphics.device
        val vbo = device.createGPUFloatBuffer("vbo", vertices, setOf(BufferUsage.vertex))
        val ibo = device.createGPUShortBuffer("ibo", indices, setOf(BufferUsage.index))
        val shader = device.createShaderModule(ShaderModuleDescriptor(textureShader))
        val preferredFormat = graphics.preferredFormat
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Size3D(image.width, image.height),
                    TextureFormat.rgba8unormsrgb,
                    setOf(TextureUsage.copyDst, TextureUsage.textureBinding)
                )
            )

        val queue = device.queue
        run {
            val textureBuffer =
                device.createGPUByteBuffer("tex temp", image.pixels.toArray(), setOf(BufferUsage.copysrc))
            val commandEncoder = device.createCommandEncoder()
            commandEncoder.copyBufferToTexture(
                ImageCopyBuffer(textureBuffer, 0L, image.width * 4, image.height),
                ImageCopyTexture(texture),
                Size3D(image.width, image.height)
            )
            queue.submit(listOf(commandEncoder.finish()))
            textureBuffer.close()
        }

        val sampler = device.createSampler(SamplerDescriptor())
        val textureView = texture.createView()
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        Entry(0, setOf(ShaderStage.fragment), TextureBindingLayout()),
                        Entry(1, setOf(ShaderStage.fragment), SamplerBindingLayout())
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                    BindGroupDescriptor(
                        bindGroupLayout,
                        listOf(
                            BindGroupEntry(0, TextureViewBinding(textureView)),
                            BindGroupEntry(1, SamplerBinding(sampler))
                        )
                    )
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout)))
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                VertexState(
                    module = shader,
                    entryPoint = "vs_main",
                    buffers = listOf(
                        VertexBufferLayout(
                            4L * Float.SIZE_BYTES,
                            listOf(
                                VertexAttribute(VertexFormat.float32x2, 0, 0),
                                VertexAttribute(
                                    VertexFormat.float32x2,
                                    2L * Float.SIZE_BYTES,
                                    1
                                )
                            ),
                            VertexStepMode.vertex,
                        )
                    )
                ),
                fragment =
                FragmentState(
                    module = shader,
                    entryPoint = "fs_main",
                    targets =
                    listOf(
                        ColorTargetState(
                            format = preferredFormat,
                            blend = BlendStates.NonPreMultiplied,
                            writeMask = ColorWriteMask.all
                        )
                    )
                ),
                primitive = PrimitiveState(topology = PrimitiveTopology.triangleList),
                depthStencil = null,
                multisample =
                    MultisampleState(count = 1, mask = 0xFFFFFFFu, alphaToCoverageEnabled = false)
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
                    return@onUpdate
                }
            }
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

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
            renderPassEncoder.setBindGroup(0, bindGroup)
            renderPassEncoder.setVertexBuffer(0, vbo)
            renderPassEncoder.setIndexBuffer(ibo, IndexFormat.uint16)
            renderPassEncoder.drawIndexed(indices.size, 1)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            queue.submit(listOf(commandBuffer))
            graphics.surface.present()

            commandBuffer.close()
            commandEncoder.close()
            frame.close()
            swapChainTexture.close()
        }

        onRelease {
            renderPipeline.close()
            pipelineLayout.close()
            bindGroup.close()
            bindGroupLayout.close()
            sampler.close()
            textureView.close()
            texture.close()
            ibo.close()
            vbo.close()
            texture.close()
            shader.close()
        }
    }
}
