package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.createGPUFloatBuffer
import com.littlekt.graphics.textureIndexedMesh
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupDescriptor.BindGroupEntry
import io.ygdrasil.webgpu.BindGroupDescriptor.BufferBinding
import io.ygdrasil.webgpu.BindGroupDescriptor.SamplerBinding
import io.ygdrasil.webgpu.BindGroupDescriptor.TextureViewBinding
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry.BufferBindingLayout
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry.SamplerBindingLayout
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor.Entry.TextureBindingLayout
import io.ygdrasil.webgpu.BufferUsage
import io.ygdrasil.webgpu.ColorWriteMask
import io.ygdrasil.webgpu.IndexFormat
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.PipelineLayoutDescriptor
import io.ygdrasil.webgpu.PrimitiveTopology
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.MultisampleState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.PrimitiveState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.ShaderStage
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceTextureStatus
import io.ygdrasil.webgpu.TextureUsage

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
                setOf(BufferUsage.Uniform, BufferUsage.CopyDst)
            )

        val shader = device.createShaderModule(ShaderModuleDescriptor(textureShader))
        val preferredFormat = graphics.preferredFormat

        val queue = device.queue

        val vertexGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(Entry(0u, setOf(ShaderStage.Vertex), BufferBindingLayout()))
                )
            )
        val vertexBindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    vertexGroupLayout,
                    listOf(BindGroupEntry(0u, BufferBinding(cameraUniformBuffer))),
                )
            )
        val fragmentGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        Entry(0u, setOf(ShaderStage.Fragment), TextureBindingLayout()),
                        Entry(1u, setOf(ShaderStage.Fragment), SamplerBindingLayout())
                    )
                )
            )
        val fragmentBindGroup =
            device.createBindGroup(
                    BindGroupDescriptor(
                        fragmentGroupLayout,
                        listOf(
                            BindGroupEntry(0u, TextureViewBinding(texture.view)),
                            BindGroupEntry(1u, SamplerBinding(texture.sampler)))
                    )
            )
        val pipelineLayout =
            device.createPipelineLayout(
                PipelineLayoutDescriptor(listOf(vertexGroupLayout, fragmentGroupLayout))
            )
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = VertexState(
                    module = shader,
                    entryPoint = "vs_main",
                    buffers = listOf(mesh.geometry.layout.gpuVertexBufferLayout)
                ),
                fragment =
                FragmentState(
                    module = shader,
                    entryPoint = "fs_main",
                    targets =
                    listOf(
                        FragmentState.ColorTargetState(
                            format = preferredFormat,
                            blend = BlendStates.NonPreMultiplied,
                            writeMask = ColorWriteMask.All
                        )
                    )
                ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TriangleList),
                depthStencil = null,
                multisample = MultisampleState(count = 1u, mask = 0xFFFFFFFu, alphaToCoverageEnabled = false)
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)
        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
            graphics.surface.supportedAlphaMode.first()
        )

        onResize { width, height ->
            camera.ortho(width, height)
            graphics.configureSurface(
                setOf(TextureUsage.RenderAttachment),
                preferredFormat,
                graphics.surface.supportedAlphaMode.first()
            )
        }

        addWASDMovement(camera, 0.5f)
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
            device.queue.writeBuffer(cameraUniformBuffer, 0uL, cameraFloatBuffer.toArray())

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
                                    clearValue = Color.DARK_GRAY.toWebGPUColor()
                                )
                            )
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0u, vertexBindGroup)
            renderPassEncoder.setBindGroup(1u, fragmentBindGroup)
            renderPassEncoder.setVertexBuffer(0u, mesh.vbo)
            renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.Uint16)
            renderPassEncoder.drawIndexed(6u, 1u)
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
            fragmentBindGroup.close()
            fragmentGroupLayout.close()
            texture.release()
            mesh.release()
            texture.release()
            shader.close()
        }
    }
}
