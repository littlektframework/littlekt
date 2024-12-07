package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.file.gltf.toModel
import com.littlekt.file.vfs.readGltf
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.geom.degrees
import com.littlekt.util.milliseconds

/**
 * An example using a simple Orthographic camera to move around a texture.
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class SimpleGltfExample(context: Context) : ContextListener(context) {

    // language=wgsl
    private val shaderSrc =
        """
        alias float4 = vec4<f32>;
        alias float3 = vec3<f32>;
        alias float2 = vec2<f32>;

        struct VertexInput {
            @location(0) position: float3,
            @location(1) texcoords: float2,
        };
        
        struct VertexOutput {
            @builtin(position) position: float4,
            @location(0) world_pos: float3,
            @location(1) texcoords: float2,
        };
        
        struct MaterialParams {
            base_color_factor: float4,
            metallic_factor: f32,
            roughness_factor: f32,
        };

        struct CameraUniform {
            view_proj: mat4x4<f32>,
        };
        
        struct NodeParams {
            transform: mat4x4<f32>,
        };

        @group(0) @binding(0)
        var<uniform> camera: CameraUniform;
        
        @group(1) @binding(0)
        var<uniform> node_params: NodeParams;
        
        @group(2) @binding(0)
        var<uniform> material_params: MaterialParams;
        
        @group(2) @binding(1)
        var base_color_sampler: sampler;
        
        @group(2) @binding(2)
        var base_color_texture: texture_2d<f32>;
        
        fn linear_to_srgb(x: f32) -> f32 {
            if (x <= 0.0031308) {
                return 12.92 * x;
            }
            return 1.055 * pow(x, 1.0 / 2.4) - 0.055;
        }

        @vertex
        fn vs_main(vert: VertexInput) -> VertexOutput {
            var out: VertexOutput;
            out.position = camera.view_proj * node_params.transform * float4(vert.position, 1.0);
            out.world_pos = vert.position.xyz;
            out.texcoords = vert.texcoords;
            return out;
        };

        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) float4 {
            let dx = dpdx(in.world_pos);
            let dy = dpdy(in.world_pos);
            let n = normalize(cross(dx, dy));
            let base_color = textureSample(base_color_texture, base_color_sampler, in.texcoords);
            var color = material_params.base_color_factor * base_color;
        
            color.x = linear_to_srgb(color.x);
            color.y = linear_to_srgb(color.y);
            color.z = linear_to_srgb(color.z);
            color.w = 1.0;
            return color;
        }
    """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val camera = PerspectiveCamera()
        camera.translate(0f, 25f, 150f)

        val cameraFloatBuffer = FloatBuffer(16)
        camera.viewProjection.toBuffer(cameraFloatBuffer)
        val cameraUniformBuffer =
            device.createGPUFloatBuffer(
                "camera uniform buffer",
                cameraFloatBuffer.toArray(),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )

        val shader = device.createShaderModule(shaderSrc)
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

        val depthFormat = TextureFormat.DEPTH24_PLUS_STENCIL8
        var depthTexture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(graphics.width, graphics.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    depthFormat,
                    TextureUsage.RENDER_ATTACHMENT,
                )
            )
        var depthFrame = depthTexture.createView()
        val models =
            listOf(
                resourcesVfs["models/Duck.glb"].readGltf().toModel().apply {
                    scale(20f)
                    translate(-30f, 0f, 0f)
                },
                resourcesVfs["models/Fox.glb"].readGltf().toModel().apply {
                    translate(30f, 0f, 0f)
                },
                resourcesVfs["models/flighthelmet/FlightHelmet.gltf"].readGltf().toModel().apply {
                    scale(200f)
                    translate(90f, 0f, 0f)
                },
            )
        models.forEach { it.build(device, shader, vertexGroupLayout, preferredFormat, depthFormat) }

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        onResize { width, height ->
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
            depthFrame.release()
            depthTexture.release()
            depthTexture =
                device.createTexture(
                    TextureDescriptor(
                        Extent3D(width, height, 1),
                        1,
                        1,
                        TextureDimension.D2,
                        depthFormat,
                        TextureUsage.RENDER_ATTACHMENT,
                    )
                )
            depthFrame = depthTexture.createView()
        }

        addWASDMovement(camera, 0.5f)
        addZoom(camera, 0.01f)
        onUpdate { dt ->
            models.forEach { model ->
                model.rotate(y = 0.1.degrees * dt.milliseconds)
                model.update(device)
            }
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
                            ),
                            depthStencilAttachment =
                                RenderPassDepthStencilAttachmentDescriptor(
                                    view = depthFrame,
                                    depthClearValue = 1f,
                                    depthLoadOp = LoadOp.CLEAR,
                                    depthStoreOp = StoreOp.STORE,
                                    stencilClearValue = 0,
                                    stencilLoadOp = LoadOp.CLEAR,
                                    stencilStoreOp = StoreOp.STORE,
                                ),
                        )
                )

            models.forEach { model -> model.render(renderPassEncoder, vertexBindGroup) }
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

        onRelease { shader.release() }
    }
}
