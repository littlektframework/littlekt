package com.lehaine.littlekt

import com.lehaine.littlekt.util.TimeSpan
import com.lehaine.littlekt.util.milliseconds
import kotlin.native.concurrent.ThreadLocal

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
@ThreadLocal
object Time {

    /**
     * Total time the game has been running.
     */
    var totalTime = 0.milliseconds

    /**
     * The total time from the previous frame to the current, scaled by [timeScale].
     */
    var deltaTime = 0.milliseconds

    /**
     * The total time from the previous frame to the current, not affected by [timeScale].
     */
    var unscaledDeltaTime = 0.milliseconds

    /**
     * Secondary [deltaTime] for use when you need to scale two different deltas simultaneously.
     */
    var altDeltaTime = 0.milliseconds

    /**
     * Total time since the [Scene] was loaded.
     */
    var timeSinceSceneLoad = 0.milliseconds

    /**
     * Time scale of [deltaTime].
     */
    var timeScale = 1f

    /**
     * Time scale of [altDeltaTime].
     */
    var altTimeScale = 1f

    /**
     * Total number of frames that have passed.
     */
    var frameCount = 0

    var tmod = 1f

    internal fun update(dt: TimeSpan) {
        totalTime += dt
        deltaTime = dt * timeScale
        altDeltaTime = dt * altTimeScale
        unscaledDeltaTime = dt
        timeSinceSceneLoad += dt
        frameCount++
    }

    internal fun sceneChanged() {
        timeSinceSceneLoad = 0.milliseconds
    }

    /**
     * Allows to check in intervals. Should only be used with interval values above [deltaTime],
     * otherwise it will always return true.
     */
    fun checkEvery(interval: TimeSpan): Boolean {
        // we subtract deltaTime since timeSinceSceneLoad already includes this update ticks deltaTime
        return (timeSinceSceneLoad / interval).toInt() > ((timeSinceSceneLoad - deltaTime) / interval).toInt()
    }
}