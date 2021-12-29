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