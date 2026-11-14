package com.littlekt.graphics.particle

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration

/**
 * A [Node3D] that spawns and simulates particles on the GPU with a [ParticleEffect]. Spawning
 * follows the node's global transform, so emitters can be attached to models.
 *
 * Works in 2D too, on the `z = 0` plane: use [position] with `x` & `y` and [rotation] with an
 * [Angle] around the z axis.
 *
 * Each frame, after [update], call [ParticleRenderer.simulate] before the render pass and
 * [ParticleRenderer.render] within it.
 *
 * ```
 * val emitter = ParticleEmitter(particles, sparks)
 * emitter.position(0f, 1f, 0f)
 * emitter.burst(100)
 * ```
 *
 * @param renderer the renderer that simulates & draws this emitter
 * @param effect how particles spawn, move and look
 * @author Colton Daily
 * @date 11/14/2026
 */
class ParticleEmitter(val renderer: ParticleRenderer, effect: ParticleEffect) :
    Node3D(), Releasable {
    private val device = renderer.device
    private val params = FloatBuffer(PARAMS_FLOATS)
    private val paramsBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleEmitter params",
                PARAMS_FLOATS * Float.SIZE_BYTES.toLong(),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )
    private var particleBuffer: GPUBuffer = createParticleBuffer(effect.maxParticles)
    internal var computeBindGroup: BindGroup = createComputeBindGroup()
        private set

    internal var renderBindGroup: BindGroup = createRenderBindGroup()
        private set

    internal var textureBindGroup: BindGroup = renderer.createTextureBindGroup(effect.texture)
        private set

    private val rotation2d = MutableQuaternion()
    private val previousPosition = MutableVec3f()
    private val currentPosition = MutableVec3f()
    private var hasPreviousPosition = false
    private var pendingSpawn = 0f
    private var pendingBurst = 0
    private var pendingTime = 0f
    private var pendingClear = false
    private var cursor = 0
    private var spawned = 0
    private var timeSinceSpawn = Float.MAX_VALUE

    /** How particles spawn, move and look. Changing [ParticleEffect.maxParticles] clears them. */
    var effect: ParticleEffect = effect
        set(value) {
            val old = field
            field = value
            if (value.maxParticles != old.maxParticles) {
                computeBindGroup.release()
                renderBindGroup.release()
                particleBuffer.release()
                particleBuffer = createParticleBuffer(value.maxParticles)
                computeBindGroup = createComputeBindGroup()
                renderBindGroup = createRenderBindGroup()
                cursor = 0
                spawned = 0
                timeSinceSpawn = Float.MAX_VALUE
            }
            if (value.texture !== old.texture) {
                textureBindGroup.release()
                textureBindGroup = renderer.createTextureBindGroup(value.texture)
            }
        }

    /** If `true`, particles spawn at [ParticleEffect.rate]. Bursts spawn either way. */
    var emitting: Boolean = true

    /** The simulation speed multiplier. */
    var timeScale: Float = 1f

    /** `true` while any particle may still be alive. */
    val isActive: Boolean
        get() = spawned > 0 && timeSinceSpawn < effect.lifetime.endInclusive

    /** The number of particle slots that may be alive and are simulated & drawn. */
    val activeCount: Int
        get() = if (isActive) min(spawned, effect.maxParticles) else 0

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    init {
        name = "ParticleEmitter"
    }

    /** Moves the emitter to [x], [y] on the `z = 0` plane, such as for 2D. */
    fun position(x: Float, y: Float): ParticleEmitter {
        position(x, y, 0f)
        return this
    }

    /** Sets the emitter's rotation to [angle] around the z axis, such as for 2D. */
    fun rotation(angle: Angle): ParticleEmitter {
        rotation(rotation2d.identity().rotate(angle, Vec3f.Z_AXIS))
        return this
    }

    /** Spawns [count] particles at the next simulation. */
    fun burst(count: Int) {
        require(count >= 0) { "count can't be negative!" }
        pendingBurst += count
    }

    /** Removes every particle at the next simulation. */
    fun clear() {
        pendingClear = true
        pendingSpawn = 0f
        pendingBurst = 0
        spawned = 0
        cursor = 0
        timeSinceSpawn = Float.MAX_VALUE
    }

    override fun onUpdate(dt: Duration) {
        super.onUpdate(dt)
        val seconds = dt.seconds * timeScale
        pendingTime += seconds
        if (emitting) pendingSpawn += effect.rate * seconds
    }

    /** Writes this frame's simulation parameters. @return the number of slots to simulate */
    internal fun prepare(): Int {
        if (pendingTime <= 0f && pendingBurst == 0 && !pendingClear) return 0
        val max = effect.maxParticles
        if (spawned > 0 && !isActive) {
            spawned = 0
            cursor = 0
        }
        val whole = pendingSpawn.toInt()
        pendingSpawn -= whole
        val count = min(whole + pendingBurst, max)
        pendingBurst = 0
        val start = cursor
        cursor = (cursor + count) % max
        if (count > 0) {
            spawned = min(spawned + count, max)
            timeSinceSpawn = 0f
        } else if (timeSinceSpawn != Float.MAX_VALUE) {
            timeSinceSpawn += pendingTime
        }
        val slots = if (pendingClear) max else activeCount
        if (slots == 0) {
            pendingTime = 0f
            return 0
        }

        globalTransform.data.let { data -> for (i in 0 until 16) params[i] = data[i] }
        globalPosition.let { currentPosition.set(it) }
        if (!hasPreviousPosition) {
            previousPosition.set(currentPosition)
            hasPreviousPosition = true
        }
        writeParams(start, count, max)
        previousPosition.set(currentPosition)
        pendingTime = 0f
        pendingClear = false
        device.queue.writeBuffer(paramsBuffer, params)
        return slots
    }

    private fun writeParams(start: Int, count: Int, max: Int) {
        val e = effect
        params[16] = previousPosition.x
        params[17] = previousPosition.y
        params[18] = previousPosition.z
        params[19] = pendingTime
        params[20] = start.toFloat()
        params[21] = count.toFloat()
        params[22] = max.toFloat()
        params[23] = Random.nextFloat() * 65536f
        params[24] = e.gravity.x
        params[25] = e.gravity.y
        params[26] = e.gravity.z
        params[27] = e.drag
        params[28] = e.direction.x
        params[29] = e.direction.y
        params[30] = e.direction.z
        params[31] = cos(e.spread.radians.coerceIn(0f, 180.degrees.radians))
        when (val shape = e.shape) {
            EmitterShape.Point -> setShape(0f, 0f, 0f, 0f, 0f)
            is EmitterShape.Sphere ->
                setShape(1f, shape.radius, 0f, 0f, if (shape.surface) 1f else 0f)
            is EmitterShape.Box ->
                setShape(2f, shape.halfExtents.x, shape.halfExtents.y, shape.halfExtents.z, 0f)
            is EmitterShape.Circle -> setShape(3f, shape.radius, 0f, 0f, if (shape.edge) 1f else 0f)
        }
        params[36] = e.lifetime.start
        params[37] = e.lifetime.endInclusive
        params[38] = e.speed.start
        params[39] = e.speed.endInclusive
        params[40] = e.size.start
        params[41] = e.size.endInclusive
        params[42] = e.radialSpeed.start
        params[43] = e.radialSpeed.endInclusive
        params[44] = e.rotation.start.degrees.radians
        params[45] = e.rotation.endInclusive.degrees.radians
        params[46] = e.angularVelocity.start.degrees.radians
        params[47] = e.angularVelocity.endInclusive.degrees.radians
        params[48] = if (e.localSpace) 1f else 0f
        params[49] = e.alignment.ordinal.toFloat()
        params[50] = e.colors.size.toFloat()
        params[51] = e.sizes.size.toFloat()
        for (i in 0 until ParticleEffect.MAX_KEYS) {
            params[52 + i] = e.sizes[min(i, e.sizes.lastIndex)]
            val color = e.colors[min(i, e.colors.lastIndex)]
            params[56 + i * 4] = color.r
            params[57 + i * 4] = color.g
            params[58 + i * 4] = color.b
            params[59 + i * 4] = color.a
        }
        params[72] = if (pendingClear) 1f else 0f
        params[73] = max(0f, e.stretch)
        params[75] = e.inheritVelocity
        params[76] = if (e.flat) 1f else 0f
    }

    private fun setShape(type: Float, x: Float, y: Float, z: Float, flag: Float) {
        params[32] = type
        params[33] = x
        params[34] = y
        params[35] = z
        params[74] = flag
    }

    private fun createParticleBuffer(maxParticles: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleEmitter particles",
                maxParticles * PARTICLE_BYTES,
                BufferUsage.STORAGE,
                false,
            )
        )

    private fun createComputeBindGroup(): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                renderer.computeLayout,
                listOf(
                    BindGroupEntry(0, BufferBinding(paramsBuffer)),
                    BindGroupEntry(1, BufferBinding(particleBuffer)),
                ),
                label = "ParticleEmitter compute bind group",
            )
        )

    private fun createRenderBindGroup(): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                renderer.emitterLayout,
                listOf(
                    BindGroupEntry(0, BufferBinding(paramsBuffer)),
                    BindGroupEntry(1, BufferBinding(particleBuffer)),
                ),
                label = "ParticleEmitter render bind group",
            )
        )

    override fun release() {
        if (isReleased) return
        isReleased = true
        computeBindGroup.release()
        renderBindGroup.release()
        textureBindGroup.release()
        particleBuffer.release()
        paramsBuffer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    internal companion object {
        const val PARAMS_FLOATS = 80
        const val PARTICLE_BYTES = 48L
    }
}
