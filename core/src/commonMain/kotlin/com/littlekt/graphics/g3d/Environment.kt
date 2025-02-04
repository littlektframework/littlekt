package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.util.UniqueId
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
open class Environment(open val buffers: CameraBuffersViaCamera) {
    val id = UniqueId.next<Environment>()

    open fun update(camera: Camera, dt: Duration) = buffers.update(camera, dt)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Environment

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}
