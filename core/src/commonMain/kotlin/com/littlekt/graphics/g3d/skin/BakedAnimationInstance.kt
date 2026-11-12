package com.littlekt.graphics.g3d.skin

import com.littlekt.graphics.Color
import com.littlekt.graphics.g3d.MeshPrimitiveInstance
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.util.seconds
import kotlin.time.Duration

/**
 * A [Node3D] that draws the meshes of a [BakedAnimations] model and plays one of its clips. Every
 * instance of the same [BakedAnimations] is drawn together, so thousands can be on screen.
 * Animations advance when [update] is called, such as through a parent node.
 *
 * ```
 * val enemy = enemies.createInstance()
 * enemy.play("Run")
 * enemy.position(5f, 0f, 2f)
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class BakedAnimationInstance internal constructor(val animations: BakedAnimations) : Node3D() {
    private val instances =
        List(animations.primitiveCount) { index ->
            MeshPrimitiveInstance().also {
                it.boundsOverride = animations.bounds
                it.addTo(animations.primitive(index))
                addChild(it)
            }
        }

    /** The clip being played, if any. */
    var clip: BakedAnimations.Clip? = null
        private set

    /** The time in seconds into [clip]. */
    var time: Float = 0f
        set(value) {
            field = value
            updateFrames()
        }

    /** The playback speed multiplier. */
    var speed: Float = 1f

    /** If `true`, [clip] starts over once it ends. Otherwise it stops at its last frame. */
    var loop: Boolean = true

    /** `true` if a non-looping [clip] has reached its end. */
    val isFinished: Boolean
        get() {
            val clip = clip ?: return false
            return !loop && time >= clip.duration
        }

    /** The tint of every mesh of this instance. */
    var tint: Color = Color.WHITE
        set(value) {
            field = value
            instances.forEach { it.color = value }
        }

    init {
        name = "BakedAnimationInstance"
        updateFrames()
    }

    /**
     * Plays the given [clip].
     *
     * @param loop `true` to start over once the clip ends
     * @param restart `false` to keep the current time if [clip] is already playing
     */
    fun play(clip: BakedAnimations.Clip, loop: Boolean = true, restart: Boolean = true) {
        this.loop = loop
        if (this.clip === clip && !restart) return
        this.clip = clip
        time = 0f
    }

    /**
     * Plays the clip with the given [name].
     *
     * @param loop `true` to start over once the clip ends
     * @param restart `false` to keep the current time if the clip is already playing
     */
    fun play(name: String, loop: Boolean = true, restart: Boolean = true) {
        val clip = animations.clip(name) ?: error("No baked animation named '$name'!")
        play(clip, loop, restart)
    }

    /** Stops playing and shows the first frame of the first clip. */
    fun stop() {
        clip = null
        time = 0f
    }

    /** Removes this instance from rendering. It can't be drawn again after. */
    fun remove() {
        instances.forEach { it.remove() }
    }

    override fun onUpdate(dt: Duration) {
        super.onUpdate(dt)
        val clip = clip ?: return
        if (speed == 0f || isFinished) return
        var next = time + dt.seconds * speed
        if (loop && clip.duration > 0f) {
            next %= clip.duration
            if (next < 0f) next += clip.duration
        } else {
            next = next.coerceIn(0f, clip.duration)
        }
        time = next
    }

    private fun updateFrames() {
        val clip = clip ?: animations.clips.first()
        val time = if (this.clip == null) 0f else time
        val frame = clip.firstFrame + clip.frameAt(time)
        val blend = clip.blendAt(time)
        val nextFrame = if (blend > 0f) frame + 1 else frame
        for (i in instances.indices) {
            instances[i].setSkinFrames(
                animations.frameOffset(i, frame),
                animations.frameOffset(i, nextFrame),
                blend,
            )
        }
    }

    override fun copy(): Node3D =
        animations.createInstance().also {
            it.name = name
            it.globalTransform = globalTransform
            it.speed = speed
            it.tint = tint
            clip?.let { clip -> it.play(clip, loop) }
            it.time = time
        }

    override fun onDestroy() {
        super.onDestroy()
        remove()
    }
}
