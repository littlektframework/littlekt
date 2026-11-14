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
 * Clips can crossfade, so switching from one to another blends smoothly instead of snapping.
 *
 * ```
 * val enemy = enemies.createInstance()
 * enemy.play("Run")
 * enemy.position(5f, 0f, 2f)
 * enemy.play("Attack", loop = false, fade = 0.2.seconds)
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

    /** The playback speed multiplier. Applies to the clip fading out too. */
    var speed: Float = 1f

    /** If `true`, [clip] starts over once it ends. Otherwise it stops at its last frame. */
    var loop: Boolean = true

    /** The crossfade used by [play] when no fade is given. */
    var defaultFade: Duration = Duration.ZERO

    /** `true` if a non-looping [clip] has reached its end. */
    val isFinished: Boolean
        get() {
            val clip = clip ?: return false
            return !loop && time >= clip.duration
        }

    /** The clip fading out while [clip] fades in, if any. */
    var fadingClip: BakedAnimations.Clip? = null
        private set

    /** `true` while crossfading from [fadingClip] to [clip]. */
    val isFading: Boolean
        get() = fadingClip != null

    private var fadingTime = 0f
    private var fadingLoop = true
    private var fadeElapsed = 0f
    private var fadeDuration = 0f

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
     * @param fade how long to crossfade from the current pose to [clip]. The clip being replaced
     *   keeps playing while it fades out.
     */
    fun play(
        clip: BakedAnimations.Clip,
        loop: Boolean = true,
        restart: Boolean = true,
        fade: Duration = defaultFade,
    ) {
        val wasLooping = this.loop
        this.loop = loop
        if (this.clip === clip && !restart) return
        val current = this.clip
        val seconds = fade.seconds
        if (current != null && seconds > 0f) {
            fadingClip = current
            fadingTime = time
            fadingLoop = wasLooping
            fadeElapsed = 0f
            fadeDuration = seconds
        } else {
            fadingClip = null
        }
        this.clip = clip
        time = 0f
    }

    /**
     * Plays the clip with the given [name].
     *
     * @param loop `true` to start over once the clip ends
     * @param restart `false` to keep the current time if the clip is already playing
     * @param fade how long to crossfade from the current pose to the clip
     */
    fun play(
        name: String,
        loop: Boolean = true,
        restart: Boolean = true,
        fade: Duration = defaultFade,
    ) {
        val clip = animations.clip(name) ?: error("No baked animation named '$name'!")
        play(clip, loop, restart, fade)
    }

    /** Stops playing and shows the first frame of the first clip. */
    fun stop() {
        clip = null
        fadingClip = null
        time = 0f
    }

    /** Removes this instance from rendering. It can't be drawn again after. */
    fun remove() {
        instances.forEach { it.remove() }
    }

    override fun onUpdate(dt: Duration) {
        super.onUpdate(dt)
        val seconds = dt.seconds
        var changed = false
        val fading = fadingClip
        if (fading != null) {
            fadingTime = advance(fading, fadingTime, fadingLoop, seconds * speed)
            fadeElapsed += seconds
            if (fadeElapsed >= fadeDuration) fadingClip = null
            changed = true
        }
        val clip = clip
        if (clip != null && speed != 0f && !isFinished) {
            val next = advance(clip, time, loop, seconds * speed)
            if (next != time) {
                time = next
                return
            }
        }
        if (changed) updateFrames()
    }

    private fun advance(clip: BakedAnimations.Clip, time: Float, loop: Boolean, delta: Float): Float {
        var next = time + delta
        if (loop && clip.duration > 0f) {
            next %= clip.duration
            if (next < 0f) next += clip.duration
        } else {
            next = next.coerceIn(0f, clip.duration)
        }
        return next
    }

    private fun updateFrames() {
        val clip = clip ?: animations.clips.first()
        val time = if (this.clip == null) 0f else time
        val frame = clip.firstFrame + clip.frameAt(time)
        val blend = clip.blendAt(time)
        val nextFrame = if (blend > 0f) frame + 1 else frame
        val fading = fadingClip
        if (fading == null) {
            for (i in instances.indices) {
                instances[i].setSkinFrames(
                    animations.frameOffset(i, frame),
                    animations.frameOffset(i, nextFrame),
                    blend,
                )
            }
            return
        }
        val fadeFrame = fading.firstFrame + fading.frameAt(fadingTime)
        val fadeBlend = fading.blendAt(fadingTime)
        val fadeNextFrame = if (fadeBlend > 0f) fadeFrame + 1 else fadeFrame
        val weight = (1f - fadeElapsed / fadeDuration).coerceIn(0f, 1f)
        for (i in instances.indices) {
            instances[i].setSkinFrames(
                animations.frameOffset(i, frame),
                animations.frameOffset(i, nextFrame),
                blend,
                animations.frameOffset(i, fadeFrame),
                animations.frameOffset(i, fadeNextFrame),
                fadeBlend,
                weight,
            )
        }
    }

    override fun copy(): Node3D =
        animations.createInstance().also {
            it.name = name
            it.globalTransform = globalTransform
            it.speed = speed
            it.tint = tint
            it.defaultFade = defaultFade
            clip?.let { clip -> it.play(clip, loop, fade = Duration.ZERO) }
            it.time = time
        }

    override fun onDestroy() {
        super.onDestroy()
        remove()
    }
}
