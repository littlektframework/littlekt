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

        struct VertexInput {
            @location(0) position: float3,
        };

        struct VertexOutput {
            @builtin(position) position: float4,
            @location(0) world_pos: float3,
        };

        struct ViewParams {
            view_proj: mat4x4<f32>,
        };
        
        struct NodeParams {
            transform: mat4x4<f32>,
        };

        @group(0) @binding(0)
        var<uniform> view_params: ViewParams;
        @group(1) @binding(0)
        var<uniform> node_params: NodeParams;

        @vertex
        fn vs_main(vert: VertexInput) -> VertexOutput {
            var out: VertexOutput;
            out.position = view_params.view_proj * node_params.transform * float4(vert.position, 1.0);
            out.world_pos = vert.position.xyz;
            return out;
        };

        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) float4 {
            // Compute the normal by taking the cross product of the
            // dx & dy vectors computed through fragment derivatives
            let dx = dpdx(in.world_pos);
            let dy = dpdy(in.world_pos);
            let n = normalize(cross(dx, dy));
            return float4((n + 1.0) * 0.5, 1.0);
        }
    """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device
        val camera = PerspectiveCamera()
        camera.far = 1000f
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
        val model =
            resourcesVfs["2CylinderEngine.glb"].readGltf().toModel(device).apply {
                build(device, shader, vertexGroupLayout, preferredFormat, depthFormat)
            }

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
            depthTexture.release()
            depthTexture =
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
        }

        addWASDMovement(camera, 0.5f)
        addZoom(camera, 0.01f)
        onUpdate { dt ->
            model.rotate(y = 0.1.degrees * dt.milliseconds)
            model.update(device)
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
            val depthFrame = depthTexture.createView()

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

            model.render(renderPassEncoder, vertexBindGroup)
            renderPassEncoder.end()

            val commandBuffer = commandEncoder.finish()

            queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            renderPassEncoder.release()
            commandEncoder.release()
            frame.release()
            depthFrame.release()
            swapChainTexture.release()
        }

        onRelease { shader.release() }
    }
}
