package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.*
import com.littlekt.graphics.shader.Shader
import io.ygdrasil.wgpu.BindGroupDescriptor
import io.ygdrasil.wgpu.BindGroupDescriptor.BindGroupEntry
import io.ygdrasil.wgpu.BindGroupDescriptor.BufferBinding
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.Entry
import io.ygdrasil.wgpu.BindGroupLayoutDescriptor.Entry.BufferBindingLayout
import io.ygdrasil.wgpu.BufferBindingType
import io.ygdrasil.wgpu.BufferDescriptor
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.ColorWriteMask
import io.ygdrasil.wgpu.CommandEncoderDescriptor
import io.ygdrasil.wgpu.ComputePipelineDescriptor
import io.ygdrasil.wgpu.ComputePipelineDescriptor.ProgrammableStage
import io.ygdrasil.wgpu.LoadOp
import io.ygdrasil.wgpu.PipelineLayoutDescriptor
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.RenderPassDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor
import io.ygdrasil.wgpu.RenderPipelineDescriptor.VertexState
import io.ygdrasil.wgpu.ShaderModuleDescriptor
import io.ygdrasil.wgpu.ShaderStage
import io.ygdrasil.wgpu.StoreOp
import io.ygdrasil.wgpu.SurfaceTextureStatus
import io.ygdrasil.wgpu.TextureUsage
import io.ygdrasil.wgpu.VertexFormat
import io.ygdrasil.wgpu.VertexStepMode
import kotlin.math.ceil
import kotlin.random.Random

/**
 * An example using a [ComputePassEncoder]
 *
 * @author Colton Daily
 * @date 5/14/2024
 */
class ComputeBoidsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        val device = graphics.device

        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            setOf(TextureUsage.renderattachment),
            preferredFormat,
            PresentMode.fifo,
            graphics.surface.supportedAlphaMode.first()
        )

        val spriteShader = Shader(device, SPRITE_WGSL_SRC, emptyList())
        val renderPipeline =
            device.createRenderPipeline(
                    RenderPipelineDescriptor(
                        layout = spriteShader.pipelineLayout,
                        vertex =
                        VertexState(
                            module = spriteShader.shaderModule,
                            entryPoint = spriteShader.vertexEntryPoint,
                            buffers =
                            listOf(
                                VertexBufferLayoutView(
                                    arrayStride = 4 * 4,
                                    stepMode = VertexStepMode.instance,
                                    attributes =
                                    listOf(
                                        VertexAttributeView(
                                            format = VertexFormat.float32x2,
                                            offset = 0L,
                                            shaderLocation = 0,
                                            usage = VertexAttrUsage.POSITION
                                        ),
                                        VertexAttributeView(
                                            format = VertexFormat.float32x2,
                                            offset =
                                            VertexFormat.float32x2.sizeInByte
                                                .toLong(),
                                            shaderLocation = 1,
                                            usage = VertexAttrUsage.GENERIC
                                        ),
                                    )
                                )
                                    .gpuVertexBufferLayout,
                                VertexBufferLayoutView(
                                    arrayStride = 2 * 4,
                                    stepMode = VertexStepMode.vertex,
                                    attributes =
                                    listOf(
                                        VertexAttributeView(
                                            format = VertexFormat.float32x2,
                                            offset = 0,
                                            shaderLocation = 2,
                                            usage = VertexAttrUsage.GENERIC
                                        ),
                                    )
                                )
                                    .gpuVertexBufferLayout
                            )
                        ),
                        fragment =
                        RenderPipelineDescriptor.FragmentState(
                            module = spriteShader.shaderModule,
                            entryPoint = spriteShader.fragmentEntryPoint,
                            targets = listOf(
                                RenderPipelineDescriptor.FragmentState.ColorTargetState(
                                    format = preferredFormat,
                                    blend = BlendStates.NonPreMultiplied,
                                    writeMask = ColorWriteMask.all
                                )
                            )
                        )
                    )
            )
        val computeBindGroupLayoutDesc =
            BindGroupLayoutDescriptor(
                listOf(
                    Entry(0, setOf(ShaderStage.compute),
                        BufferBindingLayout()
                    ),
                    Entry(
                        1,
                        setOf(ShaderStage.compute),
                        BufferBindingLayout(BufferBindingType.readonlystorage)
                    ),
                    Entry(
                        2,
                        setOf(ShaderStage.compute),
                        BufferBindingLayout(BufferBindingType.storage)
                    )
                ),
                "compute bind group"
            )
        val computeBindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        Entry(0, setOf(ShaderStage.compute), BufferBindingLayout()),
                        Entry(
                            1,
                            setOf(ShaderStage.compute),
                            BufferBindingLayout(BufferBindingType.readonlystorage)
                        ),
                        Entry(
                            2,
                            setOf(ShaderStage.compute),
                            BufferBindingLayout(BufferBindingType.storage)
                        )
                    ),
                    "compute bind group"
                )
            )
        val computePipelineLayout =
            device.createPipelineLayout(
                PipelineLayoutDescriptor(listOf(computeBindGroupLayout), "compute pipeline layout")
            )
        val computePipeline =
            device.createComputePipeline(
                    ComputePipelineDescriptor(
                        layout = computePipelineLayout,
                        compute =
                        ProgrammableStage(
                            module = device.createShaderModule(ShaderModuleDescriptor(UPDATE_SPRITES_WGSL_SRC)),
                            entryPoint = "main"
                        )
                    )
            )

        val vertexBufferData = floatArrayOf(-0.01f, -0.02f, 0.01f, -0.02f, 0f, 0.02f)
        val spriteVertexBuffer =
            device.createGPUFloatBuffer(
                "sprite vertex buffer",
                vertexBufferData,
                setOf( BufferUsage.vertex)
            )
        val simParams =
            SimParams(
                deltaT = 0.04f,
                rule1Distance = 0.1f,
                rule2Distance = 0.025f,
                rule3Distance = 0.025f,
                rule1Scale = 0.02f,
                rule2Scale = 0.05f,
                rule3Scale = 0.005f
            )

        val simParamBufferSize = 7L * Float.SIZE_BYTES
        val simParamBuffer =
            device.createBuffer(
                BufferDescriptor(
                    label = "sim param buffer",
                    size = simParamBufferSize,
                    usage = setOf(BufferUsage.uniform, BufferUsage.copydst),
                    mappedAtCreation = false
                )
            )
        val simParamBufferData = FloatBuffer(7)
        simParamBufferData.put(
            floatArrayOf(
                simParams.deltaT,
                simParams.rule1Distance,
                simParams.rule2Distance,
                simParams.rule3Distance,
                simParams.rule1Scale,
                simParams.rule2Scale,
                simParams.rule3Scale
            )
        )
        device.queue.writeBuffer(simParamBuffer, 0L, simParamBufferData.toArray())

        val numParticles = 1500
        val initialParticleData = FloatArray(numParticles * 4)
        repeat(numParticles) { i ->
            initialParticleData[4 * i + 0] = 2 * (Random.nextFloat() - 0.5f)
            initialParticleData[4 * i + 1] = 2 * (Random.nextFloat() - 0.5f)
            initialParticleData[4 * i + 2] = 2 * (Random.nextFloat() - 0.5f) * 0.1f
            initialParticleData[4 * i + 3] = 2 * (Random.nextFloat() - 0.5f) * 0.1f
        }

        val particleBuffers =
            Array(2) {
                device.createGPUFloatBuffer(
                    "particle buffer $it",
                    initialParticleData,
                    setOf(BufferUsage.vertex, BufferUsage.storage)
                )
            }

        val particleBindGroups =
            Array(2) { i ->
                device.createBindGroup(
                    BindGroupDescriptor(
                        computeBindGroupLayout,
                        listOf(
                            BindGroupEntry(0, BufferBinding(simParamBuffer)),
                            BindGroupEntry(
                                1,
                                BufferBinding(particleBuffers[i], 0, initialParticleData.size * 4L)
                            ),
                            BindGroupEntry(
                                2,
                                BufferBinding(
                                    particleBuffers[(i + 1) % 2],
                                    0,
                                    initialParticleData.size * 4L
                                )
                            ),
                        )
                    )
                )
            }

        onResize { width, height ->
            graphics.configureSurface(
                setOf(TextureUsage.renderattachment),
                preferredFormat,
                PresentMode.fifo,
                graphics.surface.supportedAlphaMode.first()
            )
        }

        var fidx = 0
        onUpdate { dt ->
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
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder(CommandEncoderDescriptor("scenegraph command encoder"))
            val renderPassDescriptor =
                RenderPassDescriptor(
                    listOf(
                        RenderPassDescriptor.ColorAttachment(
                            view = frame,
                            loadOp = LoadOp.clear,
                            storeOp = StoreOp.store,
                            clearValue = Color.DARK_GRAY.toWebGPUColor()
                        )
                    ),
                    label = "Init render pass"
                )

            run computePass@{
                val passEncoder = commandEncoder.beginComputePass()
                passEncoder.setPipeline(computePipeline)
                passEncoder.setBindGroup(0, particleBindGroups[fidx % 2])
                passEncoder.dispatchWorkgroups(ceil(numParticles / 64f).toInt())
                passEncoder.end()
                passEncoder.close()
            }

            run renderPass@{
                val passEncoder = commandEncoder.beginRenderPass(renderPassDescriptor)
                passEncoder.setPipeline(renderPipeline)
                passEncoder.setVertexBuffer(0, particleBuffers[(fidx + 1) % 2])
                passEncoder.setVertexBuffer(1, spriteVertexBuffer)
                passEncoder.draw(3, numParticles, 0, 0)
                passEncoder.end()
            }

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(listOf(commandBuffer))
            graphics.surface.present()

            fidx++

            commandBuffer.close()
            commandEncoder.close()
            frame.close()
            swapChainTexture.close()
        }

        onRelease {}
    }

    private data class SimParams(
        var deltaT: Float,
        var rule1Distance: Float,
        var rule2Distance: Float,
        var rule3Distance: Float,
        var rule1Scale: Float,
        var rule2Scale: Float,
        var rule3Scale: Float
    )
}

