package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.util.internal.umod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * Animation based class that holds info on what frames to be animating. Recommended to be used with [AnimationPlayer]
 * to assist in playing back the frames.
 * @param KeyFrameType the type to be used as the key frame.
 * @see [AnimationPlayer]
 * @author Colton Daily
 * @date 11/27/2021
 */
class Animation<KeyFrameType>(
    /**
     * All the key frames to animate
     */
    val frames: List<KeyFrameType>,
    /**
     * The order of each frame should be played based on the indices of [frames].
     */
    val frameIndices: List<Int>,
    /**
     * The amount of time spent displaying each frame.
     */
    val frameTimes: List<Duration>,
) {
    val id = nextAnimId++
    val frameStackSize: Int get() = frames.size
    val totalFrames: Int get() = frameIndices.size
    val firstFrame: KeyFrameType get() = frames[0]

    /**
     * The total duration of this animation
     */
    val duration = if (frameTimes.isEmpty()) Duration.ZERO else frameTimes.reduce { acc, ft -> acc + ft }

    fun getFrame(time: Duration): KeyFrameType {
        var counter = time
        var index = 0
        while (counter > frameTimes[index] && index < frameIndices.size) {
            counter -= frameTimes[index]
            index++
        }
        return getFrame(index)
    }

    fun getFrame(index: Int): KeyFrameType = frames[frameIndices[index umod frames.size]]

    fun getFrameTime(index: Int): Duration = frameTimes[frameIndices[index umod frames.size]]

    /**
     * @param index the index of the key frame
     * @return the key frame at the specified index
     */
    operator fun get(index: Int) = frames[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Animation<*>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    companion object {
        private var nextAnimId = 1L
    }
}

/**
 * An [Animation] builder to help create custom animations easily.
 * @see [Animation]
 * @see [AnimationPlayer]
 */
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


/**
 * Creates an animation from a prefix based on the names of the sprites in a [TextureAtlas].
 * @param prefix the prefix to use to create the animation
 * @param defaultTimePerFrame the amount of time each frame is displayed
 */
fun TextureAtlas.getAnimation(
    prefix: String = "",
    defaultTimePerFrame: Duration = 100.milliseconds,
): Animation<TextureSlice> {
    val slices = entries.filter { it.name.startsWith(prefix) }.map { it.slice }
    return Animation(slices, List(slices.size) { it }, List(slices.size) { defaultTimePerFrame })
}

/**
 * Create an animation from a prefix of the names of the sprites in a [TextureAtlas] using an [AnimationBuilder]
 * to specify the frame index orders and times.
 * @param prefix the prefix to use to create the animation
 * @param action the [AnimationBuilder] callback to build the animation
 */
fun TextureAtlas.createAnimation(prefix: String, action: AnimationBuilder<TextureSlice>.() -> Unit) =
    AnimationBuilder(entries.filter { it.name.startsWith(prefix) }.map { it.slice }).apply(action).build()