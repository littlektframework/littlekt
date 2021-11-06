package com.lehaine.littlekt

import com.lehaine.littlekt.render.TextureImage
import com.lehaine.littlekt.shader.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class LwjglGL : GL {
    override fun clearColor(r: Percent, g: Percent, b: Percent, a: Percent) {
        glClearColor(r.toPercent(), g.toPercent(), b.toPercent(), a.toPercent())
    }

    override fun clear(mask: ByteMask) {
        glClear(mask)
    }

    override fun clearDepth(depth: Number) {
        glClearDepth(depth.toDouble())
    }

    override fun enable(mask: ByteMask) {
        glEnable(mask)
    }

    override fun disable(mask: ByteMask) {
        glDisable(mask)
    }

    override fun blendFunc(sfactor: ByteMask, dfactor: ByteMask) {
        glBlendFunc(sfactor, dfactor)
    }

    override fun createProgram(): ShaderProgram {
        return ShaderProgram(this, PlatformShaderProgram(glCreateProgram()))
    }

    override fun getAttribLocation(shaderProgram: ShaderProgram, name: String): Int {
        return glGetAttribLocation(shaderProgram.program.address, name)
    }

    override fun getUniformLocation(shaderProgram: ShaderProgram, name: String): Uniform {
        return Uniform(glGetUniformLocation(shaderProgram.program.address, name))
    }

    override fun attachShader(shaderProgram: ShaderProgram, shader: Shader) {
        glAttachShader(shaderProgram.program.address, shader.address)
    }

    override fun linkProgram(shaderProgram: ShaderProgram) {
        glLinkProgram(shaderProgram.program.address)
    }

    override fun getProgramParameter(shaderProgram: ShaderProgram, mask: ByteMask): Any {
        return glGetProgrami(shaderProgram.program.address, mask)
    }

    override fun getShaderParameter(shader: Shader, mask: ByteMask): Any {
        return glGetShaderi(shader.address, mask)
    }

    override fun getProgramParameterB(shaderProgram: ShaderProgram, mask: ByteMask): Boolean {
        return (getProgramParameter(shaderProgram, mask) as? Int) == 1
    }

    override fun getString(parameterName: Int): String? {
        return glGetString(parameterName)
    }

    override fun getShaderParameterB(shader: Shader, mask: ByteMask): Boolean {
        return (getShaderParameter(shader, mask) as? Int) == 1
    }

    override fun createShader(type: ByteMask): Shader {
        return Shader(glCreateShader(type))
    }

    override fun shaderSource(shader: Shader, source: String) {
        glShaderSource(shader.address, source)
    }

    override fun compileShader(shader: Shader) {
        glCompileShader(shader.address)
    }

    override fun getShaderInfoLog(shader: Shader): String {
        return glGetShaderInfoLog(shader.address)
    }

    override fun deleteShader(shader: Shader) {
        glDeleteShader(shader.address)
    }

    override fun getProgramInfoLog(shader: ShaderProgram): String {
        return glGetProgramInfoLog(shader.program.address)
    }

    override fun createBuffer(): Buffer {
        return Buffer(glGenBuffers())
    }

    override fun createFrameBuffer(): FrameBufferReference {
        return FrameBufferReference(glGenFramebuffers())
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

    override fun bindBuffer(target: ByteMask, buffer: Buffer) {
        glBindBuffer(target, buffer.address)
    }

    override fun bufferData(target: ByteMask, data: DataSource, usage: Int) {
        when (data) {
            is DataSource.FloatDataSource -> glBufferData(target, data.floats, usage)
            is DataSource.IntDataSource -> glBufferData(target, data.ints, usage)
            is DataSource.ShortDataSource -> glBufferData(target, data.shorts, usage)
            is DataSource.UIntDataSource -> glBufferData(target, data.ints, usage)
            is DataSource.DoubleDataSource -> glBufferData(target, data.double, usage)
        }
    }

    override fun depthFunc(target: ByteMask) {
        glDepthFunc(target)
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
    }

    override fun enableVertexAttribArray(index: Int) {
        glEnableVertexAttribArray(index)
    }

    override fun useProgram(shaderProgram: ShaderProgram) {
        glUseProgram(shaderProgram.program.address)
    }

    override fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: Array<Float>) {
        glUniformMatrix4fv(uniform.address, transpose, data.toFloatArray())
    }

    override fun uniform1i(uniform: Uniform, data: Int) {
        glUniform1i(uniform.address, data)
    }

    override fun uniform2i(uniform: Uniform, a: Int, b: Int) {
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

    override fun drawArrays(mask: ByteMask, offset: Int, vertexCount: Int) {
        glDrawArrays(mask, offset, vertexCount)
    }

    override fun drawElements(mask: ByteMask, vertexCount: Int, type: Int, offset: Int) {
        glDrawElements(mask, vertexCount, type, offset.toLong())
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
        type: Int,
        source: TextureImage
    ) {
        glTexImage2D(
            target,
            level,
            internalformat,
            source.width,
            source.height,
            0,
            source.glFormat,
            source.glType,
            source.pixels
        )
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

    override fun activeTexture(byteMask: ByteMask) {
        glActiveTexture(byteMask)
    }

    override fun texParameteri(target: Int, paramName: Int, paramValue: Int) {
        glTexParameteri(target, paramName, paramValue)
    }

    override fun generateMipmap(target: Int) {
        glGenerateMipmap(target)
    }
}