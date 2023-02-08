package com.lehaine.littlekt

import com.lehaine.littlekt.file.*
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.HdpiMode
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer as NioByteBuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class LwjglGL(private val engineStats: EngineStats, private val graphics: Graphics, private val hdpiMode: HdpiMode) :
    GL {
    internal var glVersion: GLVersion = GLVersion(Context.Platform.DESKTOP)
    override val version: GLVersion get() = glVersion

    private var lastBoundBuffer: GlBuffer? = null
    private var lastBoundShader: GlShaderProgram? = null
    private var lastBoundTexture: GlTexture? = null

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        engineStats.calls++
        glClearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        engineStats.calls++
        glClear(mask)
    }

    override fun clearDepth(depth: Float) {
        engineStats.calls++
        glClearDepth(depth.toDouble())
    }

    override fun clearStencil(stencil: Int) {
        engineStats.calls++
        glClearStencil(stencil)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        engineStats.calls++
        glColorMask(red, green, blue, alpha)
    }

    override fun cullFace(mode: Int) {
        engineStats.calls++
        glCullFace(mode)
    }

    override fun enable(cap: Int) {
        engineStats.calls++
        glEnable(cap)
    }

    override fun disable(cap: Int) {
        engineStats.calls++
        glDisable(cap)
    }

    override fun finish() {
        engineStats.calls++
        glFinish()
    }

    override fun flush() {
        engineStats.calls++
        glFlush()
    }

    override fun frontFace(mode: Int) {
        engineStats.calls++
        glFrontFace(mode)
    }

    override fun getError(): Int {
        engineStats.calls++
        return glGetError()
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        engineStats.calls++
        glBlendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        engineStats.calls++
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun stencilFunc(func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        glStencilFunc(func, ref, mask)
    }

    override fun stencilMask(mask: Int) {
        engineStats.calls++
        glStencilMask(mask)
    }

    override fun stencilOp(fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        glStencilOp(fail, zfail, zpass)
    }

    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        glStencilFuncSeparate(face, func, ref, mask)
    }

    override fun stencilMaskSeparate(face: Int, mask: Int) {
        engineStats.calls++
        glStencilMaskSeparate(face, mask)
    }

    override fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        glStencilOpSeparate(face, fail, zfail, zpass)
    }

    override fun createProgram(): GlShaderProgram {
        engineStats.calls++
        return GlShaderProgram(glCreateProgram())
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        engineStats.calls++
        return glGetAttribLocation(glShaderProgram.address, name)
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        engineStats.calls++
        return UniformLocation(glGetUniformLocation(glShaderProgram.address, name))
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        glAttachShader(glShaderProgram.address, glShader.address)
    }

    override fun detachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        glDetachShader(glShaderProgram.address, glShader.address)
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        glLinkProgram(glShaderProgram.address)
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        glDeleteProgram(glShaderProgram.address)
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any {
        engineStats.calls++
        return glGetProgrami(glShaderProgram.address, pname)
    }

    override fun getShaderParameter(glShader: GlShader, pname: Int): Any {
        engineStats.calls++
        return glGetShaderi(glShader.address, pname)
    }

    override fun getProgramParameterB(glShaderProgram: GlShaderProgram, pname: Int): Boolean {
        engineStats.calls++
        return (getProgramParameter(glShaderProgram, pname) as? Int) == 1
    }

    override fun getString(pname: Int): String? {
        engineStats.calls++
        return glGetString(pname)
    }

    override fun hint(target: Int, mode: Int) {
        engineStats.calls++
        glHint(target, mode)
    }

    override fun lineWidth(width: Float) {
        engineStats.calls++
        glLineWidth(width)
    }

    override fun polygonOffset(factor: Float, units: Float) {
        engineStats.calls++
        glPolygonOffset(factor, units)
    }

    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        engineStats.calls++
        glBlendColor(red, green, blue, alpha)
    }

    override fun blendEquation(mode: Int) {
        engineStats.calls++
        glBlendEquation(mode)
    }

    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        engineStats.calls++
        glBlendEquationSeparate(modeRGB, modeAlpha)
    }

    override fun getIntegerv(pname: Int, data: IntBuffer) {
        engineStats.calls++
        data as IntBufferImpl
        glGetIntegerv(pname, data.buffer)
    }

    override fun getBoundFrameBuffer(data: IntBuffer): GlFrameBuffer {
        getIntegerv(GL.FRAMEBUFFER_BINDING, data)
        return GlFrameBuffer(data[0])
    }

    override fun getShaderParameterB(glShader: GlShader, pname: Int): Boolean {
        engineStats.calls++
        return (getShaderParameter(glShader, pname) as? Int) == 1
    }

    override fun createShader(type: Int): GlShader {
        engineStats.calls++
        return GlShader(glCreateShader(type))
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        engineStats.calls++
        glShaderSource(glShader.address, source)
    }

    override fun compileShader(glShader: GlShader) {
        engineStats.calls++
        glCompileShader(glShader.address)
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        engineStats.calls++
        return glGetShaderInfoLog(glShader.address)
    }

    override fun deleteShader(glShader: GlShader) {
        engineStats.calls++
        glDeleteShader(glShader.address)
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        engineStats.calls++
        return glGetProgramInfoLog(glShader.address)
    }

    override fun createBuffer(): GlBuffer {
        engineStats.calls++
        return GlBuffer(glGenBuffers())
    }

    override fun createFrameBuffer(): GlFrameBuffer {
        engineStats.calls++
        return if (version.major >= 3) {
            GlFrameBuffer(glGenFramebuffers())
        } else {
            GlFrameBuffer(EXTFramebufferObject.glGenFramebuffersEXT())
        }
    }

    override fun createVertexArray(): GlVertexArray {
        engineStats.calls++
        return GlVertexArray(glGenVertexArrays())
    }

    override fun bindVertexArray(glVertexArray: GlVertexArray) {
        engineStats.calls++
        glBindVertexArray(glVertexArray.address)
    }

    override fun bindDefaultVertexArray() {
        engineStats.calls++
        glBindVertexArray(GL.NONE)
    }

    override fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        if (version.major >= 3) {
            glBindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.reference)
        } else {
            EXTFramebufferObject.glBindFramebufferEXT(GL.FRAMEBUFFER, glFrameBuffer.reference)
        }
    }

    override fun bindDefaultFrameBuffer() {
        engineStats.calls++
        if (version.major >= 3) {
            glBindFramebuffer(GL.FRAMEBUFFER, GL.NONE)
        } else {
            EXTFramebufferObject.glBindFramebufferEXT(GL.FRAMEBUFFER, GL.NONE)
        }
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        engineStats.calls++
        return if (version.major >= 3) {
            GlRenderBuffer(glGenRenderbuffers())
        } else {
            GlRenderBuffer(EXTFramebufferObject.glGenRenderbuffersEXT())
        }
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        if (version.major >= 3) {
            glBindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.reference)
        } else {
            EXTFramebufferObject.glBindRenderbufferEXT(GL.RENDERBUFFER, glRenderBuffer.reference)
        }
    }

    override fun bindDefaultRenderBuffer() {
        engineStats.calls++
        if (version.major >= 3) {
            glBindRenderbuffer(GL.RENDERBUFFER, GL.NONE)
        } else {
            EXTFramebufferObject.glBindRenderbufferEXT(GL.RENDERBUFFER, GL.NONE)
        }
    }

    override fun renderBufferStorage(internalFormat: RenderBufferInternalFormat, width: Int, height: Int) {
        engineStats.calls++
        if (version.major >= 3) {
            glRenderbufferStorage(GL.RENDERBUFFER, internalFormat.glFlag, width, height)
        } else {
            EXTFramebufferObject.glRenderbufferStorageEXT(GL.RENDERBUFFER, internalFormat.glFlag, width, height)
        }
    }

    override fun frameBufferRenderBuffer(
        attachementType: FrameBufferRenderBufferAttachment, glRenderBuffer: GlRenderBuffer,
    ) {
        engineStats.calls++
        if (version.major >= 3) {
            glFramebufferRenderbuffer(
                GL.FRAMEBUFFER, attachementType.glFlag, GL.RENDERBUFFER, glRenderBuffer.reference
            )
        } else {
            EXTFramebufferObject.glFramebufferRenderbufferEXT(
                GL.FRAMEBUFFER, attachementType.glFlag, GL.RENDERBUFFER, glRenderBuffer.reference
            )
        }
    }

    override fun deleteFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        if (version.major >= 3) {
            glDeleteFramebuffers(glFrameBuffer.reference)
        } else {
            EXTFramebufferObject.glDeleteFramebuffersEXT(glFrameBuffer.reference)
        }
    }

    override fun deleteRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        if (version.major >= 3) {
            glDeleteRenderbuffers(glRenderBuffer.reference)
        } else {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(glRenderBuffer.reference)
        }
    }

    override fun frameBufferTexture2D(
        attachementType: FrameBufferRenderBufferAttachment, glTexture: GlTexture, level: Int,
    ) {
        engineStats.calls++
        if (version.major >= 3) {
            glFramebufferTexture2D(
                GL.FRAMEBUFFER, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
            )
        } else {
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                GL.FRAMEBUFFER, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
            )
        }
    }

    override fun frameBufferTexture2D(
        target: Int, attachementType: FrameBufferRenderBufferAttachment, glTexture: GlTexture, level: Int,
    ) {
        engineStats.calls++
        if (version.major >= 3) {
            glFramebufferTexture2D(
                target, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
            )
        } else {
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                target, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
            )
        }
    }

    override fun readBuffer(mode: Int) {
        engineStats.calls++
        glReadBuffer(mode)
    }

    override fun checkFrameBufferStatus(): FrameBufferStatus {
        engineStats.calls++
        return if (version.major >= 3) {
            FrameBufferStatus(glCheckFramebufferStatus(GL.FRAMEBUFFER))
        } else {
            FrameBufferStatus(EXTFramebufferObject.glCheckFramebufferStatusEXT(GL.FRAMEBUFFER))
        }
    }

    override fun bindBuffer(target: Int, glBuffer: GlBuffer) {
        engineStats.calls++
        lastBoundBuffer = glBuffer
        glBindBuffer(target, glBuffer.address)
    }

    override fun bindDefaultBuffer(target: Int) {
        engineStats.calls++
        lastBoundBuffer = null
        glBindBuffer(target, GL.NONE)
    }

    override fun deleteBuffer(glBuffer: GlBuffer) {
        engineStats.calls++
        if (lastBoundBuffer == glBuffer) {
            lastBoundBuffer = null
        }
        glDeleteBuffers(glBuffer.address)
    }

    override fun bufferData(target: Int, data: Buffer, usage: Int) {
        engineStats.calls++
        val limit = data.limit
        val pos = data.position
        data.position = 0
        data.limit = data.capacity
        engineStats.bufferDeleted(lastBoundBuffer!!.bufferId)
        when (data) {
            is FloatBuffer -> {
                data as FloatBufferImpl
                glBufferData(target, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity * 4)
            }

            is ByteBuffer -> {
                data as ByteBufferImpl
                glBufferData(target, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity)
            }

            is ShortBuffer -> {
                data as ShortBufferImpl
                glBufferData(target, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity * 2)
            }

            is IntBuffer -> {
                data as IntBufferImpl
                glBufferData(target, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity * 4)
            }
        }

        data.limit = limit
        data.position = pos
    }

    override fun bufferSubData(target: Int, offset: Int, data: Buffer) {
        engineStats.calls++
        val limit = data.limit
        val pos = data.position
        data.position = 0
        data.limit = data.capacity
        when (data) {
            is FloatBuffer -> {
                data as FloatBufferImpl
                GL15.glBufferSubData(target, offset.toLong(), data.buffer)
            }

            is ByteBuffer -> {
                data as ByteBufferImpl
                GL15.glBufferSubData(target, offset.toLong(), data.buffer)
            }

            is ShortBuffer -> {
                data as ShortBufferImpl
                GL15.glBufferSubData(target, offset.toLong(), data.buffer)
            }

            is IntBuffer -> {
                data as IntBufferImpl
                GL15.glBufferSubData(target, offset.toLong(), data.buffer)
            }
        }

        data.limit = limit
        data.position = pos
    }

    override fun depthFunc(func: Int) {
        engineStats.calls++
        glDepthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        engineStats.calls++
        glDepthMask(flag)
    }

    override fun depthRangef(zNear: Float, zFar: Float) {
        engineStats.calls++
        glDepthRange(zNear.toDouble(), zFar.toDouble())
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        engineStats.calls++
        glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
    }

    override fun enableVertexAttribArray(index: Int) {
        engineStats.calls++
        glEnableVertexAttribArray(index)
    }

    override fun disableVertexAttribArray(index: Int) {
        engineStats.calls++
        glDisableVertexAttribArray(index)
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        if (lastBoundShader != glShaderProgram) {
            engineStats.calls++
            engineStats.shaderSwitches++
            lastBoundShader = glShaderProgram
            glUseProgram(glShaderProgram.address)
        } else if (engineStats.shaderSwitches == 0) {
            engineStats.shaderSwitches = 1
        }
    }

    override fun validateProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        glValidateProgram(glShaderProgram.address)
    }

    override fun useDefaultProgram() {
        if (lastBoundShader != null) {
            lastBoundBuffer = null
            engineStats.calls++
            engineStats.shaderSwitches++
            glUseProgram(0)
        }
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++
        if (hdpiMode == HdpiMode.LOGICAL && (graphics.width != graphics.backBufferWidth || graphics.height != graphics.backBufferHeight)) {
            with(graphics) {
                glScissor(x.toBackBufferX, y.toBackBufferY, width.toBackBufferX, height.toBackBufferY)
            }
        } else {
            glScissor(x, y, width, height)
        }
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat3) {
        engineStats.calls++
        val buffer = createFloatBuffer(19) as FloatBufferImpl
        data.toBuffer(buffer)
        glUniformMatrix3fv(uniformLocation.address, transpose, buffer.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
        glUniformMatrix3fv(uniformLocation.address, transpose, data.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        glUniformMatrix3fv(uniformLocation.address, transpose, data.toFloatArray())
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4) {
        engineStats.calls++
        val buffer = createFloatBuffer(16) as FloatBufferImpl
        data.toBuffer(buffer)
        glUniformMatrix4fv(uniformLocation.address, transpose, buffer.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
        glUniformMatrix4fv(uniformLocation.address, transpose, data.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        glUniformMatrix4fv(uniformLocation.address, transpose, data.toFloatArray())
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        engineStats.calls++
        glUniform1i(uniformLocation.address, data)
    }

    override fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int) {
        engineStats.calls++
        glUniform2i(uniformLocation.address, x, y)
    }

    override fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int) {
        engineStats.calls++
        glUniform3i(uniformLocation.address, x, y, z)
    }

    override fun uniform1f(uniformLocation: UniformLocation, x: Float) {
        engineStats.calls++
        glUniform1f(uniformLocation.address, x)
    }

    override fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float) {
        engineStats.calls++
        glUniform2f(uniformLocation.address, x, y)
    }

    override fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float) {
        engineStats.calls++
        glUniform3f(uniformLocation.address, x, y, z)
    }

    override fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float) {
        engineStats.calls++
        glUniform4f(uniformLocation.address, x, y, z, w)
    }

    override fun uniform1fv(uniformLocation: UniformLocation, floats: FloatArray) {
        engineStats.calls++
        GL20.glUniform1fv(uniformLocation.address, floats)
    }

    override fun uniform2fv(uniformLocation: UniformLocation, floats: FloatArray) {
        engineStats.calls++
        GL20.glUniform2fv(uniformLocation.address, floats)
    }

    override fun uniform3fv(uniformLocation: UniformLocation, floats: FloatArray) {
        engineStats.calls++
        GL20.glUniform3fv(uniformLocation.address, floats)
    }

    override fun uniform4fv(uniformLocation: UniformLocation, floats: FloatArray) {
        engineStats.calls++
        GL20.glUniform4fv(uniformLocation.address, floats)
    }

    override fun drawArrays(mode: Int, offset: Int, count: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        glDrawArrays(mode, offset, count)
    }

    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        glDrawElements(mode, count, type, offset.toLong())
    }

    override fun pixelStorei(pname: Int, param: Int) {
        engineStats.calls++
        glPixelStorei(pname, param)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++

        // handle hdpi related viewports here as well
        if (hdpiMode == HdpiMode.LOGICAL && (graphics.width != graphics.backBufferWidth || graphics.height != graphics.backBufferHeight)) {
            with(graphics) {
                glViewport(x.toBackBufferX, y.toBackBufferY, width.toBackBufferX, height.toBackBufferY)
            }
        } else {
            glViewport(x, y, width, height)
        }
    }

    override fun createTexture(): GlTexture {
        engineStats.calls++
        return GlTexture(glGenTextures())
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        if (lastBoundTexture != glTexture) {
            engineStats.calls++
            engineStats.textureBindings++
            lastBoundTexture = glTexture
            glBindTexture(target, glTexture.reference)
        } else if (engineStats.textureBindings == 0) {
            engineStats.textureBindings = 1
        }
    }

    override fun bindDefaultTexture(target: TextureTarget) {
        if (lastBoundTexture != null) {
            engineStats.calls++
            lastBoundTexture = null
            glBindTexture(target.glFlag, GL.NONE)
        }
    }

    override fun deleteTexture(glTexture: GlTexture) {
        engineStats.calls++
        glDeleteTextures(glTexture.reference)
    }

    override fun compressedTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        source: ByteBuffer?,
    ) {
        engineStats.calls++
        if (source != null) {
            source as ByteBufferImpl
            GL13.glCompressedTexImage2D(target, level, internalFormat, width, height, 0, source.buffer)
        } else {
            GL13.glCompressedTexImage2D(target, level, internalFormat, width, height, 0, null)
        }
    }

    override fun compressedTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        GL13.glCompressedTexSubImage2D(target, level, xOffset, yOffset, width, height, format, source.buffer)
    }

    override fun copyTexImage2D(
        target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int,
    ) {
        engineStats.calls++
        glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border)
    }

    override fun copyTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        engineStats.calls++
        glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
    }

    override fun texSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        GL11.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, source.buffer)
    }

    override fun texImage2D(
        target: Int, level: Int, internalFormat: Int, format: Int, width: Int, height: Int, type: Int,
    ) {
        engineStats.calls++
        glTexImage2D(
            target, level, internalFormat, width, height, 0, format, type, null as NioByteBuffer?
        )
    }

    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        val pos = source.position
        source.position = 0
        glTexImage2D(
            target, level, internalFormat, width, height, 0, format, type, source.buffer
        )
        source.position = pos
    }

    override fun compressedTexImage3D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        depth: Int,
        source: ByteBuffer?,
    ) {
        engineStats.calls++
        if (source != null) {
            source as ByteBufferImpl
            GL13.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, 0, source.buffer)
        } else {
            GL13.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, 0, null)
        }
    }

    override fun compressedTexSubImage3D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        GL13.glCompressedTexSubImage3D(
            target, level, xOffset, yOffset, zOffset, width, height, depth, format, source.buffer
        )
    }

    override fun copyTexSubImage3D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        engineStats.calls++
        glCopyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height)
    }

    override fun texSubImage3D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        zOffset: Int,
        width: Int,
        height: Int,
        depth: Int,
        format: Int,
        type: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        GL12.glTexSubImage3D(
            target, level, xOffset, yOffset, zOffset, width, height, depth, format, type, source.buffer
        )
    }

    override fun texImage3D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        depth: Int,
        type: Int,
    ) {
        engineStats.calls++
        glTexImage3D(
            target, level, internalFormat, width, height, depth, 0, format, type, null as NioByteBuffer?
        )
    }

    override fun texImage3D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        depth: Int,
        type: Int,
        source: ByteBuffer,
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        val pos = source.position
        source.position = 0
        glTexImage3D(
            target, level, internalFormat, width, height, depth, 0, format, type, source.buffer
        )
        source.position = pos
    }

    override fun activeTexture(texture: Int) {
        engineStats.calls++
        glActiveTexture(texture)
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        engineStats.calls++
        glTexParameteri(target, pname, param)
    }

    override fun texParameterf(target: Int, pname: Int, param: Float) {
        engineStats.calls++
        glTexParameterf(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        engineStats.calls++
        glGenerateMipmap(target)
    }
}