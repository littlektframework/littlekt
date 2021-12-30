package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.internal.umod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Enables playback of [Animation] classes.
 * @see [Animation]
 * @param KeyFrameType the type of key frame the [Animation] class is using.
 * @author Colton Daily
 * @date 12/29/2021
 */
open class AnimationPlayer<KeyFrameType> {
    /**
     * The current playing animations. Set this by using one of the `play` methods.
     * @see [play]
     * @see [playOnce]
     * @see [playLooped]
     */
    var currentAnimation: Animation<KeyFrameType>? = null
        private set

    /**
     * The total frames the [currentAnimation] has.
     */
    val totalFrames: Int get() = currentAnimation?.totalFrames ?: 1

    /**
     * The total amount of frames played across all animations.
     */
    var totalFramesPlayed = 0
        private set

    /**
     * The index of the current frame
     */
    var currentFrameIdx = 0
        private set(value) {
            field = value umod totalFrames
            onFrameChange?.invoke(field)
        }

    /**
     * Invoked when a frame is changed.
     */
    var onFrameChange: ((Int) -> Unit)? = null

    private var animationRequested = false
    private var numOfFramesRequested = 0
        set(value) {
            if (value == 0) {
                stop()
            }
            field = value
        }
    private var frameDisplayTime: Duration = 100.milliseconds
    private var animationType = AnimationType.STANDARD
    private var loop = false
    private var lastFrameTime: Duration = Duration.ZERO
    private var remainingDuration: Duration = Duration.ZERO

    /**
     * Play the specified animation an X number of times.
     * @param animation the animation to play
     * @param times the number of times to play
     * @param force if true force the animation to restart if already playing; otherwise continue
     */
    fun play(animation: Animation<KeyFrameType>, times: Int = 1, force: Boolean = false) {
        setAnimInfo(
            animation,
            cyclesRequested = times,
            type = AnimationType.STANDARD,
            force = force
        )
    }

    /**
     * Play the specified animation one time.
     * @param animation the animation to play
     * @param force if true force the animation to restart if already playing; otherwise continue
     */
    fun playOnce(animation: Animation<KeyFrameType>, force: Boolean = false) {
        setAnimInfo(
            animation,
            cyclesRequested = 1,
            type = AnimationType.STANDARD,
            force = force
        )
    }

    /**
     * Play the specified animation as a loop.
     * @param animation the animation to play
     * @param force if true force the animation to restart if already playing; otherwise continue
     */
    fun playLooped(animation: Animation<KeyFrameType>, force: Boolean = false) {
        setAnimInfo(
            animation,
            loop = true,
            type = AnimationType.LOOPED,
            force = force
        )
    }

    /**
     * Runs any updates for any requested animation and grabs the next frame if so.
     */
    fun update(dt: Duration) {
        if (animationRequested) {
            nextFrame(dt)
        }
    }

    /**
     * Stops any running animations.
     */
    fun stop() {
        animationRequested = false
    }

    private fun nextFrame(frameTime: Duration) {
        lastFrameTime += frameTime
        if (lastFrameTime + frameTime >= frameDisplayTime) {
            when (animationType) {
                AnimationType.STANDARD -> {
                    if (numOfFramesRequested > 0) {
                        numOfFramesRequested--
                    }
                }
                AnimationType.DURATION -> {
                    remainingDuration -= lastFrameTime
                }
                AnimationType.LOOPED -> {
                    // do nothing, let it loop
                }
            }

            if (animationRequested) {
                totalFramesPlayed++
                currentFrameIdx++
                frameDisplayTime = currentAnimation?.getFrameTime(currentFrameIdx) ?: Duration.ZERO
                lastFrameTime = Duration.ZERO
            }
        }
    }

    private fun setAnimInfo(
        animation: Animation<KeyFrameType>,
        cyclesRequested: Int = 1,
        loop: Boolean = false,
        type: AnimationType = AnimationType.STANDARD,
        force: Boolean = false
    ) {
        if (!force && currentAnimation == animation) return

        currentAnimation = animation
        currentFrameIdx = 0
        frameDisplayTime = currentAnimation?.getFrameTime(currentFrameIdx) ?: Duration.ZERO
        this.loop = loop
        animationType = type
        animationRequested = true
        numOfFramesRequested = cyclesRequested * totalFrames
    }
}

/**
 * The playback types of animation.
 * @author Colton Daily
 * @date 11/27/2021
 */
enum class AnimationType {
    /**
     * An animation that runs from start to end an **X** amount of times.
     */
    STANDARD,

    /**
     * An animation the loops from start to end.
     */
    LOOPED,

    /**
     * An animation type the runs for a certain duration.
     */
    DURATION
}