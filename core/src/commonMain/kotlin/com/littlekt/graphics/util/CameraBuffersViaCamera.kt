package com.littlekt.graphics.util

import com.littlekt.graphics.Camera
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/13/2025
 */
interface CameraBuffersViaCamera : CameraBuffers {

    fun update(camera: Camera, dt: Duration = Duration.ZERO, dynamicOffset: Long = 0)
}
