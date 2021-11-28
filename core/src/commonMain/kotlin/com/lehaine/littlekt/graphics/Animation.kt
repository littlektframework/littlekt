package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.internal.umod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * @author Colton Daily
 * @date 11/27/2021
 */
enum class AnimationType {
    STANDARD, LOOPED, DURATION
}

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
class Animation<T>(
    val frames: List<T>,
    val frameIndices: List<Int>,
    val frameTimes: List<Duration>
) {
    val frameStackSize: Int get() = frames.size
    val totalFrames: Int get() = frameIndices.size
    val firstFrame: T get() = frames[0]

    /**
     * The total duration of this animation
     */
    val duration = frameTimes.reduce { acc, ft -> acc + ft }
    val currentFrame get() = getFrame(currentFrameIdx)

    var totalFramesPlayed = 0
        private set

    var currentFrameIdx = 0
        private set(value) {
            field = value umod totalFrames
        }

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
    private var lastFrameTime: Duration = 0.milliseconds
    private var remainingDuration: Duration = 0.milliseconds


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
                frameDisplayTime = getFrameTime(currentFrameIdx)
                lastFrameTime = 0.milliseconds
            }
        }
    }

    private fun setAnimInfo(
        cyclesRequested: Int = 1,
        loop: Boolean = false,
        type: AnimationType = AnimationType.STANDARD
    ) {
        currentFrameIdx = 0
        frameDisplayTime = getFrameTime(currentFrameIdx)
        this.loop = loop
        animationType = type
        animationRequested = true
        numOfFramesRequested = cyclesRequested * totalFrames
    }

    fun getFrame(time: Duration): T {
        var counter = time
        var index = 0
        while (counter > frameTimes[index] && index < frameIndices.size) {
            counter -= frameTimes[index]
            index++
        }
        return getFrame(index)
    }

    fun getFrame(index: Int): T = frames[frameIndices[index umod frames.size]]

    fun getFrameTime(index: Int): Duration = frameTimes[frameIndices[index umod frames.size]]

    /**
     * @param index the index of the key frame
     * @return the key frame at the specified index
     */
    operator fun get(index: Int) = frames[index]
}

class AnimationBuilder<T>(private val frames: List<T>) {

    private val frameIndices = arrayListOf<Int>()
    private val frameTimes = arrayListOf<Duration>()

    fun frames(indices: IntRange = 0..frames.size, repeats: Int = 0, frameTime: Duration = 100.milliseconds) {
        repeat(repeats + 1) {
            frameIndices.addAll(indices)
            repeat(indices.count()) { frameTimes += frameTime }
        }
    }

    fun frames(index: Int = 0, repeats: Int = 0, frameTime: Duration = 100.milliseconds) =
        frames(index..index, repeats, frameTime)

    fun build(): Animation<T> = Animation(frames, frameIndices, frameTimes)
}


fun TextureAtlas.getAnimation(
    prefix: String = "",
    defaultTimePerFrame: Duration = 100.milliseconds
): Animation<TextureSlice> {
    val slices = entries.filter { it.name.startsWith(prefix) }.map { it.slice }
    return Animation(slices, List(slices.size) { it }, List(slices.size) { defaultTimePerFrame })
}