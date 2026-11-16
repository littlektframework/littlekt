package com.littlekt.graphics.g3d.skin

import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.math.*
import com.littlekt.util.datastructure.TreeMap
import com.littlekt.util.seconds
import kotlin.math.min
import kotlin.time.Duration

/**
 * A keyframed animation clip of a [com.littlekt.graphics.g3d.Model]. Clips are usually played,
 * crossfaded, and blended by their model with [com.littlekt.graphics.g3d.Model.play].
 *
 * @author Colton Daily
 * @date 2/3/2025
 */
class Animation(val name: String?) {
    val channels = mutableListOf<AnimationChannel<*>>()

    /** The blend weight of this clip. Weights of all playing clips are normalized when blending. */
    var weight = 1f

    /** The playback speed multiplier. A negative speed plays the clip in reverse. */
    var speed = 1f

    /** The length of this clip in seconds. */
    var duration = 1f
        private set

    /** The current time of this clip in seconds, within `[0, duration]`. */
    var progress = 0f

    /** Whether this clip wraps around at its end. A clip that doesn't loop holds its last pose. */
    var loop = true

    /** Whether this clip, when not looping, has reached its end. */
    var isFinished = false
        private set

    private val _events = mutableListOf<AnimationEvent>()

    /** The events of this clip, sorted by time. */
    val events: List<AnimationEvent>
        get() = _events

    /** Invoked when playback crosses the time of one of [events]. */
    var onEvent: ((AnimationEvent) -> Unit)? = null

    /** Invoked once when this clip, when not looping, reaches its end. */
    var onFinished: (() -> Unit)? = null

    internal var fadeTarget = 0f
    internal var fadeRate = 0f

    private val animationNodes = mutableListOf<AnimationNode>()

    fun prepareAnimation() {
        duration = channels.maxOfOrNull { it.lastKeyTime } ?: 0f
        channels.forEach { it.duration = duration }
        animationNodes.clear()
        animationNodes += channels.map { it.animationNode }.distinct()
    }

    /**
     * Adds an event that is fired with [onEvent] when playback crosses [time].
     *
     * @param time the time of the event in seconds
     * @param name the name of the event
     * @return the added event
     */
    fun addEvent(time: Float, name: String): AnimationEvent {
        val event = AnimationEvent(time, name)
        var index = _events.size
        while (index > 0 && _events[index - 1].time > time) index--
        _events.add(index, event)
        return event
    }

    /** Removes [event] from this clip. */
    fun removeEvent(event: AnimationEvent) {
        _events.remove(event)
    }

    /** Removes all events of this clip. */
    fun clearEvents() {
        _events.clear()
    }

    /** Rewinds this clip to its start, or to its end when [speed] is negative. */
    fun reset() {
        progress = if (speed < 0f) duration else 0f
        isFinished = false
    }

    /**
     * Advances [progress] by [dt] scaled by [speed], wrapping or clamping by [loop], without posing
     * any nodes.
     *
     * @param dt the time to advance by
     * @param notify whether to invoke [onEvent] and [onFinished]
     */
    fun advance(dt: Duration, notify: Boolean = true) {
        if (loop) isFinished = false
        if (isFinished || duration <= 0f) return
        val step = dt.seconds * speed
        if (step == 0f) return
        val from = progress
        var to = from + step
        if (step > 0f) {
            if (to < duration) {
                if (notify) fireEvents(from, true, to, false, false)
            } else if (loop) {
                if (notify) fireEvents(from, true, duration, true, false)
                to %= duration
                if (notify) fireEvents(0f, true, to, false, false)
            } else {
                to = duration
                if (notify) fireEvents(from, true, duration, true, false)
                finish(notify)
            }
        } else {
            if (to > 0f) {
                if (notify) fireEvents(to, false, from, true, true)
            } else if (loop) {
                if (notify) fireEvents(0f, true, from, true, true)
                to = to % duration + duration
                if (notify) fireEvents(to, false, duration, true, true)
            } else {
                to = 0f
                if (notify) fireEvents(0f, true, from, true, true)
                finish(notify)
            }
        }
        progress = to
    }

    /** Advances this clip by [dt] and poses its nodes at full weight. */
    fun apply(dt: Duration) {
        advance(dt)
        applyPose()
    }

    /** Poses the animated nodes at [progress] with full weight. */
    fun applyPose() {
        evaluate(progress)
        for (i in animationNodes.indices) {
            animationNodes[i].applyTransform()
        }
    }

    /**
     * Poses the animated nodes at [time] seconds with full weight without changing [progress].
     *
     * @param time the time in seconds, clamped to `[0, duration]`
     */
    fun sample(time: Float) {
        evaluate(time.coerceIn(0f, duration))
        for (i in animationNodes.indices) {
            animationNodes[i].applyTransform()
        }
    }

