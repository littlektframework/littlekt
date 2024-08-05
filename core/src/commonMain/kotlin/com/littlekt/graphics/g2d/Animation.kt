package com.littlekt.graphics.g2d

import com.littlekt.util.internal.umod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Animation based class that holds info on what frames to be animating. Recommended to be used with
 * [AnimationPlayer] to assist in playing back the frames.
 *
 * @param KeyFrameType the type to be used as the key frame.
 * @see [AnimationPlayer]
 * @author Colton Daily
 * @date 11/27/2021
 */
class Animation<KeyFrameType>(
    /** All the key frames to animate */
    val frames: List<KeyFrameType>,
    /** The order of each frame should be played based on the indices of [frames]. */
    val frameIndices: List<Int>,
    /** The amount of time spent displaying each frame. */
    val frameTimes: List<Duration>,
) {
    /** The id of this animation. */
    val id = nextAnimId++

    /** The amount of frames in the stack. Alias for `frames.size`. */
    val frameStackSize: Int
        get() = frames.size

    /** The total frames to play. Alias for `frameIndices.size`. */
    val totalFrames: Int
        get() = frameIndices.size

    /** The first frame in the stack. Alias for `frames[0]`. */
    val firstFrame: KeyFrameType
        get() = frames[0]

    /** The total duration of this animation */
    val duration =
        if (frameTimes.isEmpty()) Duration.ZERO else frameTimes.reduce { acc, ft -> acc + ft }

    /**
     * @param time the elapsed time of the animation.
     * @return the frame that corresponds the time elapsed
     */
    fun getFrame(time: Duration): KeyFrameType {
        var counter = time
        var index = 0
        while (counter > frameTimes[index] && index < frameIndices.size) {
            counter -= frameTimes[index]
            index++
        }
        return getFrame(index)
    }

    /**
     * @param index the index of the frame.
     * @return the frame that corresponds to index.
     */
    fun getFrame(index: Int): KeyFrameType = frames[frameIndices[index umod frameIndices.size] umod frames.size]

    /**
     * @param index the index of the frame.
     * @return the frame time of the frame.
     */
    fun getFrameTime(index: Int): Duration = frameTimes[frameIndices[index umod frameIndices.size] umod frameTimes.size]

    /**
     * @param index the index of the key frame
     * @return the key frame at the specified index
     */
    operator fun get(index: Int) = frames[index]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Animation<*>

        return id == other.id
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
 *
 * @see [Animation]
 * @see [AnimationPlayer]
 */
class AnimationBuilder<T>(private val frames: List<T>) {

    private val frameIndices = arrayListOf<Int>()
    private val frameTimes = arrayListOf<Duration>()

    /**
     * Add a list of frame indices based of the list of [frames] to be used in the animation.
     *
     * ```
     * frames(indices = 2..5) // adds frames 2 through 5 once
     * frames(indices = 2..5, repeats = 1) // adds frames 2 through 5 twice
     * ```
     *
     * @param indices range of indices that correspond to the frame in [frames]
     * @param repeats number of times this repeats
     * @param frameTime time to display each frame
     */
    fun frames(
        indices: IntRange = 0..frames.size,
        repeats: Int = 0,
        frameTime: Duration = 100.milliseconds
    ) {
        repeat(repeats + 1) {
            frameIndices.addAll(indices)
            repeat(indices.count()) { frameTimes += frameTime }
        }
    }

    /**
     * Add a single frame index based of the list of [frames] to be used in the animation.
     *
     * ```
     * frames(index = 3) // adds frame 3 once
     * frames(indices = 3, repeats = 1) // adds frame 3 twice
     * ```
     *
     * @param index index that correspond to the frame in [frames]
     * @param repeats number of times this repeats
     * @param frameTime time to display each frame
     */
    fun frames(index: Int = 0, repeats: Int = 0, frameTime: Duration = 100.milliseconds) =
        frames(index..index, repeats, frameTime)

    /**
     * Builds the complete [Animation].
     *
     * @return newly created [Animation].
     */
    fun build(): Animation<T> = Animation(frames, frameIndices, frameTimes)
}

/**
 * Creates an animation from a prefix based on the names of the sprites in a [TextureAtlas].
 *
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
 * Create an animation from a prefix of the names of the sprites in a [TextureAtlas] using an
 * [AnimationBuilder] to specify the frame index orders and times.
 *
 * @param prefix the prefix to use to create the animation
 * @param action the [AnimationBuilder] callback to build the animation
 */
fun TextureAtlas.createAnimation(
    prefix: String,
    action: AnimationBuilder<TextureSlice>.() -> Unit
) =
    AnimationBuilder(entries.filter { it.name.startsWith(prefix) }.map { it.slice })
        .apply(action)
        .build()
