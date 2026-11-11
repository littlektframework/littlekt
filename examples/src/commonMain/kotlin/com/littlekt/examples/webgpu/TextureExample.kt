package com.littlekt.examples.webgpu

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*
import com.littlekt.renderFrame
import com.littlekt.surfaceColor

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
        configureSurfaceOnResize()
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
        val image = resourcesVfs["logo.png"].readPixmap()
        val device = graphics.device
        val vbo = device.createGPUFloatBuffer("vbo", vertices, BufferUsage.VERTEX)
        val ibo = device.createGPUShortBuffer("ibo", indices, BufferUsage.INDEX)
        val shader = device.createShaderModule(textureShader)
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(image.width, image.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    graphics.textureFormat,
                    TextureUsage.COPY_DST or TextureUsage.TEXTURE,
                )
            )

        device.queue.writeTexture(
            data = image.pixels.toArray(),
            destination = TextureCopyView(texture),
            layout = TextureDataLayout(image.width * 4, image.height),
            copySize = Extent3D(image.width, image.height, 1),
        )

        val sampler = device.createSampler(SamplerDescriptor())
        val textureView = texture.createView()
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                desc =
                    BindGroupDescriptor(
                        bindGroupLayout,
                        listOf(BindGroupEntry(0, textureView), BindGroupEntry(1, sampler)),
                    )
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(bindGroupLayout))
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                    VertexState(
                        module = shader,
                        entryPoint = "vs_main",
                        WebGPUVertexBufferLayout(
                            4L * Float.SIZE_BYTES,
                            VertexStepMode.VERTEX,
                            listOf(
                                WebGPUVertexAttribute(VertexFormat.FLOAT32x2, 0, 0),
                                WebGPUVertexAttribute(
                                    VertexFormat.FLOAT32x2,
                                    2L * Float.SIZE_BYTES,
                                    1,
                                ),
                            ),
                        ),
                    ),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = "fs_main",
                        target =
                            ColorTargetState(
                                format = graphics.preferredFormat,
                                blendState = BlendState.NonPreMultiplied,
                                writeMask = ColorWriteMask.ALL,
                            ),
                    ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                depthStencil = null,
                multisample =
                    MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false),
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPassEncoder =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame, clearColor))
                    )
                renderPassEncoder.setPipeline(renderPipeline)
                renderPassEncoder.setBindGroup(0, bindGroup)
                renderPassEncoder.setVertexBuffer(0, vbo)
                renderPassEncoder.setIndexBuffer(ibo, IndexFormat.UINT16)
                renderPassEncoder.drawIndexed(indices.size, 1)
                renderPassEncoder.end()
                renderPassEncoder.release()
            }
        }

        onRelease {
            renderPipeline.release()
            pipelineLayout.release()
            bindGroup.release()
            bindGroupLayout.release()
            sampler.release()
            textureView.release()
            texture.release()
            ibo.release()
            vbo.release()
            shader.release()
        }
    }
}
