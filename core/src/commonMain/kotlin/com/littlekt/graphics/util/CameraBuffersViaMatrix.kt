package com.littlekt.graphics.util

import com.littlekt.math.Mat4
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/13/2025
 */
interface CameraBuffersViaMatrix : CameraBuffers {

    fun update(viewProj: Mat4, dt: Duration = Duration.ZERO, dynamicOffset: Long = 0)
}
