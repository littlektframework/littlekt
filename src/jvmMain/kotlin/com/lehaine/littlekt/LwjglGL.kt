package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.DataSource
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class LwjglGL : GL {
    internal var _glVersion: GLVersion = GLVersion.GL_32

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

    override fun enable(mask: Int) {
        glEnable(mask)
    }

    override fun disable(mask: Int) {
        glDisable(mask)
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

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, mask: Int): Any {
        return glGetProgrami(glShaderProgram.address, mask)
    }

    override fun getShaderParameter(glShader: GlShader, mask: Int): Any {
        return glGetShaderi(glShader.address, mask)
    }

    override fun getProgramParameterB(glShaderProgram: GlShaderProgram, mask: Int): Boolean {
        return (getProgramParameter(glShaderProgram, mask) as? Int) == 1
    }

    override fun getString(parameterName: Int): String? {
        return glGetString(parameterName)
    }

    override fun getShaderParameterB(glShader: GlShader, mask: Int): Boolean {
        return (getShaderParameter(glShader, mask) as? Int) == 1
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
            is DataSource.FloatBufferDataSource -> glBufferData(target, data.buffer.dw, usage)
            is DataSource.ShortBufferDataSource -> glBufferData(target, data.buffer.dw, usage)
        }
    }

    override fun depthFunc(target: Int) {
        glDepthFunc(target)
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

    override fun uniform2i(uniformLocation: UniformLocation, a: Int, b: Int) {
        glUniform2i(uniformLocation.address, a, b)
    }

    override fun uniform3i(uniformLocation: UniformLocation, a: Int, b: Int, c: Int) {
        glUniform3i(uniformLocation.address, a, b, c)
    }

    override fun uniform1f(uniformLocation: UniformLocation, first: Float) {
        glUniform1f(uniformLocation.address, first)
    }

    override fun uniform2f(uniformLocation: UniformLocation, first: Float, second: Float) {
        glUniform2f(uniformLocation.address, first, second)
    }

    override fun uniform3f(uniformLocation: UniformLocation, first: Float, second: Float, third: Float) {
        glUniform3f(uniformLocation.address, first, second, third)
    }

    override fun uniform4f(uniformLocation: UniformLocation, first: Float, second: Float, third: Float, fourth: Float) {
        glUniform4f(uniformLocation.address, first, second, third, fourth)
    }

    override fun drawArrays(mode: Int, offset: Int, vertexCount: Int) {
        glDrawArrays(mode, offset, vertexCount)
    }

    override fun drawElements(mode: Int, vertexCount: Int, type: Int, offset: Int) {
        glDrawElements(mode, vertexCount, type, offset.toLong())
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

    override fun texImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteArray
    ) {
        val buffer = ByteBuffer.allocateDirect(source.size)
        buffer.put(source)
        (buffer as java.nio.Buffer).position(0)

        glTexImage2D(
            target,
            level,
            internalformat,
            width,
            height,
            0,
            format,
            type,
            buffer
        )
    }

    override fun activeTexture(int: Int) {
        glActiveTexture(int)
    }

    override fun texParameteri(target: Int, paramName: Int, paramValue: Int) {
        glTexParameteri(target, paramName, paramValue)
    }

    override fun generateMipmap(target: Int) {
        glGenerateMipmap(target)
    }
}