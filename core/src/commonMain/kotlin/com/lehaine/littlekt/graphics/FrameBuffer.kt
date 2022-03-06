package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.createIntBuffer
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.math.MutableVec4i

/**
 * Encapsulates OpenGL ES 2.0 frame buffer objects.
 * @param width the width of the framebuffer in pixels
 * @param height the height of the framebuffer in pixels
 * @param hasDepth whether to attach a depth buffer. Defaults to false.
 * @param hasStencil whether to attach a stencil buffer. Defaults to false.
 * @param format the format of the color buffer
 * @author Colton Daily
 * @date 11/25/2021
 */
class FrameBuffer(
    val width: Int,
    val height: Int,
    val hasDepth: Boolean = false,
    val hasStencil: Boolean = false,
    var hasPackedDepthStencil: Boolean = false,
    val format: Pixmap.Format = Pixmap.Format.RGBA8888,
    val minFilter: TexMinFilter = TexMinFilter.LINEAR,
    val magFilter: TexMagFilter = TexMagFilter.LINEAR
) : Preparable, Disposable {

    /**
     * Gets set when the frame buffer is prepared by the application
     */
    private lateinit var gl: GL

    private var fboHandle: GlFrameBuffer? = null
    private var depthBufferHandle: GlRenderBuffer? = null
    private var stencilBufferHandle: GlRenderBuffer? = null
    private var depthStencilPackedBufferHandle: GlRenderBuffer? = null

    private var previousFboHandle: GlFrameBuffer? = null
    private val previousViewport = MutableVec4i()
    private var isBound = false
    private var isPrepared = false

    var texture: Texture? = null
    val colorBufferTexture get() = texture!!

    override val prepared: Boolean
        get() = isPrepared


    override fun prepare(context: Context) {
        gl = context.gl
        val fboHandle = gl.createFrameBuffer()
        this.fboHandle = fboHandle

        gl.bindFrameBuffer(fboHandle)
        if (hasDepth) {
            depthBufferHandle = gl.createRenderBuffer()
            depthBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH_COMPONENT16, width, height)
            }
        }

        if (hasStencil) {
            stencilBufferHandle = gl.createRenderBuffer()
            stencilBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.STENCIL_INDEX8, width, height)
            }
        }

        if (hasPackedDepthStencil) {
            depthStencilPackedBufferHandle = gl.createRenderBuffer()
            depthStencilPackedBufferHandle?.let {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH24_STENCIL8, width, height)
            }
        }
        texture = Texture(GLTextureData(width, height, 0, format.glFormat, format.glFormat, format.glType)).apply {
            minFilter = this@FrameBuffer.minFilter
            magFilter = this@FrameBuffer.magFilter
            uWrap = TexWrap.CLAMP_TO_EDGE
            vWrap = TexWrap.CLAMP_TO_EDGE
        }.also { it.prepare(context) } // preparing the texture will also bind it

        texture?.glTexture?.let {
            gl.frameBufferTexture2D(FrameBufferRenderBufferAttachment.COLOR_ATTACHMENT(), it, 0)
        }

        depthBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_ATTACHMENT, it)
        }

        stencilBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.STENCIL_ATTACHMENT, it)
        }
        depthStencilPackedBufferHandle?.let {
            gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_STENCIL_ATTACHMENT, it)
        }

        gl.bindDefaultRenderBuffer()
        texture?.glTexture?.let {
            gl.bindTexture(TextureTarget._2D, it)
        }

        var result = gl.checkFrameBufferStatus()
        if (result == FrameBufferStatus.FRAMEBUFFER_UNSUPPORTED && hasDepth && hasStencil &&
            (context.graphics.supportsExtension("GL_OES_packed_depth_stencil")
                    || context.graphics.supportsExtension("GL_EXT_packed_depth_stencil"))
        ) {
            if (hasDepth) {
                depthBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                depthBufferHandle = null
            }
            if (hasStencil) {
                stencilBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                stencilBufferHandle = null
            }
            if (hasPackedDepthStencil) {
                depthStencilPackedBufferHandle?.let {
                    gl.deleteRenderBuffer(it)
                }
                depthStencilPackedBufferHandle = null
            }

            depthStencilPackedBufferHandle = gl.createRenderBuffer().also {
                gl.bindRenderBuffer(it)
                gl.renderBufferStorage(RenderBufferInternalFormat.DEPTH24_STENCIL8_OES, width, height)
                gl.bindDefaultRenderBuffer()
                gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.DEPTH_ATTACHMENT, it)
                gl.frameBufferRenderBuffer(FrameBufferRenderBufferAttachment.STENCIL_ATTACHMENT, it)
            }
            hasPackedDepthStencil = true
            result = gl.checkFrameBufferStatus()
        }

        gl.bindDefaultFrameBuffer()

        if (result != FrameBufferStatus.FRAMEBUFFER_COMPLETE) {
            dispose()

            when (result) {
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> throw IllegalStateException("Frame buffer couldn't be constructed: incomplete attachment")
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> throw IllegalStateException("Frame buffer couldn't be constructed: incomplete dimensions")
                FrameBufferStatus.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> throw IllegalStateException("Frame buffer couldn't be constructed: missing attachment")
                FrameBufferStatus.FRAMEBUFFER_UNSUPPORTED -> throw IllegalStateException("Frame buffer couldn't be constructed: unsupported combination of formats")
                else -> throw IllegalStateException("Frame buffer couldn't be constructed: unknown error ${result.glFlag}")
            }
        }
        isPrepared = true
    }

    fun begin() {
        val fboHandle = fboHandle
        check(isPrepared && fboHandle != null) { "The framebuffer has not been prepared yet! Ensure you called prepare() sometime before you call begin()" }
        check(!isBound) { "end() must be called before another draw can begin." }
        isBound = true

        previousFboHandle = getBoundFrameBuffer(gl)
        gl.bindFrameBuffer(fboHandle)

        getViewport(gl, previousViewport)
        gl.viewport(0, 0, width, height)
    }

    fun end() {
        val fboHandle = fboHandle
        check(isPrepared && fboHandle != null) { "The framebuffer has not been prepared yet! Ensure you called prepare() sometime before you call end()" }
        check(isBound) { "begin() must be called first!" }

        isBound = false
        val currentFbo = getBoundFrameBuffer(gl)
        check(currentFbo == fboHandle) {
            "The current bound framebuffer ($currentFbo) doesn't match this one. " +
                    "Ensure that the frame buffers are closed in the same order they were opened in."
        }
        val previousFboHandle = previousFboHandle
        check(previousFboHandle != null) { "The previous framebuffer object is null. That means it was not found for some unknown reason." }
        gl.bindFrameBuffer(previousFboHandle)
        gl.viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3])
    }

    override fun dispose() {
        texture?.dispose()

        if (hasDepth) {
            depthBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            depthBufferHandle = null
        }
        if (hasStencil) {
            stencilBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            stencilBufferHandle = null
        }
        if (hasPackedDepthStencil) {
            depthStencilPackedBufferHandle?.let {
                gl.deleteRenderBuffer(it)
            }
            depthStencilPackedBufferHandle = null
        }
        fboHandle?.let { gl.deleteFrameBuffer(it) }
    }


    companion object {
        /**
         * Internal buffer used to handle checking for current bound frame buffer and viewports.
         * Max size of 64 bytes required as at most 16 integer elements can be returned.
         */
        private val intBuffer = createIntBuffer(16 * Int.SIZE_BYTES)

        private fun getBoundFrameBuffer(gl: GL): GlFrameBuffer {
            return gl.getBoundFrameBuffer(intBuffer)
        }

        private fun getViewport(gl: GL, result: MutableVec4i) {
            gl.getIntegerv(GL.VIEWPORT, intBuffer)
            result[0] = intBuffer[0]
            result[1] = intBuffer[1]
            result[2] = intBuffer[2]
            result[3] = intBuffer[3]
        }
    }
}