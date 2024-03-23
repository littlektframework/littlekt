package com.lehaine.littlekt.graphics.gl

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlFrameBuffer(var reference: Int) {

    /**
     * Copy another [GlFrameBuffer].
     * @param glFrameBuffer the frame buffer to copy from
     */
    actual fun copy(glFrameBuffer: GlFrameBuffer): GlFrameBuffer {
        reference = glFrameBuffer.reference
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlFrameBuffer

        return reference == other.reference
    }

    override fun hashCode(): Int {
        return reference
    }

    override fun toString(): String {
        return "GlFrameBuffer(reference=$reference)"
    }

    actual companion object {
        /**
         * Generate an empty [GlFrameBuffer].
         */
        actual fun EmptyGlFrameBuffer(): GlFrameBuffer = GlFrameBuffer(-1)
    }
}