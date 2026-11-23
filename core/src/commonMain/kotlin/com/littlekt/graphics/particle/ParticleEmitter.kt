package com.littlekt.graphics.particle

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.graphics.Camera
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
import kotlin.math.sqrt
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
    private var orderSize = sortSize(effect.maxParticles)
    private var orderBuffer: GPUBuffer = createOrderBuffer(orderSize)
    private var orderIsIdentity = true
    private var keyBuffer: GPUBuffer? = null
    private var sortInfoBuffer: GPUBuffer? = null
    private val sortInfo = FloatBuffer(SORT_INFO_FLOATS)
    internal var sortBindGroup: BindGroup? = null
        private set

    private var eventBuffer: GPUBuffer = createEventBuffer(1)
    private val counterBuffer = createSmallStorage("ParticleEmitter event count")
    internal val cursorBuffer = createSmallStorage("ParticleEmitter event cursor")
    private val zeroCounter = IntBuffer(4)
    private var spawnInfoBuffer: GPUBuffer? = null
    private val spawnInfo = FloatBuffer(4)
    internal var eventsBindGroup: BindGroup? = null
        private set

    internal var eventDrivenTime = Float.MAX_VALUE

    internal var computeBindGroup: BindGroup = createComputeBindGroup()
        private set

    internal var renderBindGroup: BindGroup = createRenderBindGroup()
        private set

    internal var textureBindGroup: BindGroup = renderer.createTextureBindGroup(effect.texture)
        private set

    internal var meshBindGroup: BindGroup? = effect.mesh?.let { renderer.createMeshBindGroup(it) }
        private set

    private val rotation2d = MutableQuaternion()
    private var effectDirty = true
    private var maxLifetime = effect.lifetime.endInclusive
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
            effectDirty = true
            maxLifetime = value.lifetime.endInclusive
            if (value.maxParticles != old.maxParticles) {
                computeBindGroup.release()
                renderBindGroup.release()
                particleBuffer.release()
                orderBuffer.release()
                releaseSort()
                particleBuffer = createParticleBuffer(value.maxParticles)
                orderSize = sortSize(value.maxParticles)
                orderBuffer = createOrderBuffer(orderSize)
                orderIsIdentity = true
                computeBindGroup = createComputeBindGroup()
                renderBindGroup = createRenderBindGroup()
                cursor = 0
                spawned = 0
                timeSinceSpawn = Float.MAX_VALUE
            } else if (!value.sorted && old.sorted && !orderIsIdentity) {
                writeIdentityOrder(orderBuffer, orderSize)
                orderIsIdentity = true
            }
            if (value.texture !== old.texture) {
                textureBindGroup.release()
                textureBindGroup = renderer.createTextureBindGroup(value.texture)
            }
            if (value.mesh !== old.mesh) {
                meshBindGroup?.release()
                meshBindGroup = value.mesh?.let { renderer.createMeshBindGroup(it) }
            }
        }

    /**
     * Spawns particles of another emitter when particles of this one die or are born, or `null`
     * for none.
     */
    var subEmitter: SubEmitter? = null
        set(value) {
            if (value != null) {
                require(value.emitter.renderer === renderer) {
                    "A SubEmitter must use the same ParticleRenderer as its parent!"
                }
                require(value.emitter !== this) { "An emitter can't be its own SubEmitter!" }
            }
            field = value
            rebuildEvents(value)
        }

    /** If `true`, particles spawn at [ParticleEffect.rate]. Bursts spawn either way. */
    var emitting: Boolean = true

    /** The simulation speed multiplier. */
    var timeScale: Float = 1f

    /** `true` while any particle may still be alive. */
    val isActive: Boolean
        get() = (spawned > 0 && timeSinceSpawn < maxLifetime) || eventDriven

    /** The number of particle slots that may be alive and are simulated & drawn. */
    val activeCount: Int
        get() =
            when {
                eventDriven -> effect.maxParticles
                spawned > 0 && timeSinceSpawn < maxLifetime -> min(spawned, effect.maxParticles)
                else -> 0
            }

    private val eventDriven: Boolean
        get() = eventDrivenTime < maxLifetime

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
        if (eventDrivenTime != Float.MAX_VALUE) eventDrivenTime += seconds
        if (emitting) pendingSpawn += effect.rate * seconds
    }

    /** Writes this frame's simulation parameters. @return the number of slots to simulate */
    internal fun prepare(): Int {
        if (pendingTime <= 0f && pendingBurst == 0 && !pendingClear) return 0
        val max = effect.maxParticles
        if (spawned > 0 && !(timeSinceSpawn < maxLifetime) && !eventDriven) {
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
        val sub = subEmitter
        if (sub != null) {
            device.queue.writeBuffer(counterBuffer, zeroCounter)
            sub.emitter.eventDrivenTime = 0f
        }
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
        params[72] = if (pendingClear) 1f else 0f
        val sub = subEmitter
        params[78] =
            when (sub?.trigger) {
                null -> 0f
                SubEmitterTrigger.DEATH -> 1f
                SubEmitterTrigger.BIRTH -> 2f
            }
        params[79] = sub?.maxEvents?.toFloat() ?: 0f
        if (effectDirty) {
            writeEffect(e)
            effectDirty = false
        }
    }

    private fun writeEffect(e: ParticleEffect) {
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
        params[73] = max(0f, e.stretch)
        params[75] = e.inheritVelocity
        params[76] = if (e.flat) 1f else 0f
        params[77] = max(0f, e.softness)
        val sheet = e.flipbook
        params[80] = sheet?.columns?.toFloat() ?: 1f
        params[81] = sheet?.rows?.toFloat() ?: 1f
        params[82] = sheet?.frames?.toFloat() ?: 1f
        params[83] = sheet?.fps ?: 0f
        params[84] = if (sheet?.loop == true) 1f else 0f
        params[85] = if (sheet?.randomStart == true) 1f else 0f
        params[86] = if (sheet?.blend == true) 1f else 0f
        params[87] = 0f
        params[88] = e.emissive
        params[89] = e.shininess
        params[90] = 0f
        params[91] = 0f
    }

    private fun setShape(type: Float, x: Float, y: Float, z: Float, flag: Float) {
        params[32] = type
        params[33] = x
        params[34] = y
        params[35] = z
        params[74] = flag
    }

    /**
     * Writes the sort parameters for [camera]. @return the number of slots to sort, a power of
     * two, or `0` to skip sorting
     */
    internal fun prepareSort(camera: Camera): Int {
        val active = activeCount
        if (!effect.sorted || active < 2) return 0
        val size = sortSize(active)
        val keys =
            keyBuffer
                ?: device
                    .createBuffer(
                        BufferDescriptor(
                            "ParticleEmitter sort keys",
                            orderSize * Float.SIZE_BYTES.toLong(),
                            BufferUsage.STORAGE,
                            false,
                        )
                    )
                    .also { keyBuffer = it }
        val info =
            sortInfoBuffer
                ?: device
                    .createBuffer(
                        BufferDescriptor(
                            "ParticleEmitter sort info",
                            SORT_INFO_FLOATS * Float.SIZE_BYTES.toLong(),
                            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                            false,
                        )
                    )
                    .also { sortInfoBuffer = it }
        if (sortBindGroup == null) {
            sortBindGroup =
                device.createBindGroup(
                    BindGroupDescriptor(
                        renderer.sortLayout,
                        listOf(
                            BindGroupEntry(0, BufferBinding(keys)),
                            BindGroupEntry(1, BufferBinding(orderBuffer)),
                            BindGroupEntry(2, BufferBinding(info)),
                        ),
                        label = "ParticleEmitter sort bind group",
                    )
                )
        }
        val view = camera.view.data
        val fx = -view[2]
        val fy = -view[6]
        val fz = -view[10]
        val length = sqrt(fx * fx + fy * fy + fz * fz).let { if (it > 0f) it else 1f }
        sortInfo[0] = camera.position.x
        sortInfo[1] = camera.position.y
        sortInfo[2] = camera.position.z
        sortInfo[3] = 0f
        sortInfo[4] = fx / length
        sortInfo[5] = fy / length
        sortInfo[6] = fz / length
        sortInfo[7] = 0f
        sortInfo[8] = active.toFloat()
        sortInfo[9] = size.toFloat()
        sortInfo[10] = 0f
        sortInfo[11] = 0f
        device.queue.writeBuffer(info, sortInfo)
        orderIsIdentity = false
        return size
    }

    /** The threads spawning [subEmitter] particles from this emitter's events. */
    internal val eventSpawnThreads: Int
        get() = subEmitter?.let { it.maxEvents * it.count } ?: 0

    private fun rebuildEvents(sub: SubEmitter?) {
        eventsBindGroup?.release()
        eventsBindGroup = null
        val capacity = sub?.maxEvents ?: 1
        val oldEvents = eventBuffer
        eventBuffer = createEventBuffer(capacity)
        computeBindGroup.release()
        computeBindGroup = createComputeBindGroup()
        oldEvents.release()
        if (sub == null) return
        val info =
            spawnInfoBuffer
                ?: device
                    .createBuffer(
                        BufferDescriptor(
                            "ParticleEmitter spawn info",
                            4L * Float.SIZE_BYTES,
                            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                            false,
                        )
                    )
                    .also { spawnInfoBuffer = it }
        spawnInfo[0] = sub.count.toFloat()
        spawnInfo[1] = sub.maxEvents.toFloat()
        spawnInfo[2] = 0f
        spawnInfo[3] = 0f
        device.queue.writeBuffer(info, spawnInfo)
        eventsBindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    renderer.eventsLayout,
                    listOf(
                        BindGroupEntry(0, BufferBinding(eventBuffer)),
                        BindGroupEntry(1, BufferBinding(counterBuffer)),
                        BindGroupEntry(2, BufferBinding(sub.emitter.cursorBuffer)),
                        BindGroupEntry(3, BufferBinding(info)),
                    ),
                    label = "ParticleEmitter events bind group",
                )
            )
    }

    private fun releaseSort() {
        sortBindGroup?.release()
        sortBindGroup = null
        keyBuffer?.release()
        keyBuffer = null
    }

    private fun createOrderBuffer(size: Int): GPUBuffer =
        device
            .createBuffer(
                BufferDescriptor(
                    "ParticleEmitter order",
                    size * Int.SIZE_BYTES.toLong(),
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                    false,
                )
            )
            .also { writeIdentityOrder(it, size) }

    private fun writeIdentityOrder(buffer: GPUBuffer, size: Int) {
        val identity = IntBuffer(size)
        for (i in 0 until size) identity[i] = i
        device.queue.writeBuffer(buffer, identity)
    }

    private fun createEventBuffer(capacity: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "ParticleEmitter events",
                capacity * EVENT_BYTES,
                BufferUsage.STORAGE,
                false,
            )
        )

    private fun createSmallStorage(label: String): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(label, 16L, BufferUsage.STORAGE or BufferUsage.COPY_DST, false)
        )

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
                    BindGroupEntry(2, BufferBinding(eventBuffer)),
                    BindGroupEntry(3, BufferBinding(counterBuffer)),
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
                    BindGroupEntry(2, BufferBinding(orderBuffer)),
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
        meshBindGroup?.release()
        eventsBindGroup?.release()
        releaseSort()
        sortInfoBuffer?.release()
        spawnInfoBuffer?.release()
        eventBuffer.release()
        counterBuffer.release()
        cursorBuffer.release()
        orderBuffer.release()
        particleBuffer.release()
        paramsBuffer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    internal companion object {
        const val PARAMS_FLOATS = 92
        const val PARTICLE_BYTES = 48L
        const val EVENT_BYTES = 32L
        const val SORT_INFO_FLOATS = 12

        fun sortSize(count: Int): Int {
            var size = 1
            while (size < count) size = size shl 1
            return size
        }
    }
}
