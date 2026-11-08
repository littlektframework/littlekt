package com.littlekt.graphics.g3d.shadow

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.math.floor

/**
 * A shadow map for a directional light. The scene is rendered from the light's point of view, using
 * [camera] & [environment], into a depth texture covering a square area of size `radius * 2`
 * around [center].
 *
 * Usage each frame:
 * ```
 * shadowMap.update(directionalLight.direction)
 * val shadowPass = shadowMap.beginRenderPass(commandEncoder)
 * shadowBatch.render(scene, shadowMap.camera, shadowMap.environment)
 * shadowBatch.flush(shadowPass, shadowMap.camera, dt)
 * shadowPass.end()
 * ```
 *
 * @param size the width & height of the shadow map texture.
 * @param format the depth format of the shadow map texture.
 * @author Colton Daily
 * @date 11/8/2026
 */
class DirectionalShadowMap(
    val device: Device,
    val size: Int = 2048,
    val format: TextureFormat = TextureFormat.DEPTH32_FLOAT,
) : Releasable {
    /** The camera used to render the scene from the light's point of view. */
    val camera = OrthographicCamera()

    /** The [Environment] to render the shadow casters with. */
    val environment: Environment = UnlitEnvironment(device)

    /** The center of the area that receives shadows. */
    val center = MutableVec3f()

    /** Half the size of the area that receives shadows, in world units. */
    var radius: Float = 20f

    /** Depth bias applied when comparing against the shadow map. */
    var depthBias: Float = 0.001f

    /** Offsets the sampled position along the surface normal, in world units, to reduce acne. */
    var normalOffset: Float = 0.05f

    private val texture =
        device.createTexture(
            TextureDescriptor(
                Extent3D(size, size, 1),
                1,
                1,
                TextureDimension.D2,
                format,
                TextureUsage.RENDER_ATTACHMENT or TextureUsage.TEXTURE,
                "directional shadow map",
            )
        )

    /** The view of the shadow map depth texture. */
    val view: TextureView = texture.createView()

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

    /** The [BufferBinding] holding the light view projection matrix and shadow params. */
    val uniformBufferBinding = BufferBinding(uniformBuffer)

    private val renderPassDescriptor =
        RenderPassDescriptor(
            emptyList(),
            depthStencilAttachment =
                RenderPassDepthStencilAttachmentDescriptor(
                    view = view,
                    depthClearValue = 1f,
                    depthLoadOp = LoadOp.CLEAR,
                    depthStoreOp = StoreOp.STORE,
                ),
            label = "Directional Shadow Map Render Pass",
        )

    private val forward = MutableVec3f()
    private val right = MutableVec3f()
    private val lightUp = MutableVec3f()
    private val upAxis = MutableVec3f()
    private val snapped = MutableVec3f()

    /**
     * Updates the [camera] and the shadow uniform to cover the area around [center] for a light
     * shining from the given direction.
     *
     * @param direction the direction pointing towards the light, the same as
     *   [com.littlekt.graphics.g3d.light.DirectionalLight.direction].
     */
    fun update(direction: Vec3f) {
        forward.set(direction).scale(-1f).norm()
        if (abs(forward.y) > 0.99f) upAxis.set(Vec3f.Z_AXIS) else upAxis.set(Vec3f.Y_AXIS)
        forward.cross(upAxis, right).norm()
        right.cross(forward, lightUp).norm()

        val texelSize = radius * 2f / size
        val x = floor(center.dot(right) / texelSize) * texelSize
        val y = floor(center.dot(lightUp) / texelSize) * texelSize
        val z = center.dot(forward)
        snapped.set(right).scale(x)
        upAxis.set(lightUp).scale(y)
        snapped.add(upAxis)
        upAxis.set(forward).scale(z)
        snapped.add(upAxis)

        camera.position.set(forward).scale(-radius * 2f).add(snapped)
        camera.direction.set(forward)
        camera.up.set(lightUp)
        camera.virtualWidth = radius * 2f
        camera.virtualHeight = radius * 2f
        camera.near = 0f
        camera.far = radius * 4f
        camera.update()

        uniformData.put(camera.viewProjection.data, dstOffset = 0)
        uniformData.put(depthBias)
        uniformData.put(normalOffset)
        uniformData.put(texelSize)
        uniformData.put(0f)
        device.queue.writeBuffer(uniformBuffer, uniformData)
    }

    /** Begins a new depth only render pass that renders into the shadow map. */
    fun beginRenderPass(commandEncoder: CommandEncoder): RenderPassEncoder =
        commandEncoder.beginRenderPass(renderPassDescriptor)

    override fun release() {
        environment.release()
        view.release()
        texture.release()
        sampler.release()
        uniformBuffer.release()
    }

    companion object {
        private const val UNIFORM_SIZE = 20
    }
}