    internal fun resetBlend() {
        for (i in animationNodes.indices) {
            animationNodes[i].resetBlend()
        }
    }

    internal fun blend(weight: Float) {
        evaluate(progress)
        for (i in animationNodes.indices) {
            animationNodes[i].blendTransform(weight)
        }
    }

    internal fun applyBlend(totalWeight: Float) {
        for (i in animationNodes.indices) {
            animationNodes[i].applyBlend(totalWeight)
        }
    }

    internal fun shareBlends(
        transforms: MutableMap<Node3D, TransformBlend>,
        morphs: MutableMap<MeshPrimitive, MorphBlend>,
    ) {
        for (i in animationNodes.indices) {
            when (val node = animationNodes[i]) {
                is AnimatedTransformGroup ->
                    node.blend = transforms.getOrPut(node.target) { node.blend }
                is MorphAnimatedMesh -> node.blend = morphs.getOrPut(node.target) { node.blend }
            }
        }
    }

    private fun evaluate(time: Float) {
        for (i in animationNodes.indices) {
            animationNodes[i].initTransform()
        }
        for (i in channels.indices) {
            channels[i].apply(time)
        }
    }

    private fun finish(notify: Boolean) {
        isFinished = true
        if (notify) onFinished?.invoke()
    }

    private fun fireEvents(
        from: Float,
        fromInclusive: Boolean,
        to: Float,
        toInclusive: Boolean,
        reverse: Boolean,
    ) {
        val callback = onEvent ?: return
        val count = _events.size
        for (n in 0 until count) {
            val event = _events[if (reverse) count - 1 - n else n]
            val t = event.time
            val afterFrom = if (fromInclusive) t >= from else t > from
            val beforeTo = if (toInclusive) t <= to else t < to
            if (afterFrom && beforeTo) callback(event)
        }
    }

    fun printChannels() {
        println("$name channels:")
        channels.forEach { ch ->
            println("  ${ch.name} [node: ${ch.animationNode.name}]")
            ch.printKeys("    ")
        }
    }
}

/**
 * A named point in time of an [Animation], such as a footstep or the frame a hit lands.
 *
 * @property time the time of the event in seconds
 * @property name the name of the event
 * @author Colton Daily
 * @date 11/16/2026
 */
class AnimationEvent(val time: Float, val name: String) {
    override fun toString(): String = "AnimationEvent(time=$time, name=$name)"
}

abstract class AnimationChannel<T : AnimationKey<T>>(
    val name: String?,
    val animationNode: AnimationNode,
) {
    val keys = TreeMap<Float, T>()
    val lastKeyTime: Float
        get() = keys.lastKey()

    var duration = 0f

    fun apply(time: Float) {
        var key = keys.floorValue(time)
        if (key == null) {
            key = if (isFuzzyEqual(lastKeyTime, duration)) keys.lastValue() else keys.firstValue()
        }
        key.apply(time, keys.higherValue(time), animationNode)
    }

    fun printKeys(indent: String = "") {
        val animKeys = keys.values.toList()
        for (i in 0 until min(5, animKeys.size)) {
            println("$indent${animKeys[i]}")
        }
        if (animKeys.size > 5) {
            println("$indent  ...${animKeys.size - 5} more")
        }
    }
}

class TranslationAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<TranslationKey>(name, animationNode)

class RotationAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<RotationKey>(name, animationNode)

class ScaleAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<ScaleKey>(name, animationNode)

class WeightAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<WeightKey>(name, animationNode)

interface AnimationNode {
    val name: String?

    fun initTransform() {}

    /** Applies the evaluated pose to the target at full weight. */
    fun applyTransform()

    /** Clears the blend of the target before the clips of a frame are blended. */
    fun resetBlend()

    /** Adds the evaluated pose to the blend of the target with [weight]. */
    fun blendTransform(weight: Float)

    /**
     * Applies the blend of the target. Any weight below [totalWeight] is filled with the rest pose.
     */
    fun applyBlend(totalWeight: Float)

    fun setTranslation(translation: Vec3f) {}

    fun setRotation(rotation: Quaternion) {}

    fun setScale(scale: Vec3f) {}

    fun setWeights(weights: FloatArray) {}
}

class AnimatedTransformGroup(val target: Node3D) : AnimationNode {
    override val name: String
        get() = target.name

    private val _initTranslation = MutableVec3f()
    val initTranslation: Vec3f
        get() = _initTranslation

    private val _initRotation = MutableQuaternion()
    val initRotation: Quaternion
        get() = _initRotation

