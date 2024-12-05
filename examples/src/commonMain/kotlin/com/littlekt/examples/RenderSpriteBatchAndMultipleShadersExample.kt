package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.shader.SpriteShader
import com.littlekt.graphics.webgpu.*

/**
 * An example showing a [SpriteBatch] drawing multiple textures and using different shaders to
 * perform multiple draw calls in a render pass.
 *
 * @author Colton Daily
 * @date 4/12/2024
 */
class RenderSpriteBatchAndMultipleShadersExample(context: Context) : ContextListener(context) {

    private class ColorShader(device: Device) :
        SpriteShader(
            device,
            src =
                """
        struct CameraUniform {
            view_proj: mat4x4<f32>
        };
        @group(0) @binding(0)
        var<uniform> camera: CameraUniform;
        
        struct VertexOutput {
            @location(0) color: vec4<f32>,
            @location(1) uv: vec2<f32>,
            @builtin(position) position: vec4<f32>,
        };
                   
        @vertex
        fn vs_main(
            @location(0) pos: vec3<f32>,
            @location(1) color: vec4<f32>,
            @location(2) uvs: vec2<f32>) -> VertexOutput {
            
            var output: VertexOutput;
            output.position = camera.view_proj * vec4<f32>(pos.x, pos.y, 0, 1);
            output.color = color;
            output.uv = uvs;
            
            return output;
        }
        
        @group(1) @binding(0)
        var my_texture: texture_2d<f32>;
        @group(1) @binding(1)
        var my_sampler: sampler;
        
        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
            return textureSample(my_texture, my_sampler, in.uv) * vec4<f32>(1, 0, 0, 1);
        }
        """,
            layout =
                listOf(
                    BindGroupLayoutDescriptor(
                        listOf(BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout()))
                    ),
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        )
                    ),
                ),
        ) {
        override fun MutableList<BindGroup>.createBindGroupsWithTexture(
            texture: Texture,
            data: Map<String, Any>,
        ) {
            add(
                device.createBindGroup(
                    BindGroupDescriptor(
                        layouts[0],
                        listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
                    )
                )
            )
            add(
                device.createBindGroup(
                    BindGroupDescriptor(
                        layouts[1],
                        listOf(BindGroupEntry(0, texture.view), BindGroupEntry(1, texture.sampler)),
                    )
                )
            )
        }

        override fun setBindGroups(
            encoder: RenderPassEncoder,
            bindGroups: List<BindGroup>,
            dynamicOffsets: List<Long>,
        ) {
            encoder.setBindGroup(0, bindGroups[0])
            encoder.setBindGroup(1, bindGroups[1])
        }
    }

    // language=wgsl

    override suspend fun Context.start() {
        addStatsHandler()
        val device = graphics.device
        val logoTexture = resourcesVfs["logo.png"].readTexture()
        val pikaTexture = resourcesVfs["pika.png"].readTexture()

        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val coloredShader = ColorShader(device)

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat)

        onResize { width, height ->
            batch.viewProjection =
                batch.viewProjection.setToOrthographic(
                    left = -width * 0.5f,
                    right = width * 0.5f,
                    bottom = -height * 0.5f,
                    top = height * 0.5f,
                    near = 0f,
                    far = 1f,
                )
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                TextureStatus.TIMEOUT,
                TextureStatus.OUTDATED,
                TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
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
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor = Color.DARK_GRAY.toLinear(),
                                )
                            )
                        )
                )
            batch.begin()
            batch.draw(pikaTexture, graphics.width * 0.5f - pikaTexture.width, 0f)
            batch.shader = coloredShader
            batch.draw(pikaTexture, 0f, 0f)
            batch.useDefaultShader()
            batch.draw(logoTexture, -graphics.width * 0.5f, 0f, scaleX = 0.1f, scaleY = 0.1f)
            batch.flush(renderPassEncoder)
            batch.end()
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease {
            batch.release()
            logoTexture.release()
        }
    }
}
