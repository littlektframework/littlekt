package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.file.ShortBuffer
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import com.littlekt.graphics.createGPUFloatBuffer
import com.littlekt.graphics.createGPUShortBuffer
import io.ygdrasil.wgpu.BindGroupDescriptor
import io.ygdrasil.wgpu.BindGroupDescriptor.SamplerBinding
import io.ygdrasil.wgpu.BindGroupDescriptor.TextureViewBinding
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.Entry.SamplerBindingLayout
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.Entry.TextureBindingLayout
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.ColorWriteMask
import io.ygdrasil.wgpu.ImageCopyTexture
import io.ygdrasil.wgpu.IndexFormat
import io.ygdrasil.wgpu.LoadOp
import io.ygdrasil.wgpu.PipelineLayoutDescriptor
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.PrimitiveTopology
import io.ygdrasil.wgpu.RenderPassDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor.FragmentState
import io.ygdrasil.wgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout
import io.ygdrasil.wgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout.VertexAttribute
import io.ygdrasil.wgpu.SamplerDescriptor
import io.ygdrasil.wgpu.ShaderModuleDescriptor
import io.ygdrasil.wgpu.ShaderStage
import io.ygdrasil.wgpu.Size3D
import io.ygdrasil.wgpu.StoreOp
import io.ygdrasil.wgpu.SurfaceTextureStatus
import io.ygdrasil.wgpu.TextureDataLayout
import io.ygdrasil.wgpu.TextureDescriptor
import io.ygdrasil.wgpu.TextureFormat
import io.ygdrasil.wgpu.TextureUsage
import io.ygdrasil.wgpu.VertexFormat
import io.ygdrasil.wgpu.VertexStepMode

/**
 * An example showing drawing a texture with pure WebGPU.
 *
 * @author Colton Daily
 * @date 4/5/2024
 */
class TextureExample(context: Context) : ContextListener(context) {
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
        val vertices =
            FloatBuffer(
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
            )
        val indices = ShortBuffer(shortArrayOf(0, 1, 2, 0, 2, 3))
        val image = resourcesVfs["logo.png"].readPixmap()
        val device = graphics.device
        val vbo = device.createGPUFloatBuffer("vbo", vertices.toArray(), setOf(BufferUsage.vertex))
        val ibo = device.createGPUShortBuffer("ibo", indices.toArray(), setOf(BufferUsage.index))
        val shader = device.createShaderModule(ShaderModuleDescriptor(textureShader))
        val preferredFormat = graphics.preferredFormat
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Size3D(image.width, image.height),
                    TextureFormat.rgba8unorm,
                    setOf(TextureUsage.copyDst, TextureUsage.textureBinding)
                )
            )

        val queue = device.queue
        queue.writeTexture(
            data = image.pixels.toArray(),
            destination = ImageCopyTexture(texture),
            dataLayout = TextureDataLayout(0L, image.width * 4, image.height),
            size = Size3D(image.width, image.height)
        )

        val sampler = device.createSampler(SamplerDescriptor())
        val textureView = texture.createView()
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutDescriptor.Entry(
                            0, setOf(ShaderStage.fragment), TextureBindingLayout()
                        ),
                        BindGroupLayoutDescriptor.Entry(
                            1, setOf(ShaderStage.fragment),
                            SamplerBindingLayout()
                        )
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    bindGroupLayout,
                    listOf(
                        BindGroupDescriptor.BindGroupEntry(0, TextureViewBinding(textureView)),
                        BindGroupDescriptor.BindGroupEntry(1, SamplerBinding(sampler))
                    )
                )
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout)))
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                RenderPipelineDescriptor.VertexState(
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
                    targets = listOf(
                        FragmentState.ColorTargetState(
                            format = preferredFormat,
                            blend = BlendStates.NonPreMultiplied,
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
            setOf(TextureUsage.renderattachment),
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
                        setOf(TextureUsage.renderattachment),
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
                                clearValue = Color.DARK_GRAY.toWebGPUColor()
                            )
                        )
                    )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0, bindGroup)
            renderPassEncoder.setVertexBuffer(0, vbo)
            renderPassEncoder.setIndexBuffer(ibo, IndexFormat.uint16)
            renderPassEncoder.drawIndexed(indices.capacity, 1)
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
