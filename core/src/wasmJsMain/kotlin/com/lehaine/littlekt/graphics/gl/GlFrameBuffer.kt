package com.lehaine.littlekt.graphics.gl

import org.khronos.webgl.WebGLFramebuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlFrameBuffer(var delegate: WebGLFramebuffer?) {

    actual fun copy(glFrameBuffer: GlFrameBuffer): GlFrameBuffer {
        delegate = glFrameBuffer.delegate
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GlFrameBuffer

        return delegate == other.delegate
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String {
        return "GlFrameBuffer(delegate=$delegate)"
    }

    actual companion object {
        /**
         * Generate an empty [GlFrameBuffer].
         */
        actual fun EmptyGlFrameBuffer(): GlFrameBuffer = GlFrameBuffer(null)
    }
}