package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.BlendStates
import com.littlekt.graphics.Color
import com.littlekt.graphics.createGPUFloatBuffer
import com.littlekt.math.Mat4
import com.littlekt.math.geom.radians
import com.littlekt.util.now
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BufferUsage
import io.ygdrasil.webgpu.ColorWriteMask
import io.ygdrasil.webgpu.CompareFunction
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.CullMode
import io.ygdrasil.webgpu.LoadOp
import io.ygdrasil.webgpu.PipelineLayoutDescriptor
import io.ygdrasil.webgpu.PrimitiveTopology
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor.DepthStencilState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.FragmentState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.MultisampleState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.PrimitiveState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout
import io.ygdrasil.webgpu.RenderPipelineDescriptor.VertexState.VertexBufferLayout.VertexAttribute
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.ShaderStage
import io.ygdrasil.webgpu.Size3D
import io.ygdrasil.webgpu.StoreOp
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage
import io.ygdrasil.webgpu.VertexFormat
import io.ygdrasil.webgpu.VertexStepMode
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

    private val cubeVertexSize = 4u * 10uL // byte size of one cube vertex
    private val cubePositionOffset = 0uL
    private val cubeUVOffset = 4u * 8uL
    private val cubeVertexCount = 36u

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
        val preferredFormat = graphics.preferredFormat
        graphics.configureSurface(
            setOf(TextureUsage.RenderAttachment),
            preferredFormat,
            CompositeAlphaMode.Opaque
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
            device.createGPUFloatBuffer("cube vbo", cubeVertexArray, setOf(BufferUsage.Vertex))
        val matrixBuffer =
            device.createGPUFloatBuffer(
                "mvp buffer",
                modelViewProjMatrix.data,
                setOf(BufferUsage.Uniform, BufferUsage.CopyDst)
            )

        val shader = device.createShaderModule(ShaderModuleDescriptor(shaderSrc))
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutDescriptor.Entry(
                            0u, setOf(ShaderStage.Vertex),
                            BindGroupLayoutDescriptor.Entry.BufferBindingLayout()
                        )
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    bindGroupLayout, listOf(BindGroupDescriptor.BindGroupEntry(
                        0u,
                        BindGroupDescriptor.BufferBinding(matrixBuffer)
                    ))
                )
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout)))
        val renderPipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = pipelineLayout,
                    vertex =
                    VertexState(
                        module = shader,
                        entryPoint = "vs_main",
                        buffers = listOf(
                        VertexBufferLayout(
                            arrayStride = cubeVertexSize,
                            stepMode = VertexStepMode.Vertex,
                            attributes =
                            listOf(
                                VertexAttribute(
                                    VertexFormat.Float32x4,
                                    cubePositionOffset,
                                    0u
                                ),
                                VertexAttribute(
                                    VertexFormat.Float32x2,
                                    cubeUVOffset,
                                    1u
                                )
                            )
                        )
                        )
                    ),
                    fragment = FragmentState(
                        module = shader,
                        entryPoint = "fs_main",
                        targets = listOf(
                            FragmentState.ColorTargetState(
                                preferredFormat,
                                ColorWriteMask.All,
                                BlendStates.Opaque,
                            )
                        )

                    ),
                    primitive =
                    PrimitiveState(PrimitiveTopology.TriangleList, cullMode = CullMode.Back),
                    depthStencil =
                    DepthStencilState(
                        format = TextureFormat.Depth24Plus,
                        true,
                        CompareFunction.Less
                    ),
                    multisample = MultisampleState(1u, 268435455u, false)
                )
            )
        var depthTexture =
            device.createTexture(
                TextureDescriptor(
                    Size3D(graphics.width.toUInt(), graphics.height.toUInt()),
                    TextureFormat.Depth24Plus,
                    setOf(TextureUsage.RenderAttachment)
                )
            )
        var depthTextureView = depthTexture.createView()
        val queue = device.queue
        val matBuffer = FloatBuffer(16)
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            val valid =
                surfaceTexture.isValid(context) {
                    depthTexture.close()
                    depthTextureView.close()
                    depthTexture =
                        device.createTexture(
                            TextureDescriptor(
                                Size3D(graphics.width.toUInt(), graphics.height.toUInt()),
                                TextureFormat.Depth24Plus,
                                setOf(TextureUsage.RenderAttachment),
                            )
                        )
                    depthTextureView = depthTexture.createView()
                    SurfaceConfiguration(
                        device,
                        preferredFormat,
                        setOf(TextureUsage.RenderAttachment),
                        alphaMode = CompositeAlphaMode.Opaque
                    )
                }
            if (!valid) return@onUpdate
            updateMvp()
            queue.writeBuffer(matrixBuffer, 0uL, modelViewProjMatrix.toBuffer(matBuffer).toArray())
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
                                clearValue = Color.BLACK.toWebGPUColor()
                            )
                        ),
                        depthStencilAttachment = RenderPassDescriptor.DepthStencilAttachment(
                            depthTextureView,
                            depthClearValue = 1f,
                            depthLoadOp = LoadOp.Clear,
                            depthStoreOp = StoreOp.Store
                        )
                    )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0u, bindGroup)
            renderPassEncoder.setVertexBuffer(0u, vertexBuffer)
            renderPassEncoder.draw(cubeVertexCount, 1u)
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
            bindGroup.close()
            bindGroupLayout.close()
            matrixBuffer.close()
            depthTexture.close()
            depthTextureView.close()
            shader.close()
        }
    }
}
