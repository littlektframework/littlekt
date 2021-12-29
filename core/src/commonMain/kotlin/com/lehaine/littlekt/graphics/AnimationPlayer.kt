package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.internal.umod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
open class AnimationPlayer<T> {
    var currentAnimation: Animation<T>? = null
    val totalFrames: Int get() = currentAnimation?.totalFrames ?: 1
    var totalFramesPlayed = 0
        private set

    var currentFrameIdx = 0
        private set(value) {
            field = value umod totalFrames
            onFrameChange?.invoke(value)
        }

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


    fun play(times: Int = 1) {
        setAnimInfo(
            cyclesRequested = times,
            type = AnimationType.STANDARD
        )
    }

    fun playOnce() {
        setAnimInfo(
            cyclesRequested = 1,
            type = AnimationType.STANDARD
        )
    }

    fun playLooped() {
        setAnimInfo(
            loop = true,
            type = AnimationType.LOOPED
        )
    }

    fun update(dt: Duration) {
        if (animationRequested) {
            nextFrame(dt)
        }
    }

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
        cyclesRequested: Int = 1,
        loop: Boolean = false,
        type: AnimationType = AnimationType.STANDARD
    ) {
        currentFrameIdx = 0
        frameDisplayTime = currentAnimation?.getFrameTime(currentFrameIdx) ?: Duration.ZERO
        this.loop = loop
        animationType = type
        animationRequested = true
        numOfFramesRequested = cyclesRequested * totalFrames
    }
}
