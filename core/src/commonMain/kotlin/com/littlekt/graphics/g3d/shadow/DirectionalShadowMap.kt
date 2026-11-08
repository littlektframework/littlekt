package com.littlekt.graphics.g3d.shadow

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.util.datastructure.fastForEach
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration

/**
 * A cascaded shadow map for a directional light. The view frustum of the main camera, up to
 * [shadowDistance], is split into [cascadeCount] cascades. Each cascade is rendered from the
 * light's point of view, using its camera in [cameras] & [environment], into its own layer of the
 * shadow map. Closer cascades cover a smaller area which keeps nearby shadows sharp.
 *
 * Usage each frame:
 * ```
 * shadowMap.update(directionalLight.direction, camera)
 * shadowMap.render(commandEncoder, shadowBatch, nodes, dt)
 * ```
 *
 * @param cascadeCount the number of cascades. Must be between `1` and [MAX_CASCADES].
 * @param size the width & height of each cascade.
 * @param format the depth format of the shadow map texture.
 * @author Colton Daily
 * @date 11/8/2026
 */
class DirectionalShadowMap(
    val device: Device,
    val cascadeCount: Int = 3,
    val size: Int = 2048,
    val format: TextureFormat = TextureFormat.DEPTH32_FLOAT,
) : Releasable {

    init {
        require(cascadeCount in 1..MAX_CASCADES) {
            "DirectionalShadowMap.cascadeCount must be between 1 and $MAX_CASCADES!"
        }
    }

    /** The cameras used to render each cascade from the light's point of view. */
    val cameras: List<OrthographicCamera> = List(cascadeCount) { OrthographicCamera() }

    /** The [Environment] to render the shadow casters with. */
    val environment: Environment = UnlitEnvironment(device)

    /** The max distance from the camera that receives shadows. */
    var shadowDistance: Float = 60f

    /**
     * Blends the cascade splits between uniform (`0`) and logarithmic (`1`). Higher values give
     * closer cascades more resolution.
     */
    var splitLambda: Float = 0.8f

    /** How far behind each cascade, towards the light, shadow casters are still rendered. */
    var casterDistance: Float = 50f

    /** Depth bias applied when comparing against the shadow map. */
    var depthBias: Float = 0.0005f

    /** Offsets the sampled position along the surface normal, in texels, to reduce acne. */
    var normalOffset: Float = 1.5f

    private val texture =
        device.createTexture(
            TextureDescriptor(
                Extent3D(size, size, cascadeCount),
                1,
                1,
                TextureDimension.D2,
                format,
                TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                "directional shadow map",
            )
        )

    /** The array view of all the cascades of the shadow map depth texture. */
    val view: TextureView =
        texture.createView(
            TextureViewDescriptor(
                format,
                TextureViewDimension.D2_ARRAY,
                TextureAspect.DEPTH_ONLY,
                arrayLayerCount = cascadeCount,
            )
        )

    private val cascadeViews =
        List(cascadeCount) {
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
                label = "directional shadow map sampler",
            )
        )

    private val uniformData = FloatBuffer(UNIFORM_SIZE)
    private val uniformBuffer =
        device.createGPUFloatBuffer(
            "directional shadow map uniform",
            uniformData,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    /** The [BufferBinding] holding the cascade matrices, splits and shadow params. */
    val uniformBufferBinding = BufferBinding(uniformBuffer)

    private val renderPassDescriptors =
        List(cascadeCount) {
            RenderPassDescriptor(
                emptyList(),
                depthStencilAttachment =
                    RenderPassDepthStencilAttachmentDescriptor(
                        view = cascadeViews[it],
                        depthClearValue = 1f,
                        depthLoadOp = LoadOp.CLEAR,
                        depthStoreOp = StoreOp.STORE,
                    ),
                label = "Directional Shadow Map Cascade $it Render Pass",
            )
        }

    private val splits = FloatArray(cascadeCount)
    private val nearCorners = Array(4) { MutableVec3f() }
    private val farCorners = Array(4) { MutableVec3f() }
    private val corner = MutableVec3f()
    private val forward = MutableVec3f()
    private val right = MutableVec3f()
    private val lightUp = MutableVec3f()
    private val upAxis = MutableVec3f()
    private val center = MutableVec3f()
    private val snapped = MutableVec3f()

    /**
     * Updates each cascade's camera to cover its slice of the [camera] frustum, for a light shining
     * from the given direction, and updates the shadow uniform.
     *
     * @param direction the direction pointing towards the light, the same as
     *   [com.littlekt.graphics.g3d.light.DirectionalLight.direction].
     * @param camera the main camera that will render the scene receiving the shadows.
     */
    fun update(direction: Vec3f, camera: Camera) {
        forward.set(direction).scale(-1f).norm()
        if (abs(forward.y) > 0.99f) upAxis.set(Vec3f.Z_AXIS) else upAxis.set(Vec3f.Y_AXIS)
        forward.cross(upAxis, right).norm()
        right.cross(forward, lightUp).norm()

        val near = camera.near
        val far = camera.far
        val maxDistance = shadowDistance.coerceIn(near, far)
        for (i in 0 until 4) {
            val x = if (i == 0 || i == 3) -1f else 1f
            val y = if (i < 2) -1f else 1f
            camera.unproject(corner.set(x, y, 0f), nearCorners[i])
            camera.unproject(corner.set(x, y, 1f), farCorners[i])
        }

        for (i in 0 until cascadeCount) {
            val p = (i + 1).toFloat() / cascadeCount
            val logNear = max(near, 0.01f)
            val log = logNear * (maxDistance / logNear).pow(p)
            val uniform = near + (maxDistance - near) * p
            splits[i] = splitLambda * log + (1f - splitLambda) * uniform
        }

        uniformData.position = 0
        for (i in 0 until cascadeCount) {
            val start = if (i == 0) near else splits[i - 1]
            updateCascade(i, start, splits[i], near, far)
        }

        for (i in 0 until MAX_CASCADES) {
            uniformData.put(if (i < cascadeCount) splits[i] else 0f)
        }
        uniformData.put(depthBias)
        uniformData.put(cascadeCount.toFloat())
        uniformData.put(0f)
        uniformData.put(0f)
        for (i in 0 until MAX_CASCADES) {
            val texelSize = if (i < cascadeCount) cameras[i].virtualWidth / size else 0f
            uniformData.put(texelSize * normalOffset)
        }
        device.queue.writeBuffer(uniformBuffer, uniformData)
    }

    /**
     * Renders each [nodes] into every cascade of the shadow map using the given [batch]. The batch
     * should use a [ShadowMaterialPipelineProvider] and have its depth format set to [format].
     */
    fun render(commandEncoder: CommandEncoder, batch: ModelBatch, nodes: List<Node3D>, dt: Duration) {
        for (i in 0 until cascadeCount) {
            val camera = cameras[i]
            val renderPassEncoder = beginRenderPass(commandEncoder, i)
            nodes.fastForEach { batch.render(it, camera, environment) }
            batch.flush(renderPassEncoder, camera, dt)
            renderPassEncoder.end()
            renderPassEncoder.release()
        }
    }

    /** Begins a new depth only render pass that renders into the given [cascade]. */
    fun beginRenderPass(commandEncoder: CommandEncoder, cascade: Int): RenderPassEncoder =
        commandEncoder.beginRenderPass(renderPassDescriptors[cascade])

    private fun updateCascade(index: Int, start: Float, end: Float, near: Float, far: Float) {
        val t0 = (start - near) / (far - near)
        val t1 = (end - near) / (far - near)
        center.set(0f, 0f, 0f)
        for (i in 0 until 4) {
            center.add(corner.set(nearCorners[i]).lerp(farCorners[i], t0))
            center.add(corner.set(nearCorners[i]).lerp(farCorners[i], t1))
        }
        center.scale(1f / 8f)

        var radius = 0f
        for (i in 0 until 4) {
            corner.set(nearCorners[i]).lerp(farCorners[i], t0)
            radius = max(radius, corner.distance(center))
            corner.set(nearCorners[i]).lerp(farCorners[i], t1)
            radius = max(radius, corner.distance(center))
        }
        radius = ceil(radius * 16f) / 16f

        val texelSize = radius * 2f / size
        val x = floor(center.dot(right) / texelSize) * texelSize
        val y = floor(center.dot(lightUp) / texelSize) * texelSize
        val z = center.dot(forward)
        snapped.set(right).scale(x)
        snapped.add(corner.set(lightUp).scale(y))
        snapped.add(corner.set(forward).scale(z))

        val camera = cameras[index]
        camera.position.set(forward).scale(-(radius + casterDistance)).add(snapped)
        camera.direction.set(forward)
        camera.up.set(lightUp)
        camera.virtualWidth = radius * 2f
        camera.virtualHeight = radius * 2f
        camera.near = 0f
        camera.far = radius * 2f + casterDistance
        camera.update()

        uniformData.put(camera.viewProjection.data, dstOffset = index * 16)
        uniformData.position = MAX_CASCADES * 16
    }

    override fun release() {
        environment.release()
        cascadeViews.fastForEach { it.release() }
        view.release()
        texture.release()
        sampler.release()
        uniformBuffer.release()
    }

    companion object {
        /** The max number of cascades. */
        const val MAX_CASCADES = 4

        private const val UNIFORM_SIZE = MAX_CASCADES * 16 + 4 + 4 + 4
    }
}
