package com.lehaine.littlekt.graphics.gl

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
expect class GlFrameBuffer {
    /**
     * Copy another [GlFrameBuffer].
     * @param glFrameBuffer the frame buffer to copy from
     */
    fun copy(glFrameBuffer: GlFrameBuffer): GlFrameBuffer

    companion object {
        /**
         * Generate an empty [GlFrameBuffer].
         */
        fun EmptyGlFrameBuffer(): GlFrameBuffer
    }
}