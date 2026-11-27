package com.littlekt.graphics.g3d.shadow

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.light.SpotLight
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.util.datastructure.fastForEach
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlin.time.Duration

/**
 * A shadow map for up to [count] spot lights. Each [SpotLight] that [SpotLight.castShadows] gets
 * its own layer of the shadow map, rendered from the light's point of view using its camera in
 * [cameras] & [environment]. Lights past [count] still shine but don't cast shadows.
 *
 * Usage each frame:
 * ```
 * spotShadowMap.update(environment.spotLights)
 * spotShadowMap.render(commandEncoder, shadowBatch, nodes, dt)
 * ```
 *
 * @param count the max number of spot lights that cast shadows at once.
 * @param size the width & height of each light's layer.
 * @param format the depth format of the shadow map texture.
 * @author Colton Daily
 * @date 11/27/2026
 */
class SpotShadowMap(
    val device: Device,
    val count: Int = 4,
    val size: Int = 1024,
    val format: TextureFormat = TextureFormat.DEPTH32_FLOAT,
) : Releasable {

    init {
        require(count >= 1) { "SpotShadowMap.count must be at least 1!" }
    }

    /** The cameras used to render each light's layer from the light's point of view. */
    val cameras: List<PerspectiveCamera> =
        List(count) {
            PerspectiveCamera(size.toFloat(), size.toFloat()).apply {
                near = 0.1f
                far = 10f
            }
        }

    /** The [Environment] to render the shadow casters with. */
    val environment: Environment =
        UnlitEnvironment(device).apply { maxViewsPerFrame = max(maxViewsPerFrame, count) }

    /** The near plane of each light's camera. Casters closer to the light than this are skipped. */
    var near: Float = 0.1f

    /** The extra angle added to each light's cone so the edge of the cone isn't clipped. */
    var fovPadding: Angle = 4.degrees

    /** Offsets the sampled position towards the light, in texels, to reduce acne. */
    var depthBias: Float = 1f

    /** Offsets the sampled position along the surface normal, in texels, to reduce acne. */
    var normalOffset: Float = 1.5f

    /**
     * The radius, in texels, of the PCF filter used to soften the shadow edges. `0` takes a single
     * sample, `1` takes `3x3` samples, `2` takes `5x5` samples, and so on.
     */
    var pcfRadius: Int = 1

    /** The number of lights that were given a layer in the last [update]. */
    var activeCount: Int = 0
        private set

    private val lights = arrayOfNulls<SpotLight>(count)

    private val texture =
        device.createTexture(
            TextureDescriptor(
                Extent3D(size, size, count),
                1,
                1,
                TextureDimension.D2,
                format,
                TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                "spot shadow map",
            )
        )

    /** The array view of all the layers of the shadow map depth texture. */
    val view: TextureView =
        texture.createView(
            TextureViewDescriptor(
                format,
                TextureViewDimension.D2_ARRAY,
                TextureAspect.DEPTH_ONLY,
                arrayLayerCount = count,
            )
        )

    private val layerViews =
        List(count) {
            texture.createView(
                TextureViewDescriptor(
                    format,
                    TextureViewDimension.D2,
                    TextureAspect.DEPTH_ONLY,
                    baseArrayLayer = it,
                )
            )
        }

    /** The comparison sampler used to sample [view]. */
    val sampler: Sampler =
        device.createSampler(
            SamplerDescriptor(
                compare = CompareFunction.LESS_EQUAL,
                magFilter = FilterMode.LINEAR,
                minFilter = FilterMode.LINEAR,
                label = "spot shadow map sampler",
            )
        )

    private val uniformData = FloatBuffer(uniformSize(count))
    private val uniformBuffer =
        device.createGPUFloatBuffer(
            "spot shadow map uniform",
            uniformData,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] holding each light's matrix & shadow params. */
    val uniformBufferBinding = BufferBinding(uniformBuffer)

    private val renderPassDescriptors =
        List(count) {
            RenderPassDescriptor(
                emptyList(),
                depthStencilAttachment =
                    RenderPassDepthStencilAttachmentDescriptor(
                        view = layerViews[it],
                        depthClearValue = 1f,
                        depthLoadOp = LoadOp.CLEAR,
                        depthStoreOp = StoreOp.STORE,
                    ),
                label = "Spot Shadow Map Layer $it Render Pass",
            )
        }

    /**
     * Gives a layer to each of the [lights] that [SpotLight.castShadows], in order, until all
     * [count] layers are taken, then updates their cameras & the shadow uniform. Call this after the
     * lights moved and before [render].
     */
    fun update(lights: List<SpotLight>) {
        activeCount = 0
        lights.fastForEach { light ->
            if (activeCount < count && light.castShadows && light.intensity > 0f) {
                this.lights[activeCount++] = light
            }
        }
        for (i in activeCount until count) {
            this.lights[i] = null
        }

        for (i in 0 until activeCount) {
            val light = this.lights[i]!!
            val camera = cameras[i]
            val fov = min(light.outerAngle.radians * 2f + fovPadding.radians, MAX_FOV)
            camera.position.set(light.position)
            camera.direction.set(light.direction).norm()
            if (abs(camera.direction.y) > 0.99f) camera.up.set(Vec3f.Z_AXIS)
            else camera.up.set(Vec3f.Y_AXIS)
            camera.fov = Angle(fov)
            camera.virtualWidth = size.toFloat()
            camera.virtualHeight = size.toFloat()
            camera.near = near
            camera.far = max(light.range, near + 0.01f)
            camera.update()

            uniformData.put(camera.viewProjection.data, dstOffset = i * 16)
            val texelPerDistance = 2f * tan(fov * 0.5f) / size
            val offset = count * 16 + i * 4
            uniformData[offset] = texelPerDistance * normalOffset
            uniformData[offset + 1] = texelPerDistance * depthBias
            uniformData[offset + 2] = 0f
            uniformData[offset + 3] = 0f
        }
        val settings = count * 20
        uniformData[settings] = max(pcfRadius, 0).toFloat()
        uniformData[settings + 1] = activeCount.toFloat()
        uniformData[settings + 2] = 0f
        uniformData[settings + 3] = 0f
        device.queue.writeBuffer(uniformBuffer, uniformData)
    }

    /** @return the layer of the [light] from the last [update]; `-1` if it has none. */
    fun indexOf(light: SpotLight): Int {
        for (i in 0 until activeCount) {
            if (lights[i] === light) return i
        }
        return -1
    }

    /**
     * Renders each [nodes] into the layer of every light given one in [update] using the given
     * [batch]. The batch should use a [ShadowMaterialPipelineProvider] and have its depth format set
     * to [format].
     */
    fun render(commandEncoder: CommandEncoder, batch: ModelBatch, nodes: List<Node3D>, dt: Duration) {
        for (i in 0 until activeCount) {
            val camera = cameras[i]
            val renderPassEncoder = beginRenderPass(commandEncoder, i)
            nodes.fastForEach { batch.render(it, camera, environment) }
            batch.flush(renderPassEncoder, camera, dt)
            renderPassEncoder.end()
            renderPassEncoder.release()
        }
    }

    /** Begins a new depth only render pass that renders into the given [layer]. */
    fun beginRenderPass(commandEncoder: CommandEncoder, layer: Int): RenderPassEncoder =
        commandEncoder.beginRenderPass(renderPassDescriptors[layer])

    override fun release() {
        environment.release()
        layerViews.fastForEach { it.release() }
        view.release()
        texture.release()
        sampler.release()
        uniformBuffer.release()
    }

    companion object {
        private val MAX_FOV = 170.degrees.radians

        private fun uniformSize(count: Int) = count * 16 + count * 4 + 4
    }
}
