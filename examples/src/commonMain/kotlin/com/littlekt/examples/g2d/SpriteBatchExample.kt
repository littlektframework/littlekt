package com.littlekt.examples.g2d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.examples.addWASDMovement
import com.littlekt.examples.colorAttachment
import com.littlekt.examples.configureSurfaceOnResize
import com.littlekt.examples.renderFrame
import com.littlekt.examples.surfaceColor
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.align
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example of drawing multiple textures with a [SpriteBatch] and a camera. Each column draws the
 * same textures: normally, with a different [BlendState], and with a custom [Shader]. Move the
 * camera with WASD.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SpriteBatchExample(context: Context) : ContextListener(context) {

    private class RedShader(device: Device) :
        Shader(
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
            bindGroupLayoutUsageLayout = listOf(BindingUsage.CAMERA, BindingUsage.TEXTURE),
            layout =
                mapOf(
                    BindingUsage.CAMERA to
                        BindGroupLayoutDescriptor(
                            listOf(
                                BindGroupLayoutEntry(
                                    0,
                                    ShaderStage.VERTEX,
                                    BufferBindingLayout(
                                        hasDynamicOffset = true,
                                        minBindingSize =
                                            (Float.SIZE_BYTES * 16)
                                                .align(
                                                    device.limits.minUniformBufferOffsetAlignment
                                                )
                                                .toLong(),
                                    ),
                                )
                            ),
                            label = "RedShader Camera BindGroupLayoutDescriptor",
                        ),
                    BindingUsage.TEXTURE to
                        BindGroupLayoutDescriptor(
                            listOf(
                                BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                                BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                            ),
                            label = "RedShader texture BindGroupLayoutDescriptor",
                        ),
                ),
        )

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val logoTexture = resourcesVfs["logo.png"].readTexture()
        val pikaTexture = resourcesVfs["pika.png"].readTexture()
        val redShader = RedShader(device)
        val batch = SpriteBatch(device, graphics, graphics.preferredFormat)
        val viewport = ExtendViewport(graphics.width, graphics.height)
        val camera = viewport.camera
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height -> viewport.update(width, height, true) }

        addWASDMovement(camera, 0.5f)
        onUpdate {
            renderFrame { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(listOf(colorAttachment(frame, clearColor)))
                    )
                camera.update()
                val column = viewport.virtualWidth / 3f
                val logoY = viewport.virtualHeight - logoTexture.height * 0.1f - 20f
                batch.begin()
                batch.draw(logoTexture, 20f, logoY, scaleX = 0.1f, scaleY = 0.1f)
                batch.draw(pikaTexture, 20f, 20f)

                batch.setBlendState(BlendState.Darken)
                batch.draw(logoTexture, column + 20f, logoY, scaleX = 0.1f, scaleY = 0.1f)
                batch.draw(pikaTexture, column + 20f, 20f)
                batch.swapToPreviousBlendState()

                batch.shader = redShader
                batch.draw(logoTexture, column * 2f + 20f, logoY, scaleX = 0.1f, scaleY = 0.1f)
                batch.draw(pikaTexture, column * 2f + 20f, 20f)
                batch.useDefaultShader()

                batch.flush(renderPass, camera.viewProjection)
                batch.end()
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            batch.release()
            redShader.release()
            logoTexture.release()
            pikaTexture.release()
        }
    }
}
