package com.littlekt

import com.littlekt.util.toString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Application runtime statistics.
 *
 * @author Colton Daily
 * @date 11/27/2021
 */
class AppStats {

    private val frameTimes = DoubleArray(25) { 0.017 }

    /**
     * Run time of this render context in seconds. This is the wall clock time between now and the
     * first time render() was called.
     */
    var runTime: Duration = 0.seconds
        private set

    /** Time between current and last call of render() in a [Duration]. */
    var dt: Duration = 0.seconds
        private set

    /** Number of rendered frames. */
    var frames: Int = 0
        private set

    /** Frames per second (averaged over last 25 frames) */
    var fps: Double = 0.0
        private set

    /**
     * Internal render function that only handles updates the run time, frame time, and fps values.
     */
    internal fun update(dt: Duration) {
        frames++
        runTime += dt
        this.dt = dt

        frameTimes[frames % frameTimes.size] = dt.toDouble(DurationUnit.SECONDS)
        var sum = 0.0
        for (i in frameTimes.indices) {
            sum += frameTimes[i]
        }
        fps = (frameTimes.size / sum) * 0.1 + fps * 0.9
    }

    override fun toString(): String {
        return buildString {
            appendLine("***************** APP STATS *****************")
            appendLine("FPS(last 25 frames): ${ fps.toString(1) }")
            appendLine("Run time : ${ runTime.toInt(DurationUnit.SECONDS) }s")
            appendLine(EngineStats.statsString())
            append("****************** END APP STATS *************")
        }
    }
}