private const val SPRITE_WGSL_SRC =
    // language=wgsl
    """
    struct VertexOutput {
      @builtin(position) position : vec4f,
      @location(4) color : vec4f,
    }
    
    @vertex
    fn vs_main(
      @location(0) a_particlePos : vec2f,
      @location(1) a_particleVel : vec2f,
      @location(2) a_pos : vec2f
    ) -> VertexOutput {
      let angle = -atan2(a_particleVel.x, a_particleVel.y);
      let pos = vec2(
        (a_pos.x * cos(angle)) - (a_pos.y * sin(angle)),
        (a_pos.x * sin(angle)) + (a_pos.y * cos(angle))
      );
      
      var output : VertexOutput;
      output.position = vec4(pos + a_particlePos, 0.0, 1.0);
      output.color = vec4(
        1.0 - sin(angle + 1.0) - a_particleVel.y,
        pos.x * 100.0 - a_particleVel.y + 0.1,
        a_particleVel.x + cos(angle + 0.5),
        1.0);
      return output;
    }
    
    @fragment
    fn fs_main(@location(4) color : vec4f) -> @location(0) vec4f {
      return color;
    }

"""

private const val UPDATE_SPRITES_WGSL_SRC =
    // language=wgsl
    """
    struct Particle {
      pos : vec2f,
      vel : vec2f,
    }
    struct SimParams {
      deltaT : f32,
      rule1Distance : f32,
      rule2Distance : f32,
      rule3Distance : f32,
      rule1Scale : f32,
      rule2Scale : f32,
      rule3Scale : f32,
    }
    struct Particles {
      particles : array<Particle>,
    }
    @binding(0) @group(0) var<uniform> params : SimParams;
    @binding(1) @group(0) var<storage, read> particlesA : Particles;
    @binding(2) @group(0) var<storage, read_write> particlesB : Particles;
    
    // https://github.com/austinEng/Project6-Vulkan-Flocking/blob/master/data/shaders/computeparticles/particle.comp
    @compute @workgroup_size(64)
    fn main(@builtin(global_invocation_id) GlobalInvocationID : vec3u) {
      var index = GlobalInvocationID.x;
    
      var vPos = particlesA.particles[index].pos;
      var vVel = particlesA.particles[index].vel;
      var cMass = vec2(0.0);
      var cVel = vec2(0.0);
      var colVel = vec2(0.0);
      var cMassCount = 0u;
      var cVelCount = 0u;
      var pos : vec2f;
      var vel : vec2f;
    
      for (var i = 0u; i < arrayLength(&particlesA.particles); i++) {
        if (i == index) {
          continue;
        }
    
        pos = particlesA.particles[i].pos.xy;
        vel = particlesA.particles[i].vel.xy;
        if (distance(pos, vPos) < params.rule1Distance) {
          cMass += pos;
          cMassCount++;
        }
        if (distance(pos, vPos) < params.rule2Distance) {
          colVel -= pos - vPos;
        }
        if (distance(pos, vPos) < params.rule3Distance) {
          cVel += vel;
          cVelCount++;
        }
      }
      if (cMassCount > 0) {
        cMass = (cMass / vec2(f32(cMassCount))) - vPos;
      }
      if (cVelCount > 0) {
        cVel /= f32(cVelCount);
      }
      vVel += (cMass * params.rule1Scale) + (colVel * params.rule2Scale) + (cVel * params.rule3Scale);
    
      // clamp velocity for a more pleasing simulation
      vVel = normalize(vVel) * clamp(length(vVel), 0.0, 0.1);
      // kinematic update
      vPos = vPos + (vVel * params.deltaT);
      // Wrap around boundary
      if (vPos.x < -1.0) {
        vPos.x = 1.0;
      }
      if (vPos.x > 1.0) {
        vPos.x = -1.0;
      }
      if (vPos.y < -1.0) {
        vPos.y = 1.0;
      }
      if (vPos.y > 1.0) {
        vPos.y = -1.0;
      }
      // Write back
      particlesB.particles[index].pos = vPos;
      particlesB.particles[index].vel = vVel;
    }
"""
