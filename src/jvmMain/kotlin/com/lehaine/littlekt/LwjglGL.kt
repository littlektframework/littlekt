package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.graphics.shader.ShaderProgramReference
import com.lehaine.littlekt.graphics.shader.ShaderReference
import com.lehaine.littlekt.graphics.shader.Uniform
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

    override fun createProgram(): ShaderProgramReference {
        return ShaderProgramReference(glCreateProgram())
    }

    override fun getAttribLocation(shaderProgram: ShaderProgramReference, name: String): Int {
        return glGetAttribLocation(shaderProgram.address, name)
    }

    override fun getUniformLocation(shaderProgram: ShaderProgramReference, name: String): Uniform {
        return Uniform(glGetUniformLocation(shaderProgram.address, name))
    }

    override fun attachShader(shaderProgram: ShaderProgramReference, shaderReference: ShaderReference) {
        glAttachShader(shaderProgram.address, shaderReference.address)
    }

    override fun linkProgram(shaderProgram: ShaderProgramReference) {
        glLinkProgram(shaderProgram.address)
    }

    override fun deleteProgram(shaderProgram: ShaderProgramReference) {
        glDeleteProgram(shaderProgram.address)
    }

    override fun getProgramParameter(shaderProgram: ShaderProgramReference, mask: Int): Any {
        return glGetProgrami(shaderProgram.address, mask)
    }

    override fun getShaderParameter(shaderReference: ShaderReference, mask: Int): Any {
        return glGetShaderi(shaderReference.address, mask)
    }

    override fun getProgramParameterB(shaderProgram: ShaderProgramReference, mask: Int): Boolean {
        return (getProgramParameter(shaderProgram, mask) as? Int) == 1
    }

    override fun getString(parameterName: Int): String? {
        return glGetString(parameterName)
    }

    override fun getShaderParameterB(shaderReference: ShaderReference, mask: Int): Boolean {
        return (getShaderParameter(shaderReference, mask) as? Int) == 1
    }

    override fun createShader(type: Int): ShaderReference {
        return ShaderReference(glCreateShader(type))
    }

    override fun shaderSource(shaderReference: ShaderReference, source: String) {
        glShaderSource(shaderReference.address, source)
    }

    override fun compileShader(shaderReference: ShaderReference) {
        glCompileShader(shaderReference.address)
    }

    override fun getShaderInfoLog(shaderReference: ShaderReference): String {
        return glGetShaderInfoLog(shaderReference.address)
    }

    override fun deleteShader(shaderReference: ShaderReference) {
        glDeleteShader(shaderReference.address)
    }

    override fun getProgramInfoLog(shader: ShaderProgramReference): String {
        return glGetProgramInfoLog(shader.address)
    }

    override fun createBuffer(): BufferReference {
        return BufferReference(glGenBuffers())
    }

    override fun createFrameBuffer(): FrameBufferReference {
        return FrameBufferReference(glGenFramebuffers())
    }

    override fun createVertexArray(): VertexArrayReference {
        return VertexArrayReference(glGenVertexArrays())
    }

    override fun bindVertexArray(vertexArrayReference: VertexArrayReference) {
        glBindVertexArray(vertexArrayReference.address)
    }

    override fun bindDefaultVertexArray() {
        glBindVertexArray(0)
    }

    override fun bindFrameBuffer(frameBufferReference: FrameBufferReference) {
        glBindFramebuffer(GL.FRAMEBUFFER, frameBufferReference.reference)
    }

    override fun bindDefaultFrameBuffer() {
        glBindFramebuffer(GL.FRAMEBUFFER, 0)
    }

    override fun createRenderBuffer(): RenderBufferReference {
        return RenderBufferReference(glGenRenderbuffers())
    }

    override fun bindRenderBuffer(renderBufferReference: RenderBufferReference) {
        glBindRenderbuffer(GL.RENDERBUFFER, renderBufferReference.reference)
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        glRenderbufferStorage(GL.RENDERBUFFER, internalformat, width, height)
    }

    override fun framebufferRenderbuffer(attachementType: Int, renderBufferReference: RenderBufferReference) {
        glFramebufferRenderbuffer(GL.FRAMEBUFFER, attachementType, GL.RENDERBUFFER, renderBufferReference.reference)
    }

    override fun frameBufferTexture2D(attachmentPoint: Int, textureReference: TextureReference, level: Int) {
        glFramebufferTexture2D(GL.FRAMEBUFFER, attachmentPoint, GL.TEXTURE_2D, textureReference.reference, level)
    }

    override fun bindBuffer(target: Int, bufferReference: BufferReference) {
        glBindBuffer(target, bufferReference.address)
    }

    override fun bindDefaultBuffer(target: Int) {
        glBindBuffer(target, 0)
    }

    override fun deleteBuffer(bufferReference: BufferReference) {
        glDeleteBuffers(bufferReference.address)
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

    override fun useProgram(shaderProgram: ShaderProgramReference) {
        glUseProgram(shaderProgram.address)
    }

    override fun useDefaultProgram() {
        glUseProgram(0)
    }

    override fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: Array<Float>) {
        glUniformMatrix4fv(uniform.address, transpose, data.toFloatArray())
    }

    override fun uniform1i(uniform: Uniform, data: Int) {
        glUniform1i(uniform.address, data)
    }

    override fun uniform2i(uniform: Uniform, a: Int, b: Int) {
        1
        glUniform2i(uniform.address, a, b)
    }

    override fun uniform3i(uniform: Uniform, a: Int, b: Int, c: Int) {
        glUniform3i(uniform.address, a, b, c)
    }

    override fun uniform1f(uniform: Uniform, first: Float) {
        glUniform1f(uniform.address, first)
    }

    override fun uniform2f(uniform: Uniform, first: Float, second: Float) {
        glUniform2f(uniform.address, first, second)
    }

    override fun uniform3f(uniform: Uniform, first: Float, second: Float, third: Float) {
        glUniform3f(uniform.address, first, second, third)
    }

    override fun uniform4f(uniform: Uniform, first: Float, second: Float, third: Float, fourth: Float) {
        glUniform4f(uniform.address, first, second, third, fourth)
    }

    override fun drawArrays(mask: Int, offset: Int, vertexCount: Int) {
        glDrawArrays(mask, offset, vertexCount)
    }

    override fun drawElements(mask: Int, vertexCount: Int, type: Int, offset: Int) {
        glDrawElements(mask, vertexCount, type, offset.toLong())
    }

    override fun vertex2f(x: Float, y: Float) {
        glVertex2f(x, y)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }

    override fun createTexture(): TextureReference {
        return TextureReference(glGenTextures())
    }

    override fun bindTexture(target: Int, textureReference: TextureReference) {
        glBindTexture(target, textureReference.reference)
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
        // glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
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

    override fun activeTexture(Int: Int) {
        glActiveTexture(Int)
    }

    override fun texParameteri(target: Int, paramName: Int, paramValue: Int) {
        glTexParameteri(target, paramName, paramValue)
    }

    override fun generateMipmap(target: Int) {
        glGenerateMipmap(target)
    }
}