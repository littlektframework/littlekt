package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*
import kotlin.math.log2
import kotlin.math.max
import kotlinx.atomicfu.atomic

/**
 * Creates a [WebGPUTexture] and writes it to the [Device.queue]. Creates a [TextureView] and
 * [Sampler].
 *
 * @author Colton Daily
 * @date 4/9/2024
 */
interface Texture : Releasable {

    /**
     * The [Extent3D] size of the texture. Usually, the width & height of the image with a depth of
     * `1`.
     */
    val size: Extent3D

    /** The width of the texture. */
    val width: Int
        get() = size.width

    /** The height of the texture. */
    val height: Int
        get() = size.height

    /** The id of the texture. */
    val id: Int

    /**
     * The [TextureDescriptor] used in [gpuTexture]. Updating this will recreate the [gpuTexture]
     * and [view]. Any bind group entries will need recreated!
     */
    var textureDescriptor: TextureDescriptor

    /** The underlying [WebGPUTexture]. Uses [textureDescriptor] in creation. */
    val gpuTexture: WebGPUTexture

    /**
     * The [TextureViewDescriptor] used [view]. Updating this will recreate the [TextureView]. Any
     * bind group entries will need recreated!
     */
    var textureViewDescriptor: TextureViewDescriptor?

    /** The underlying [TextureView]. Uses [textureViewDescriptor] in creation. */
    val view: TextureView

    /**
     * The [SamplerDescriptor] used in [sampler]. Updating this will recreate the sampler. Any bind
     * group entries will need recreated!
     */
    var samplerDescriptor: SamplerDescriptor

    /** The underlying [Sampler]. Uses [samplerDescriptor] in creation. */
    val sampler: Sampler

    /** Write this [Texture] to the GPU buffer. */
    fun writeDataToBuffer()

    /**
     * Generate mipmaps for this texture. This is usually called in the `init` of a texture but may
     * be need to be manually called for custom [Texture] types.
     */
    fun generateMipMaps(device: Device) {
        val shader =
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
        val sampler = device.createSampler(SamplerDescriptor(minFilter = FilterMode.LINEAR))
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    )
                )
            )
        val pipelineLayout =
            device.createPipelineLayout(
                PipelineLayoutDescriptor(bindGroupLayouts = listOf(bindGroupLayout))
            )
        val renderPipeline =
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
                                    format = textureDescriptor.format,
                                    blendState = BlendState.NonPreMultiplied,
                                    writeMask = ColorWriteMask.ALL,
                                ),
                        ),
                    primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                    label = "mip level generator pipeline",
                )
            )
        val commandEncoder = device.createCommandEncoder("mip gen encoder")
        var width = size.width
        var height = size.height
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
                                    gpuTexture
                                        .createView(
                                            desc =
                                                TextureViewDescriptor(
                                                    format = textureDescriptor.format,
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
                                    gpuTexture
                                        .createView(
                                            desc =
                                                TextureViewDescriptor(
                                                    format = textureDescriptor.format,
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
        views.forEach { it.release() }
        sampler.release()
        shader.release()
    }

    override fun release() {
        view.release()
        sampler.release()
        // destroy after any update/postUpdate calls to ensure we aren't in the middle of a pass!
        gpuTexture.release()
    }

    companion object {
        private var lastId by atomic(0)

        fun nextId() = lastId++

        /** @return the number of mips to generate based on the given sizes. */
        fun calculateNumMips(vararg sizes: Int): Int {
            val maxSize = sizes.maxOrNull() ?: return 1
            return 1 + log2(maxSize.toDouble()).toInt()
        }
    }
}
