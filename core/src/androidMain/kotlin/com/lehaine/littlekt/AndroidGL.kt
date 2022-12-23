package com.lehaine.littlekt

import android.opengl.GLES10
import android.opengl.GLES11
import android.opengl.GLES20
import android.opengl.GLES30
import com.lehaine.littlekt.file.*
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidGL(private val engineStats: EngineStats) : GL {
    internal var glVersion: GLVersion = GLVersion(Context.Platform.ANDROID)
    override val version: GLVersion get() = glVersion

    private var lastBoundBuffer: GlBuffer? = null
    private var lastBoundShader: GlShaderProgram? = null
    private var lastBoundTexture: GlTexture? = null

    private val intBuffer = java.nio.IntBuffer.allocate(1)

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        engineStats.calls++
        GLES20.glClearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        engineStats.calls++
        GLES20.glClear(mask)
    }

    override fun clearDepth(depth: Float) {
        engineStats.calls++
        GLES20.glClearDepthf(depth)
    }

    override fun clearStencil(stencil: Int) {
        engineStats.calls++
        GLES20.glClearStencil(stencil)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        engineStats.calls++
        GLES20.glColorMask(red, green, blue, alpha)
    }

    override fun cullFace(mode: Int) {
        engineStats.calls++
        GLES20.glCullFace(mode)
    }

    override fun enable(cap: Int) {
        engineStats.calls++
        GLES20.glEnable(cap)
    }

    override fun disable(cap: Int) {
        engineStats.calls++
        GLES20.glDisable(cap)
    }

    override fun finish() {
        engineStats.calls++
        GLES20.glFinish()
    }

    override fun flush() {
        engineStats.calls++
        GLES20.glFlush()
    }

    override fun frontFace(mode: Int) {
        engineStats.calls++
        GLES20.glFrontFace(mode)
    }

    override fun getError(): Int {
        engineStats.calls++
        return GLES20.glGetError()
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        engineStats.calls++
        GLES20.glBlendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        engineStats.calls++
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun stencilFunc(func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        GLES20.glStencilFunc(func, ref, mask)
    }

    override fun stencilMask(mask: Int) {
        engineStats.calls++
        GLES20.glStencilMask(mask)
    }

    override fun stencilOp(fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        GLES20.glStencilOp(fail, zfail, zpass)
    }

    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        GLES20.glStencilFuncSeparate(face, func, ref, mask)
    }

    override fun stencilMaskSeparate(face: Int, mask: Int) {
        engineStats.calls++
        GLES20.glStencilMaskSeparate(face, mask)
    }

    override fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        GLES20.glStencilOpSeparate(face, fail, zfail, zpass)
    }

    override fun createProgram(): GlShaderProgram {
        engineStats.calls++
        return GlShaderProgram(GLES20.glCreateProgram())
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        engineStats.calls++
        return GLES20.glGetAttribLocation(glShaderProgram.address, name)
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        engineStats.calls++
        return UniformLocation(GLES20.glGetUniformLocation(glShaderProgram.address, name))
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        GLES20.glAttachShader(glShaderProgram.address, glShader.address)
    }

    override fun detachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        GLES20.glDetachShader(glShaderProgram.address, glShader.address)
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        GLES20.glLinkProgram(glShaderProgram.address)
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        GLES20.glDeleteProgram(glShaderProgram.address)
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGetProgramiv(glShaderProgram.address, pname, intBuffer)
        return intBuffer[0]
    }

    override fun getShaderParameter(glShader: GlShader, pname: Int): Any {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGetShaderiv(glShader.address, pname, intBuffer)
        return intBuffer[0]
    }

    override fun getProgramParameterB(glShaderProgram: GlShaderProgram, pname: Int): Boolean {
        engineStats.calls++
        return (getProgramParameter(glShaderProgram, pname) as? Int) == 1
    }

    override fun getString(pname: Int): String? {
        engineStats.calls++
        return GLES20.glGetString(pname)
    }

    override fun hint(target: Int, mode: Int) {
        engineStats.calls++
        GLES20.glHint(target, mode)
    }

    override fun lineWidth(width: Float) {
        engineStats.calls++
        GLES20.glLineWidth(width)
    }

    override fun polygonOffset(factor: Float, units: Float) {
        engineStats.calls++
        GLES20.glPolygonOffset(factor, units)
    }

    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        engineStats.calls++
        GLES20.glBlendColor(red, green, blue, alpha)
    }

    override fun blendEquation(mode: Int) {
        engineStats.calls++
        GLES20.glBlendEquation(mode)
    }

    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        engineStats.calls++
        GLES20.glBlendEquationSeparate(modeRGB, modeAlpha)
    }

    override fun getIntegerv(pname: Int, data: IntBuffer) {
        engineStats.calls++
        data as IntBufferImpl
        GLES20.glGetIntegerv(pname, data.buffer)
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
        return GlShader(GLES20.glCreateShader(type))
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        engineStats.calls++
        GLES20.glShaderSource(glShader.address, source)
    }

    override fun compileShader(glShader: GlShader) {
        engineStats.calls++
        GLES20.glCompileShader(glShader.address)
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        engineStats.calls++
        return GLES20.glGetShaderInfoLog(glShader.address)
    }

    override fun deleteShader(glShader: GlShader) {
        engineStats.calls++
        GLES20.glDeleteShader(glShader.address)
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        engineStats.calls++
        return GLES20.glGetProgramInfoLog(glShader.address)
    }

    override fun createBuffer(): GlBuffer {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGenBuffers(1, intBuffer)
        return GlBuffer(intBuffer[0])
    }

    override fun createFrameBuffer(): GlFrameBuffer {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGenFramebuffers(1, intBuffer)
        return GlFrameBuffer(intBuffer[0])
    }

    override fun createVertexArray(): GlVertexArray {
        engineStats.calls++
        intBuffer.position(0)
        GLES30.glGenVertexArrays(1, intBuffer)
        return GlVertexArray(intBuffer[0])
    }

    override fun bindVertexArray(glVertexArray: GlVertexArray) {
        engineStats.calls++
        GLES30.glBindVertexArray(glVertexArray.address)
    }

    override fun bindDefaultVertexArray() {
        engineStats.calls++
        GLES30.glBindVertexArray(GL.NONE)
    }

    override fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        GLES20.glBindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.reference)
    }

    override fun bindDefaultFrameBuffer() {
        engineStats.calls++
        GLES20.glBindFramebuffer(GL.FRAMEBUFFER, GL.NONE)
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGenRenderbuffers(1, intBuffer)
        return GlRenderBuffer(intBuffer[0])
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        GLES20.glBindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.reference)
    }

    override fun bindDefaultRenderBuffer() {
        engineStats.calls++
        GLES20.glBindRenderbuffer(GL.RENDERBUFFER, GL.NONE)
    }

    override fun renderBufferStorage(internalFormat: RenderBufferInternalFormat, width: Int, height: Int) {
        engineStats.calls++
        GLES20.glRenderbufferStorage(GL.RENDERBUFFER, internalFormat.glFlag, width, height)
    }

    override fun frameBufferRenderBuffer(
        attachementType: FrameBufferRenderBufferAttachment, glRenderBuffer: GlRenderBuffer,
    ) {
        engineStats.calls++
        GLES20.glFramebufferRenderbuffer(
            GL.FRAMEBUFFER, attachementType.glFlag, GL.RENDERBUFFER, glRenderBuffer.reference
        )
    }

    override fun deleteFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        intBuffer.put(0, glFrameBuffer.reference)
        intBuffer.position(0)
        GLES20.glDeleteFramebuffers(1, intBuffer)
    }

    override fun deleteRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        intBuffer.put(0, glRenderBuffer.reference)
        intBuffer.position(0)
        GLES20.glDeleteRenderbuffers(1, intBuffer)
    }

    override fun frameBufferTexture2D(
        attachementType: FrameBufferRenderBufferAttachment, glTexture: GlTexture, level: Int,
    ) {
        engineStats.calls++
        GLES20.glFramebufferTexture2D(
            GL.FRAMEBUFFER, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
        )
    }

    override fun frameBufferTexture2D(
        target: Int, attachementType: FrameBufferRenderBufferAttachment, glTexture: GlTexture, level: Int,
    ) {
        engineStats.calls++
        GLES20.glFramebufferTexture2D(
            target, attachementType.glFlag, GL.TEXTURE_2D, glTexture.reference, level
        )
    }

    override fun readBuffer(mode: Int) {
        engineStats.calls++
        GLES30.glReadBuffer(mode)
    }

    override fun checkFrameBufferStatus(): FrameBufferStatus {
        engineStats.calls++
        return FrameBufferStatus(GLES20.glCheckFramebufferStatus(GL.FRAMEBUFFER))
    }

    override fun bindBuffer(target: Int, glBuffer: GlBuffer) {
        engineStats.calls++
        lastBoundBuffer = glBuffer
        GLES11.glBindBuffer(target, glBuffer.address)
    }

    override fun bindDefaultBuffer(target: Int) {
        engineStats.calls++
        lastBoundBuffer = null
        GLES11.glBindBuffer(target, GL.NONE)
    }

    override fun deleteBuffer(glBuffer: GlBuffer) {
        engineStats.calls++
        if (lastBoundBuffer == glBuffer) {
            lastBoundBuffer = null
        }
        intBuffer.put(0, glBuffer.address)
        intBuffer.position(0)
        GLES11.glDeleteBuffers(1, intBuffer)
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
                GLES11.glBufferData(target, data.capacity * 4, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity * 4)
            }

            is ByteBuffer -> {
                data as ByteBufferImpl
                GLES11.glBufferData(target, data.capacity, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity)
            }

            is ShortBuffer -> {
                data as ShortBufferImpl
                GLES11.glBufferData(target, data.capacity * 2, data.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.capacity * 2)
            }

            is IntBuffer -> {
                data as IntBufferImpl
                GLES11.glBufferData(target, data.capacity * 4, data.buffer, usage)
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
                GLES11.glBufferSubData(target, offset, data.capacity * 4, data.buffer)
            }

            is ByteBuffer -> {
                data as ByteBufferImpl
                GLES11.glBufferSubData(target, offset, data.capacity, data.buffer)
            }

            is ShortBuffer -> {
                data as ShortBufferImpl
                GLES11.glBufferSubData(target, offset, data.capacity * 2, data.buffer)
            }

            is IntBuffer -> {
                data as IntBufferImpl
                GLES11.glBufferSubData(target, offset, data.capacity * 4, data.buffer)
            }
        }

        data.limit = limit
        data.position = pos
    }

    override fun depthFunc(func: Int) {
        engineStats.calls++
        GLES10.glDepthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        engineStats.calls++
        GLES10.glDepthMask(flag)
    }

    override fun depthRangef(zNear: Float, zFar: Float) {
        engineStats.calls++
        GLES10.glDepthRangef(zNear, zFar)
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        engineStats.calls++
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    override fun enableVertexAttribArray(index: Int) {
        engineStats.calls++
        GLES20.glEnableVertexAttribArray(index)
    }

    override fun disableVertexAttribArray(index: Int) {
        engineStats.calls++
        GLES20.glDisableVertexAttribArray(index)
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        if (lastBoundShader != glShaderProgram) {
            engineStats.calls++
            engineStats.shaderSwitches++
            lastBoundShader = glShaderProgram
            GLES20.glUseProgram(glShaderProgram.address)
        } else if (engineStats.shaderSwitches == 0) {
            engineStats.shaderSwitches = 1
        }
    }

    override fun validateProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        GLES20.glValidateProgram(glShaderProgram.address)
    }

    override fun useDefaultProgram() {
        if (lastBoundShader != null) {
            engineStats.calls++
            engineStats.shaderSwitches++
            lastBoundShader = null
            GLES20.glUseProgram(0)
        }
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++
        GLES20.glScissor(x, y, width, height)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat3) {
        engineStats.calls++
        val buffer = createFloatBuffer(19) as FloatBufferImpl
        data.toBuffer(buffer)
        GLES20.glUniformMatrix3fv(uniformLocation.address, 1, transpose, buffer.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
        GLES20.glUniformMatrix3fv(uniformLocation.address, 1, transpose, data.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        GLES20.glUniformMatrix3fv(uniformLocation.address, 1, transpose, data.toFloatArray(), 0)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4) {
        engineStats.calls++
        val buffer = createFloatBuffer(16) as FloatBufferImpl
        data.toBuffer(buffer)
        GLES20.glUniformMatrix4fv(uniformLocation.address, 1, transpose, buffer.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
        GLES20.glUniformMatrix4fv(uniformLocation.address, 1, transpose, data.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        GLES20.glUniformMatrix4fv(uniformLocation.address, data.size, transpose, data.toFloatArray(), 0)
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        engineStats.calls++
        GLES20.glUniform1i(uniformLocation.address, data)
    }

    override fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int) {
        engineStats.calls++
        GLES20.glUniform2i(uniformLocation.address, x, y)
    }

    override fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int) {
        engineStats.calls++
        GLES20.glUniform3i(uniformLocation.address, x, y, z)
    }

    override fun uniform1f(uniformLocation: UniformLocation, x: Float) {
        engineStats.calls++
        GLES20.glUniform1f(uniformLocation.address, x)
    }

    override fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float) {
        engineStats.calls++
        GLES20.glUniform2f(uniformLocation.address, x, y)
    }

    override fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float) {
        engineStats.calls++
        GLES20.glUniform3f(uniformLocation.address, x, y, z)
    }

    override fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float) {
        engineStats.calls++
        GLES20.glUniform4f(uniformLocation.address, x, y, z, w)
    }

    override fun drawArrays(mode: Int, offset: Int, count: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        GLES20.glDrawArrays(mode, offset, count)
    }

    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        GLES20.glDrawElements(mode, count, type, offset)
    }

    override fun pixelStorei(pname: Int, param: Int) {
        engineStats.calls++
        GLES20.glPixelStorei(pname, param)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++
        GLES20.glViewport(x, y, width, height)
    }

    override fun createTexture(): GlTexture {
        engineStats.calls++
        intBuffer.position(0)
        GLES20.glGenTextures(1, intBuffer)
        return GlTexture(intBuffer[0])
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        if (lastBoundTexture != glTexture) {
            engineStats.calls++
            engineStats.textureBindings++
            lastBoundTexture = glTexture
            GLES20.glBindTexture(target, glTexture.reference)
        } else if (engineStats.textureBindings == 0) {
            engineStats.textureBindings = 1
        }
    }

    override fun bindDefaultTexture(target: TextureTarget) {
        if (lastBoundTexture != null) {
            engineStats.calls++
            engineStats.textureBindings++
            lastBoundTexture = null
            GLES20.glBindTexture(target.glFlag, GL.NONE)
        }
    }

    override fun deleteTexture(glTexture: GlTexture) {
        engineStats.calls++
        intBuffer.position(0)
        intBuffer.put(0, glTexture.reference)
        GLES20.glDeleteTextures(1, intBuffer)
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
            GLES20.glCompressedTexImage2D(target, level, internalFormat, width, height, 0, source.limit, source.buffer)
        } else {
            GLES20.glCompressedTexImage2D(target, level, internalFormat, width, height, 0, 0, null)
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
        GLES20.glCompressedTexSubImage2D(
            target,
            level,
            xOffset,
            yOffset,
            width,
            height,
            format,
            source.limit,
            source.buffer
        )
    }

    override fun copyTexImage2D(
        target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int,
    ) {
        engineStats.calls++
        GLES20.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border)
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
        GLES20.glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
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
        GLES20.glTexSubImage2D(target, level, xOffset, yOffset, width, height, format, type, source.buffer)
    }

    override fun texImage2D(
        target: Int, level: Int, internalFormat: Int, format: Int, width: Int, height: Int, type: Int,
    ) {
        engineStats.calls++
        GLES20.glTexImage2D(
            target, level, internalFormat, width, height, 0, format, type, null as java.nio.ByteBuffer?
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
        GLES20.glTexImage2D(
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
            GLES30.glCompressedTexImage3D(
                target,
                level,
                internalFormat,
                width,
                height,
                depth,
                0,
                source.limit,
                source.buffer
            )
        } else {
            GLES30.glCompressedTexImage3D(target, level, internalFormat, width, height, depth, 0, 0, null)
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
        GLES30.glCompressedTexSubImage3D(
            target,
            level,
            xOffset,
            yOffset,
            zOffset,
            width,
            height,
            depth,
            format,
            source.limit,
            source.buffer
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
        GLES30.glCopyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height)
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
        GLES30.glTexSubImage3D(
            target,
            level,
            xOffset,
            yOffset,
            zOffset,
            width,
            height,
            depth,
            format,
            type,
            source.buffer
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
        GLES30.glTexImage3D(
            target, level, internalFormat, width, height, depth, 0, format, type, null as java.nio.ByteBuffer?
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
        GLES30.glTexImage3D(
            target, level, internalFormat, width, height, depth, 0, format, type, source.buffer
        )
        source.position = pos
    }

    override fun activeTexture(texture: Int) {
        engineStats.calls++
        GLES20.glActiveTexture(texture)
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        engineStats.calls++
        GLES20.glTexParameteri(target, pname, param)
    }

    override fun texParameterf(target: Int, pname: Int, param: Float) {
        engineStats.calls++
        GLES20.glTexParameterf(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        engineStats.calls++
        GLES20.glGenerateMipmap(target)
    }
}