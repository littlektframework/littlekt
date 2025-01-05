package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.util.CameraBuffers
import kotlinx.atomicfu.atomic

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
open class Environment(val buffers: CameraBuffers) {
    val id = nextId()

    fun updateCameraBuffers(camera: Camera) = buffers.updateCameraUniform(camera)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Environment

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        private var lastId by atomic(0)

        fun nextId() = lastId++
    }
}