    private val _initScale = MutableVec3f(1f, 1f, 1f)
    val initScale: Vec3f
        get() = _initScale

    private val animTranslation = MutableVec3f()
    private val animRotation = MutableQuaternion()
    private val animScale = MutableVec3f()

    init {
        target.transform.getTranslation(_initTranslation)
        target.transform.getRotation(_initRotation)
        target.transform.getScale(_initScale)
    }

    internal var blend = TransformBlend(target, _initTranslation, _initRotation, _initScale)

    override fun initTransform() {
        animTranslation.set(_initTranslation)
        animRotation.set(_initRotation)
        animScale.set(_initScale)
    }

    override fun applyTransform() {
        target.setIdentity()
        target.translate(animTranslation)
        target.rotation(animRotation)
        target.scaling(animScale.x, animScale.y, animScale.z)
    }

    override fun resetBlend() {
        blend.reset()
    }

    override fun blendTransform(weight: Float) {
        blend.add(animTranslation, animRotation, animScale, weight)
    }

    override fun applyBlend(totalWeight: Float) {
        blend.apply(totalWeight)
    }

    override fun setTranslation(translation: Vec3f) {
        animTranslation.set(translation)
    }

    override fun setRotation(rotation: Quaternion) {
        animRotation.set(rotation)
    }

    override fun setScale(scale: Vec3f) {
        animScale.set(scale)
    }
}

class MorphAnimatedMesh(val target: MeshPrimitive) : AnimationNode {
    override val name: String = "MeshPrimitive"

    private var weights = FloatArray(1)

    internal var blend = MorphBlend(target)

    override fun applyTransform() {
        target.morphWeights = weights
    }

    override fun resetBlend() {
        blend.reset()
    }

    override fun blendTransform(weight: Float) {
        blend.add(weights, weight)
    }

    override fun applyBlend(totalWeight: Float) {
        blend.apply(totalWeight)
    }

    override fun setWeights(weights: FloatArray) {
        if (this.weights.size != weights.size) {
            this.weights = FloatArray(weights.size)
        }
        for (i in weights.indices) {
            this.weights[i] = weights[i]
        }
    }
}

/**
 * Accumulates the weighted poses of every clip that animates [target], shared by the clips of a
 * model.
 */
internal class TransformBlend(
    val target: Node3D,
    private val restTranslation: Vec3f,
    private val restRotation: Quaternion,
    private val restScale: Vec3f,
) {
    private val translation = MutableVec3f()
    private val rotation = MutableQuaternion()
    private val scale = MutableVec3f()
    private var weight = 0f
    private var applied = false

    fun reset() {
        translation.set(0f, 0f, 0f)
        rotation.set(0f, 0f, 0f, 0f)
        scale.set(0f, 0f, 0f)
        weight = 0f
        applied = false
    }

    fun add(t: Vec3f, r: Quaternion, s: Vec3f, w: Float) {
        if (w <= 0f) return
        translation.x += t.x * w
        translation.y += t.y * w
        translation.z += t.z * w
        val rw = if (weight > 0f && rotation.dot(r) < 0f) -w else w
        rotation.x += r.x * rw
        rotation.y += r.y * rw
        rotation.z += r.z * rw
        rotation.w += r.w * rw
        scale.x += s.x * w
        scale.y += s.y * w
        scale.z += s.z * w
        weight += w
    }

    fun apply(totalWeight: Float) {
        if (applied) return
        applied = true
        if (weight <= 0f) return
        if (totalWeight > weight) add(restTranslation, restRotation, restScale, totalWeight - weight)
        val inv = 1f / weight
        translation.scale(inv)
        scale.scale(inv)
        rotation.norm()
        target.setIdentity()
        target.translate(translation)
        target.rotation(rotation)
        target.scaling(scale.x, scale.y, scale.z)
    }
}

/**
 * Accumulates the weighted morph weights of every clip that animates [target], shared by the clips
 * of a model.
 */
internal class MorphBlend(val target: MeshPrimitive) {
    private var weights = FloatArray(0)
    private var weight = 0f
    private var applied = false

    fun reset() {
        weights.fill(0f)
        weight = 0f
        applied = false
    }

    fun add(values: FloatArray, w: Float) {
        if (w <= 0f) return
        if (weights.size < values.size) weights = weights.copyOf(values.size)
        for (i in values.indices) {
            weights[i] += values[i] * w
        }
        weight += w
    }

    fun apply(totalWeight: Float) {
        if (applied) return
        applied = true
        if (weight <= 0f) return
        val inv = 1f / maxOf(totalWeight, weight)
        for (i in weights.indices) {
            weights[i] *= inv
        }
        target.morphWeights = weights
    }
}
