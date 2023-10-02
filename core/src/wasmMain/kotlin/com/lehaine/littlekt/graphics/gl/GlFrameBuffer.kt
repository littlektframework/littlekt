package com.lehaine.littlekt.graphics.gl

import org.khronos.webgl.WebGLFramebuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlFrameBuffer(val delegate: WebGLFramebuffer?) {

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
}