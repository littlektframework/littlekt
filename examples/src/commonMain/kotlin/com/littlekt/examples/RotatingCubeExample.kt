package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4
import com.littlekt.math.geom.radians
import com.littlekt.util.now
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example rendering a rotating cube using pure WebGPU.
 *
 * @author Colton Daily
 * @date 4/6/2024
 */
class RotatingCubeExample(context: Context) : ContextListener(context) {

    private val shaderSrc =
        """
        struct Uniforms {
          modelViewProjectionMatrix : mat4x4f,
        }
        @binding(0) @group(0) var<uniform> uniforms : Uniforms;

        struct VertexOutput {
          @builtin(position) Position : vec4f,
          @location(0) fragUV : vec2f,
          @location(1) fragPosition: vec4f,
        }

        @vertex
        fn vs_main(
          @location(0) position : vec4f,
          @location(1) uv : vec2f
        ) -> VertexOutput {
          var output : VertexOutput;
          output.Position = uniforms.modelViewProjectionMatrix * position;
          output.fragUV = uv;
          output.fragPosition = 0.5 * (position + vec4(1.0, 1.0, 1.0, 1.0));
          return output;
        }
        
        @fragment
        fn fs_main(
          @location(0) fragUV: vec2f,
          @location(1) fragPosition: vec4f
        ) -> @location(0) vec4f {
          return fragPosition;
        }
    """
            .trimIndent()

    private val cubeVertexSize = 4 * 10L // byte size of one cube vertex
    private val cubePositionOffset = 0L
    private val cubeUVOffset = 4 * 8L
    private val cubeVertexCount = 36

    private val cubeVertexArray =
        floatArrayOf(
            // float4 position, float4 color, float2 uv,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            -1f,
            -1f,
            -1f,
            1f,
            0f,
            0f,
            0f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            0f,
            0f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            -1f,
            -1f,
            1f,
            0f,
            0f,
            0f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            0f,
            1f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            0f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            0f,
            -1f,
            1f,
            -1f,
            1f,
            0f,
            1f,
            0f,
            1f,
            0f,
            0f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            0f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            -1f,
            1f,
            -1f,
            1f,
            0f,
            1f,
            0f,
            1f,
            1f,
            0f,
            -1f,
            -1f,
            -1f,
            1f,
            0f,
            0f,
            0f,
            1f,
            0f,
            0f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            1f,
            -1f,
            1f,
            0f,
            1f,
            0f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
            0f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
            0f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            0f,
            1f,
            -1f,
            -1f,
            -1f,
            1f,
            0f,
            0f,
            0f,
            1f,
            1f,
            1f,
            -1f,
            1f,
            -1f,
            1f,
            0f,
            1f,
            0f,
            1f,
            1f,
            0f,
            1f,
            1f,
            -1f,
            1f,
            1f,
            1f,
            0f,
            1f,
            0f,
            0f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            0f,
            1f,
            -1f,
            1f,
            -1f,
            1f,
            0f,
            1f,
            0f,
            1f,
            1f,
            0f,
        )

    override suspend fun Context.start() {
        addStatsHandler()
        val device = graphics.device
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            AlphaMode.OPAQUE,
        )

        val projMatrix =
            Mat4().setToPerspective(45f, graphics.width / graphics.height.toFloat(), 1f, 100f)
        val viewMatrix = Mat4()
        val modelViewProjMatrix = Mat4()
        fun updateMvp() {
            val now = (now() / 1000f).toFloat()
            viewMatrix
                .setToIdentity()
                .translate(0f, 0f, -7f)
                .rotate(sin(now), cos(now), 0f, 1f.radians)
            modelViewProjMatrix.set(projMatrix).mul(viewMatrix)
        }

        val vertexBuffer =
            device.createGPUFloatBuffer("cube vbo", cubeVertexArray, BufferUsage.VERTEX)
        val matrixBuffer =
            device.createGPUFloatBuffer(
                "mvp buffer",
                modelViewProjMatrix.data,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )

        val shader = device.createShaderModule(shaderSrc)
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            )
        val bindGroup =
            device.createBindGroup(
                BindGroupDescriptor(bindGroupLayout, BindGroupEntry(0, BufferBinding(matrixBuffer)))
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(bindGroupLayout))
        val renderPipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = pipelineLayout,
                    vertex =
                        VertexState(
                            module = shader,
                            entryPoint = "vs_main",
                            buffer =
                                WebGPUVertexBufferLayout(
                                    arrayStride = cubeVertexSize,
                                    stepMode = VertexStepMode.VERTEX,
                                    attributes =
                                        listOf(
                                            WebGPUVertexAttribute(
                                                VertexFormat.FLOAT32x4,
                                                cubePositionOffset,
                                                0,
                                            ),
                                            WebGPUVertexAttribute(
                                                VertexFormat.FLOAT32x2,
                                                cubeUVOffset,
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
                                    preferredFormat,
                                    BlendState.Opaque,
                                    ColorWriteMask.ALL,
                                ),
                        ),
                    primitive =
                        PrimitiveState(PrimitiveTopology.TRIANGLE_LIST, cullMode = CullMode.BACK),
                    depthStencil =
                        DepthStencilState(
                            format = TextureFormat.DEPTH24_PLUS,
                            true,
                            CompareFunction.LESS,
                        ),
                    multisample = MultisampleState(1, 0xFFFFFFF, false),
                )
            )
        var depthTexture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(graphics.width, graphics.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    TextureFormat.DEPTH24_PLUS,
                    TextureUsage.RENDER_ATTACHMENT,
                )
            )
        var depthTextureView = depthTexture.createView()
        val queue = device.queue
        val matBuffer = FloatBuffer(16)
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            val valid =
                surfaceTexture.isValid(context) {
                    depthTexture.release()
                    depthTextureView.release()
                    depthTexture =
                        device.createTexture(
                            TextureDescriptor(
                                Extent3D(graphics.width, graphics.height, 1),
                                1,
                                1,
                                TextureDimension.D2,
                                TextureFormat.DEPTH24_PLUS,
                                TextureUsage.RENDER_ATTACHMENT,
                            )
                        )
                    depthTextureView = depthTexture.createView()
                    SurfaceConfiguration(
                        device,
                        TextureUsage.RENDER_ATTACHMENT,
                        preferredFormat,
                        PresentMode.FIFO,
                        AlphaMode.OPAQUE,
                        graphics.width,
                        graphics.height,
                    )
                }
            if (!valid) return@onUpdate
            updateMvp()
            queue.writeBuffer(matrixBuffer, modelViewProjMatrix.toBuffer(matBuffer))
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
                                    clearColor = Color.BLACK,
                                )
                            ),
                            depthStencilAttachment =
                                RenderPassDepthStencilAttachmentDescriptor(
                                    depthTextureView,
                                    depthClearValue = 1f,
                                    depthLoadOp = LoadOp.CLEAR,
                                    depthStoreOp = StoreOp.STORE,
                                ),
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0, bindGroup)
            renderPassEncoder.setVertexBuffer(0, vertexBuffer)
            renderPassEncoder.draw(cubeVertexCount, 1)
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
            bindGroup.release()
            bindGroupLayout.release()
            matrixBuffer.release()
            depthTexture.release()
            depthTextureView.release()
            shader.release()
        }
    }
}
