package com.littlekt.graph.node.node2d

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.particle.ParticleAlignment
import com.littlekt.graphics.particle.ParticleEffect
import com.littlekt.graphics.particle.ParticleEmitter
import com.littlekt.graphics.particle.ParticleRenderer
import com.littlekt.graphics.webgpu.CommandEncoder
import com.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * Adds a [GpuParticles2D] to the current [Node] as a child and then triggers the [callback].
 *
 * @param renderer the renderer that simulates & draws the particles
 * @param effect how particles spawn, move and look
 * @param callback the callback that is invoked with a [GpuParticles2D] context in order to
 *   initialize any values
 * @return the newly created [GpuParticles2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.gpuParticles2d(
    renderer: ParticleRenderer,
    effect: ParticleEffect,
    callback: @SceneGraphDslMarker GpuParticles2D.() -> Unit = {},
): GpuParticles2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return GpuParticles2D(renderer, effect).also(callback).addTo(this)
}

/**
 * Adds a [GpuParticles2D] to the current [SceneGraph.root] as a child and then triggers the
 * [callback].
 *
 * @param renderer the renderer that simulates & draws the particles
 * @param effect how particles spawn, move and look
 * @param callback the callback that is invoked with a [GpuParticles2D] context in order to
 *   initialize any values
 * @return the newly created [GpuParticles2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.gpuParticles2d(
    renderer: ParticleRenderer,
    effect: ParticleEffect,
    callback: @SceneGraphDslMarker GpuParticles2D.() -> Unit = {},
): GpuParticles2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.gpuParticles2d(renderer, effect, callback)
}

/**
 * A [Node2D] that draws a [ParticleEmitter] in tree order with the rest of the canvas. Particles
 * spawn at this node's global position, rotation and scale. The [renderer] is owned by the user,
 * so it can be shared by many nodes and must be released after them; set its formats to match the
 * canvas render pass.
 *
 * Effects are in canvas units, so speeds, sizes & gravity are usually in pixels, such as a gravity
 * of `Vec3f(0f, -980f, 0f)`. Effects are always [ParticleEffect.flat], so the
 * [ParticleEffect.direction] is in the XY plane, and [ParticleAlignment.HORIZONTAL] doesn't apply.
 *
 * @param renderer the renderer that simulates & draws the particles
 * @param effect how particles spawn, move and look
 * @author Colton Daily
 * @date 11/14/2026
 */
open class GpuParticles2D(val renderer: ParticleRenderer, effect: ParticleEffect) : Node2D() {

    /** The emitter that simulates the particles. Released when this node is destroyed. */
    val emitter: ParticleEmitter = ParticleEmitter(renderer, effect.flatten())

    /** How particles spawn, move and look. It is made [ParticleEffect.flat] to stay in 2D. */
    var effect: ParticleEffect
        get() = emitter.effect
        set(value) {
            emitter.effect = value.flatten()
        }

    /** If `true`, particles spawn at [ParticleEffect.rate]. Bursts spawn either way. */
    var emitting: Boolean
        get() = emitter.emitting
        set(value) {
            emitter.emitting = value
        }

    /** The simulation speed multiplier. */
    var timeScale: Float
        get() = emitter.timeScale
        set(value) {
            emitter.timeScale = value
        }

    /** `true` while any particle may still be alive. */
    val isActive: Boolean
        get() = emitter.isActive

    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private var lastRotation = Angle.ZERO
    private var lastScaleX = Float.NaN
    private var lastScaleY = Float.NaN
    private val simulateSlot: (CommandEncoder) -> Unit = ::simulate

    /** Spawns [count] particles at the next simulation. */
    fun burst(count: Int) {
        emitter.burst(count)
    }

    /** Removes every particle at the next simulation. */
    fun clear() {
        emitter.clear()
    }

    override fun onAddedToScene() {
        super.onAddedToScene()
        scene?.onPreRender?.connect(this, simulateSlot)
    }

    override fun onRemovedFromScene() {
        super.onRemovedFromScene()
        scene?.onPreRender?.disconnect(this)
    }

    override fun update(dt: Duration) {
        super.update(dt)
        syncTransform()
        emitter.update(dt)
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        if (emitter.activeCount == 0) return
        val pass = canvasRenderPassOrNull ?: return
        if (batch.drawing) batch.flush(pass)
        renderer.render(pass, camera, emitter)
    }

    private fun ParticleEffect.flatten(): ParticleEffect = if (flat) this else copy(flat = true)

    private fun simulate(commandEncoder: CommandEncoder) {
        if (!enabled) return
        renderer.simulate(commandEncoder, emitter)
    }

    private fun syncTransform() {
        val x = globalX
        val y = globalY
        val rotation = globalRotation
        val scaleX = globalScaleX
        val scaleY = globalScaleY
        if (x != lastX || y != lastY) {
            emitter.position(x, y)
            lastX = x
            lastY = y
        }
        if (rotation != lastRotation) {
            emitter.rotation(rotation)
            lastRotation = rotation
        }
        if (scaleX != lastScaleX || scaleY != lastScaleY) {
            emitter.scaling(scaleX, scaleY, 1f)
            lastScaleX = scaleX
            lastScaleY = scaleY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scene?.onPreRender?.disconnect(this)
        emitter.release()
    }
}
