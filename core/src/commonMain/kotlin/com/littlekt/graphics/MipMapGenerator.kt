package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.datastructure.internal.threadSafeMutableMapOf
import kotlin.math.max

/**
 * A helper class to generate mipmaps for textures. This class will cache [RenderPipeline] based on
 * the [TextureFormat] of the texture. Reuse the same instance of this class instead of making
 * multiple instances.
 *
 * @author Colton Daily
 * @date 1/20/2025
 */
class MipMapGenerator(private val device: Device) : Releasable {

    private val shader by lazy {
        device.createShaderModule(
            """
            struct VertexOutput {
                @location(0) uv: vec2<f32>,
                @builtin(position) position: vec4<f32>,
            };
 
            @vertex 
            fn vs_main(
              @builtin(vertex_index) vertexIndex : u32
            ) -> VertexOutput {
              var pos = array(
                // 1st triangle
                vec2f( 0.0,  0.0),  // center
                vec2f( 1.0,  0.0),  // right, center
                vec2f( 0.0,  1.0),  // center, top
 
                // 2nd triangle
                vec2f( 0.0,  1.0),  // center, top
                vec2f( 1.0,  0.0),  // right, center
                vec2f( 1.0,  1.0),  // right, top
              );
 
              var output: VertexOutput;
              let xy = pos[vertexIndex];
              output.position = vec4f(xy * 2.0 - 1.0, 0.0, 1.0);
              output.uv = vec2f(xy.x, 1.0 - xy.y);
              return output;
            }
 
            @group(0) @binding(0) var tex: texture_2d<f32>;
            @group(0) @binding(1) var samp: sampler;
 
            @fragment 
            fn fs_main(fsInput: VertexOutput) -> @location(0) vec4f {
              return textureSample(tex, samp, fsInput.uv);
            }
        """
                .trimIndent()
        )
    }
    private val sampler by lazy {
        device.createSampler(SamplerDescriptor(minFilter = FilterMode.LINEAR))
    }
    private val bindGroupLayout by lazy {
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                )
            )
        )
    }
    private val pipelineLayout by lazy {
        device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayouts = listOf(bindGroupLayout))
        )
    }

    private val renderPipelines = threadSafeMutableMapOf<TextureFormat, RenderPipeline>()

    /**
     * Generate the mips of the current [texture] and submit to the queue. Ensure that this is done
     * on the rendering thread otherwise we may run into deadlock issues with WGPU.
     */
    fun generateMips(texture: Texture) {
        val renderPipeline =
            renderPipelines.getOrPut(texture.textureDescriptor.format) {
                device.createRenderPipeline(
                    RenderPipelineDescriptor(
                        pipelineLayout,
                        vertex =
                            VertexState(
                                module = shader,
                                entryPoint = "vs_main",
                                WebGPUVertexBufferLayout(4, VertexStepMode.VERTEX, emptyList()),
                            ),
                        fragment =
                            FragmentState(
                                module = shader,
                                entryPoint = "fs_main",
                                target =
                                    ColorTargetState(
                                        format = texture.textureDescriptor.format,
                                        blendState = BlendState.NonPreMultiplied,
                                        writeMask = ColorWriteMask.ALL,
                                    ),
                            ),
                        primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                        label = "mip level generator pipeline",
                    )
                )
            }
        val commandEncoder = device.createCommandEncoder("mip gen encoder")
        var width = texture.size.width
        var height = texture.size.height
        var baseMipLevel = 0
        val views = mutableListOf<TextureView>()
        val mesh =
            mesh(
                device,
                listOf(VertexAttribute(VertexFormat.FLOAT32x2, 0, 0, VertexAttrUsage.UV)),
                6,
            ) {
                addVertex { uv.set(0f, 0f) }
                addVertex { uv.set(1f, 0f) }
                addVertex { uv.set(0f, 1f) }

                addVertex { uv.set(0f, 1f) }
                addVertex { uv.set(1f, 0f) }
                addVertex { uv.set(1f, 1f) }
            }
        while (width > 1 || height > 1) {
            width = max(1, width / 2)
            height = max(1, height / 2)

            val bindGroup =
                device.createBindGroup(
                    BindGroupDescriptor(
                        bindGroupLayout,
                        entries =
                            listOf(
                                BindGroupEntry(
                                    0,
                                    texture.gpuTexture
                                        .createView(
                                            desc =
                                                TextureViewDescriptor(
                                                    format = texture.textureDescriptor.format,
                                                    dimension = TextureViewDimension.D2,
                                                    baseMipLevel = baseMipLevel,
                                                    mipLevelCount = 1,
                                                )
                                        )
                                        .also { views += it },
                                ),
                                BindGroupEntry(1, sampler),
                            ),
                    )
                )
            baseMipLevel++
            val renderPassDescriptor =
                RenderPassDescriptor(
                    colorAttachments =
                        listOf(
                            RenderPassColorAttachmentDescriptor(
                                view =
                                    texture.gpuTexture
                                        .createView(
                                            desc =
                                                TextureViewDescriptor(
                                                    format = texture.textureDescriptor.format,
                                                    dimension = TextureViewDimension.D2,
                                                    baseMipLevel = baseMipLevel,
                                                    mipLevelCount = 1,
                                                )
                                        )
                                        .also { views += it },
                                loadOp = LoadOp.CLEAR,
                                storeOp = StoreOp.STORE,
                            )
                        )
                )
            val renderPassEncoder = commandEncoder.beginRenderPass(renderPassDescriptor)
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0, bindGroup)
            renderPassEncoder.setVertexBuffer(0, mesh.vbo)
            renderPassEncoder.draw(6, 1)
            renderPassEncoder.end()
            renderPassEncoder.release()
        }
        val buffer = commandEncoder.finish()
        device.queue.submit(buffer)
        commandEncoder.release()
        mesh.release()
        views.forEach { it.release() }
    }

    override fun release() {
        sampler.release()
        shader.release()
        bindGroupLayout.release()
        pipelineLayout.release()
        renderPipelines.values.forEach { it.release() }
    }
}
