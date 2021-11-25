package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.io.*
import com.lehaine.littlekt.math.Mat4
import org.lwjgl.opengl.GL30.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class LwjglGL(private val engineStats: EngineStats) : GL {
    internal var _glVersion: GLVersion = GLVersion.GL_30

    override fun getGLVersion(): GLVersion = _glVersion

    var lastBoundBuffer: GlBuffer? = null

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        engineStats.calls++
        glClearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        engineStats.calls++
        glClear(mask)
    }

    override fun clearDepth(depth: Number) {
        engineStats.calls++
        glClearDepth(depth.toDouble())
    }

    override fun enable(cap: Int) {
        engineStats.calls++
        glEnable(cap)
    }

    override fun disable(cap: Int) {
        engineStats.calls++
        glDisable(cap)
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        engineStats.calls++
        glBlendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        engineStats.calls++
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
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
        return GlFrameBuffer(glGenFramebuffers())
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
        glBindVertexArray(0)
    }

    override fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        glBindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.reference)
    }

    override fun bindDefaultFrameBuffer() {
        engineStats.calls++
        glBindFramebuffer(GL.FRAMEBUFFER, 0)
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        engineStats.calls++
        return GlRenderBuffer(glGenRenderbuffers())
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        glBindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.reference)
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        engineStats.calls++
        glRenderbufferStorage(GL.RENDERBUFFER, internalformat, width, height)
    }

    override fun framebufferRenderbuffer(
        attachementType: Int,
        glRenderBuffer: GlRenderBuffer
    ) {
        engineStats.calls++
        glFramebufferRenderbuffer(GL.FRAMEBUFFER, attachementType, GL.RENDERBUFFER, glRenderBuffer.reference)
    }

    override fun frameBufferTexture2D(
        attachmentPoint: Int,
        glTexture: GlTexture,
        level: Int
    ) {
        engineStats.calls++
        glFramebufferTexture2D(GL.FRAMEBUFFER, attachmentPoint, GL.TEXTURE_2D, glTexture.reference, level)
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

    override fun bufferData(target: Int, data: DataSource, usage: Int) {
        engineStats.calls++
        val limit = data.buffer.limit
        val pos = data.buffer.position
        data.buffer.position = 0
        data.buffer.limit = data.buffer.capacity
        engineStats.bufferDeleted(lastBoundBuffer!!.bufferId)
        when (data) {
            is DataSource.Float32BufferDataSource -> {
                data.buffer as Float32BufferImpl
                glBufferData(target, data.buffer.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
            is DataSource.Uint8BufferDataSource -> {
                data.buffer as Uint16BufferImpl
                glBufferData(target, data.buffer.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity)
            }
            is DataSource.Uint16BufferDataSource -> {
                data.buffer as Uint16BufferImpl
                glBufferData(target, data.buffer.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 2)
            }
            is DataSource.Uint32BufferDataSource -> {
                data.buffer as Uint32BufferImpl
                glBufferData(target, data.buffer.buffer, usage)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
        }

        data.buffer.limit = limit
        data.buffer.position = pos
    }

    override fun depthFunc(func: Int) {
        engineStats.calls++
        glDepthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        engineStats.calls++
        glDepthMask(flag)
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
        engineStats.calls++
        engineStats.shaderSwitches++
        glUseProgram(glShaderProgram.address)
    }

    override fun useDefaultProgram() {
        engineStats.calls++
        engineStats.shaderSwitches++
        glUseProgram(0)
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
        glViewport(x, y, width, height)
    }

    override fun createTexture(): GlTexture {
        engineStats.calls++
        return GlTexture(glGenTextures())
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        engineStats.calls++
        engineStats.textureBindings++
        glBindTexture(target, glTexture.reference)
    }

    override fun deleteTexture(glTexture: GlTexture) {
        engineStats.calls++
        glDeleteTextures(glTexture.reference)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        glUniformMatrix4fv(uniformLocation.address, transpose, data.toFloatArray())
    }


    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4) {
        engineStats.calls++
        val buffer = createFloat32Buffer(16) as Float32BufferImpl
        data.toBuffer(buffer)
        glUniformMatrix4fv(uniformLocation.address, transpose, buffer.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Float32Buffer) {
        engineStats.calls++
        data as Float32BufferImpl
        glUniformMatrix4fv(uniformLocation.address, transpose, data.buffer)
    }

    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: Uint8Buffer
    ) {
        engineStats.calls++
        source as Uint8BufferImpl
        glTexImage2D(
            target,
            level,
            internalFormat,
            width,
            height,
            0,
            format,
            type,
            source.buffer
        )
    }

    override fun activeTexture(texture: Int) {
        engineStats.calls++
        glActiveTexture(texture)
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        engineStats.calls++
        glTexParameteri(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        engineStats.calls++
        glGenerateMipmap(target)
    }
}