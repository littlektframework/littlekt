package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.textureIndexedMesh
import com.littlekt.graphics.webgpu.*

/**
 * An example showing drawing a texture with a [textureIndexedMesh] and using an
 * [OrthographicCamera].
 *
 * @author Colton Daily
 * @date 4/20/2024
 */
class TextureMeshAndCameraExample(context: Context) : ContextListener(context) {
    // language=wgsl
    private val textureShader =
        """
            struct CameraUniform {
                view_proj: mat4x4<f32>
            };
            @group(0) @binding(0)
            var<uniform> camera: CameraUniform;
            
            struct VertexOutput {
                @location(0) uv: vec2<f32>,
                @builtin(position) position: vec4<f32>,
            };
            
            @vertex
            fn vs_main(
                @location(0) pos: vec3<f32>,
                @location(1) color: vec4<f32>,
                @location(2) uvs: vec2<f32>) -> VertexOutput {
                
                var output: VertexOutput;
                output.position = camera.view_proj * vec4<f32>(pos.x, pos.y, 0, 1);
                output.uv = uvs;
                
                return output;
            }
            
            @group(1) @binding(0)
            var my_texture: texture_2d<f32>;
            @group(1) @binding(1)
            var my_sampler: sampler;
            
            @fragment
            fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
                return textureSample(my_texture, my_sampler, in.uv);
            }
        """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val texture = resourcesVfs["logo.png"].readTexture()
        val imgWidth = texture.width.toFloat() * 0.5f
        val imgHeight = texture.height.toFloat() * 0.5f
        val mesh =
            textureIndexedMesh(device) {
                indicesAsQuad()
                addVertex { // top left
                    position.set(0f, imgHeight, 0f)
                    texCoords.set(0f, 0f)
                }
                addVertex { // top right
                    position.set(imgWidth, imgHeight, 0f)
                    texCoords.set(1f, 0f)
                }
                addVertex { // bottom right
                    position.set(imgWidth, 0f, 0f)
                    texCoords.set(1f, 1f)
                }
                addVertex { // bottom left
                    position.set(0f, 0f, 0f)
                    texCoords.set(0f, 1f)
                }
            }
        mesh.update()
        val camera = OrthographicCamera()
        camera.ortho(graphics.width, graphics.height)

        val cameraFloatBuffer = FloatBuffer(16)
        camera.viewProjection.toBuffer(cameraFloatBuffer)
        val cameraUniformBuffer =
            device.createGPUFloatBuffer(
                "camera uniform buffer",
                cameraFloatBuffer.toArray(),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )

        val shader = device.createShaderModule(textureShader)
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        val queue = device.queue

        val vertexGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout()))
                )
            )
        val vertexBindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    vertexGroupLayout,
                    listOf(BindGroupEntry(0, BufferBinding(cameraUniformBuffer))),
                )
            )
        val fragmentGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    )
                )
            )
        val fragmentBindGroup =
            device.createBindGroup(
                desc =
                    BindGroupDescriptor(
                        fragmentGroupLayout,
                        listOf(BindGroupEntry(0, texture.view), BindGroupEntry(1, texture.sampler)),
                    )
            )
        val pipelineLayout =
            device.createPipelineLayout(
                PipelineLayoutDescriptor(listOf(vertexGroupLayout, fragmentGroupLayout))
            )
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                    VertexState(
                        module = shader,
                        entryPoint = "vs_main",
                        mesh.geometry.layout.gpuVertexBufferLayout,
                    ),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = "fs_main",
                        target =
                            ColorTargetState(
                                format = preferredFormat,
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
        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        onResize { width, height ->
            camera.ortho(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        addWASDMovement(camera, 0.5f)
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
            camera.update()
            camera.viewProjection.toBuffer(cameraFloatBuffer)
            device.queue.writeBuffer(cameraUniformBuffer, cameraFloatBuffer)

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
                                    clearColor =
                                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                                        else Color.DARK_GRAY,
                                )
                            )
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0, vertexBindGroup)
            renderPassEncoder.setBindGroup(1, fragmentBindGroup)
            renderPassEncoder.setVertexBuffer(0, mesh.vbo)
            renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
            renderPassEncoder.drawIndexed(6, 1)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease {
            renderPipeline.release()
            pipelineLayout.release()
            fragmentBindGroup.release()
            fragmentGroupLayout.release()
            texture.release()
            mesh.release()
            texture.release()
            shader.release()
        }
    }
}
