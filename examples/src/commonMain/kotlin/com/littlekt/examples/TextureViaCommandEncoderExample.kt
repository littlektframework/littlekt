package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import com.littlekt.graphics.createGPUByteBuffer
import com.littlekt.graphics.createGPUFloatBuffer
import com.littlekt.graphics.createGPUShortBuffer
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupDescriptor.*
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.*
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry.*
import io.ygdrasil.webgpu.BufferUsage
import io.ygdrasil.webgpu.ColorWriteMask
import io.ygdrasil.webgpu.ImageCopyBuffer
import io.ygdrasil.webgpu.ImageCopyTexture
import io.ygdrasil.webgpu.IndexFormat
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.PipelineLayoutDescriptor
import io.ygdrasil.webgpu.PrimitiveTopology
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor.*
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState.*
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout.*
import io.ygdrasil.webgpu.SamplerDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.ShaderStage
import io.ygdrasil.webgpu.Size3D
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceTextureStatus
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage
import io.ygdrasil.webgpu.VertexFormat
import io.ygdrasil.webgpu.VertexStepMode

/**
 * An example rendering a texture using a [io.ygdrasil.webgpu.CommandEncoder] in pure WebGPU.
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
        val vbo = device.createGPUFloatBuffer("vbo", vertices, setOf(BufferUsage.Vertex))
        val ibo = device.createGPUShortBuffer("ibo", indices, setOf(BufferUsage.Index))
        val shader = device.createShaderModule(ShaderModuleDescriptor(textureShader))
        val preferredFormat = graphics.preferredFormat
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Size3D(image.width.toUInt(), image.height.toUInt()),
                    TextureFormat.RGBA8UnormSrgb,
                    setOf(TextureUsage.CopyDst, TextureUsage.TextureBinding)
                )
            )

        val queue = device.queue
        run {
            val textureBuffer =
                device.createGPUByteBuffer("tex temp", image.pixels.toArray(), setOf(BufferUsage.CopySrc))
            val commandEncoder = device.createCommandEncoder()
            commandEncoder.copyBufferToTexture(
                ImageCopyBuffer(textureBuffer, 0uL, image.width.toUInt() * 4u, image.height.toUInt()),
                ImageCopyTexture(texture),
                Size3D(image.width.toUInt(), image.height.toUInt())
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
                        Entry(0u, setOf(ShaderStage.Fragment), TextureBindingLayout()),
                        Entry(1u, setOf(ShaderStage.Fragment), SamplerBindingLayout())
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                    BindGroupDescriptor(
                        bindGroupLayout,
                        listOf(
                            BindGroupEntry(0u, TextureViewBinding(textureView)),
                            BindGroupEntry(1u, SamplerBinding(sampler))
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
                            4uL * Float.SIZE_BYTES.toULong(),
                            listOf(
                                VertexAttribute(VertexFormat.Float32x2, 0u, 0u),
                                VertexAttribute(
                                    VertexFormat.Float32x2,
                                    2uL * Float.SIZE_BYTES.toULong(),
                                    1u
                                )
                            ),
                            VertexStepMode.Vertex,
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
                            writeMask = ColorWriteMask.All
                        )
                    )
                ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TriangleList),
                depthStencil = null,
                multisample =
                    MultisampleState(count = 1u, mask = 0xFFFFFFFu, alphaToCoverageEnabled = false)
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)
        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
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
                        setOf(TextureUsage.RenderAttachment),
                        preferredFormat,
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
                                    loadOp = LoadOp.Clear,
                                    storeOp = StoreOp.Store,
                                    clearValue = Color.CLEAR.toWebGPUColor()
                                )
                            )
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0u, bindGroup)
            renderPassEncoder.setVertexBuffer(0u, vbo)
            renderPassEncoder.setIndexBuffer(ibo, IndexFormat.Uint16)
            renderPassEncoder.drawIndexed(indices.size.toUInt(), 1u)
            renderPassEncoder.end()

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
