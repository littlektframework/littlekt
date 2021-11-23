package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.io.Uint8Buffer
import com.lehaine.littlekt.io.Uint8BufferImpl
import org.lwjgl.opengl.GL30.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class LwjglGL : GL {
    internal var _glVersion: GLVersion = GLVersion.GL_30

    override fun getGLVersion(): GLVersion = _glVersion

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        glClearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        glClear(mask)
    }

    override fun clearDepth(depth: Number) {
        glClearDepth(depth.toDouble())
    }

    override fun enable(cap: Int) {
        glEnable(cap)
    }

    override fun disable(cap: Int) {
        glDisable(cap)
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        glBlendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun createProgram(): GlShaderProgram {
        return GlShaderProgram(glCreateProgram())
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        return glGetAttribLocation(glShaderProgram.address, name)
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        return UniformLocation(glGetUniformLocation(glShaderProgram.address, name))
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        glAttachShader(glShaderProgram.address, glShader.address)
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        glLinkProgram(glShaderProgram.address)
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        glDeleteProgram(glShaderProgram.address)
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any {
        return glGetProgrami(glShaderProgram.address, pname)
    }

    override fun getShaderParameter(glShader: GlShader, pname: Int): Any {
        return glGetShaderi(glShader.address, pname)
    }

    override fun getProgramParameterB(glShaderProgram: GlShaderProgram, pname: Int): Boolean {
        return (getProgramParameter(glShaderProgram, pname) as? Int) == 1
    }

    override fun getString(pname: Int): String? {
        return glGetString(pname)
    }

    override fun getShaderParameterB(glShader: GlShader, pname: Int): Boolean {
        return (getShaderParameter(glShader, pname) as? Int) == 1
    }

    override fun createShader(type: Int): GlShader {
        return GlShader(glCreateShader(type))
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        glShaderSource(glShader.address, source)
    }

    override fun compileShader(glShader: GlShader) {
        glCompileShader(glShader.address)
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        return glGetShaderInfoLog(glShader.address)
    }

    override fun deleteShader(glShader: GlShader) {
        glDeleteShader(glShader.address)
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        return glGetProgramInfoLog(glShader.address)
    }

    override fun createBuffer(): GlBuffer {
        return GlBuffer(glGenBuffers())
    }

    override fun createFrameBuffer(): GlFrameBuffer {
        return GlFrameBuffer(glGenFramebuffers())
    }

    override fun createVertexArray(): GlVertexArray {
        return GlVertexArray(glGenVertexArrays())
    }

    override fun bindVertexArray(glVertexArray: GlVertexArray) {
        glBindVertexArray(glVertexArray.address)
    }

    override fun bindDefaultVertexArray() {
        glBindVertexArray(0)
    }

    override fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        glBindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.reference)
    }

    override fun bindDefaultFrameBuffer() {
        glBindFramebuffer(GL.FRAMEBUFFER, 0)
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        return GlRenderBuffer(glGenRenderbuffers())
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        glBindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.reference)
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        glRenderbufferStorage(GL.RENDERBUFFER, internalformat, width, height)
    }

    override fun framebufferRenderbuffer(
        attachementType: Int,
        glRenderBuffer: GlRenderBuffer
    ) {
        glFramebufferRenderbuffer(GL.FRAMEBUFFER, attachementType, GL.RENDERBUFFER, glRenderBuffer.reference)
    }

    override fun frameBufferTexture2D(
        attachmentPoint: Int,
        glTexture: GlTexture,
        level: Int
    ) {
        glFramebufferTexture2D(GL.FRAMEBUFFER, attachmentPoint, GL.TEXTURE_2D, glTexture.reference, level)
    }

    override fun bindBuffer(target: Int, glBuffer: GlBuffer) {
        glBindBuffer(target, glBuffer.address)
    }

    override fun bindDefaultBuffer(target: Int) {
        glBindBuffer(target, 0)
    }

    override fun deleteBuffer(glBuffer: GlBuffer) {
        glDeleteBuffers(glBuffer.address)
    }

    override fun bufferData(target: Int, data: DataSource, usage: Int) {
        when (data) {
            is DataSource.FloatDataSource -> glBufferData(target, data.floats, usage)
            is DataSource.IntDataSource -> glBufferData(target, data.ints, usage)
            is DataSource.ShortDataSource -> glBufferData(target, data.shorts, usage)
            is DataSource.UIntDataSource -> glBufferData(target, data.ints, usage)
            is DataSource.DoubleDataSource -> glBufferData(target, data.double, usage)
            is DataSource.Float32BufferDataSource -> {
                data.buffer.position = 0
                data.buffer.limit = data.buffer.capacity
                glBufferData(target, data.buffer.toArray(), usage)
            }
            is DataSource.Uint16BufferDataSource -> {
                data.buffer.position = 0
                data.buffer.limit = data.buffer.capacity
                glBufferData(target, data.buffer.toArray(), usage)
            }
        }
    }

    override fun depthFunc(func: Int) {
        glDepthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        glDepthMask(flag)
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
    }

    override fun enableVertexAttribArray(index: Int) {
        glEnableVertexAttribArray(index)
    }

    override fun disableVertexAttribArray(index: Int) {
        glDisableVertexAttribArray(index)
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        glUseProgram(glShaderProgram.address)
    }

    override fun useDefaultProgram() {
        glUseProgram(0)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        glUniformMatrix4fv(uniformLocation.address, transpose, data.toFloatArray())
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        glUniform1i(uniformLocation.address, data)
    }

    override fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int) {
        glUniform2i(uniformLocation.address, x, y)
    }

    override fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int) {
        glUniform3i(uniformLocation.address, x, y, z)
    }

    override fun uniform1f(uniformLocation: UniformLocation, x: Float) {
        glUniform1f(uniformLocation.address, x)
    }

    override fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float) {
        glUniform2f(uniformLocation.address, x, y)
    }

    override fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float) {
        glUniform3f(uniformLocation.address, x, y, z)
    }

    override fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float) {
        glUniform4f(uniformLocation.address, x, y, z, w)
    }

    override fun drawArrays(mode: Int, offset: Int, count: Int) {
        glDrawArrays(mode, offset, count)
    }

    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
        glDrawElements(mode, count, type, offset.toLong())
    }

    override fun pixelStorei(pname: Int, param: Int) {
        glPixelStorei(pname, param)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }

    override fun createTexture(): GlTexture {
        return GlTexture(glGenTextures())
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        glBindTexture(target, glTexture.reference)
    }

    override fun deleteTexture(glTexture: GlTexture) {
        glDeleteTextures(glTexture.reference)
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
        glActiveTexture(texture)
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        glTexParameteri(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        glGenerateMipmap(target)
    }
}