package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.GlFrameBuffer
import com.lehaine.littlekt.graphics.gl.GlRenderBuffer
import com.lehaine.littlekt.io.createUint32Buffer
import com.lehaine.littlekt.math.MutableVec4i

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
class FrameBuffer(private val application: Application) : Disposable {
    private val gl: GL get() = application.gl

    private var fboHandler: GlFrameBuffer = gl.createFrameBuffer()
    private var depthBufferHandle: GlRenderBuffer? = null
    private var stencilBufferHandle: GlRenderBuffer? = null
    private var depthStencilPackedBufferHandle: GlRenderBuffer? = null

    private var previousFBO: GlFrameBuffer? = null
    private val previousViewport = MutableVec4i()
    private var isBound = false

    fun begin() {
        check(!isBound) { "end() must be called before another draw can begin." }
        isBound = true
        previousFBO = getBoundFrameBuffer(application)
        gl.bindFrameBuffer(fboHandler)
        getViewport(application, previousViewport)
    }

    fun end() {
        check(isBound) { "begin() must be called first!" }
        isBound = false
        val currentFbo = getBoundFrameBuffer(application)
        check(currentFbo == fboHandler) {
            "The current bound framebuffer ($currentFbo) doesn't match this one. " +
                    "Ensure that the frame buffers are closed in the same order they were opened in."
        }
        previousFBO?.let {
            gl.bindFrameBuffer(it)
        }
        gl.viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3])
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }


    companion object {
        /**
         * Internal buffer used to handle checking for current bound frame buffer and viewports.
         * Max size of 64 bytes required as at most 16 integer elements can be returned.
         */
        private val intBuffer = createUint32Buffer(16 * Int.SIZE_BYTES)

        private fun getBoundFrameBuffer(application: Application): GlFrameBuffer {
            return application.gl.getBoundFrameBuffer(intBuffer)
        }

        private fun getViewport(application: Application, result: MutableVec4i) {
            application.gl.getIntegerv(GL.VIEWPORT, intBuffer)
            result[0] = intBuffer[0]
            result[1] = intBuffer[1]
            result[2] = intBuffer[2]
            result[3] = intBuffer[3]
        }
    }
}