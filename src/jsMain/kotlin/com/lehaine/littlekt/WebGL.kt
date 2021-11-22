package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.DataSource
import org.khronos.webgl.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class WebGL(private val gl: WebGLRenderingContextBase) : GL {
    override fun getGLVersion(): GLVersion {
        TODO("Not yet implemented")
    }

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        gl.clearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        gl.clear(mask)
    }

    override fun clearDepth(depth: Number) {
        gl.clearDepth(depth.toFloat())
    }

    override fun enable(cap: Int) {
        gl.enable(cap)
    }

    override fun disable(cap: Int) {
        gl.disable(cap)
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        gl.blendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun createProgram(): GlShaderProgram {
        return GlShaderProgram(gl.createProgram()!!)
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        return gl.getAttribLocation(glShaderProgram.delegate, name)
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        return UniformLocation(
            gl.getUniformLocation(glShaderProgram.delegate, name)
                ?: throw RuntimeException("Uniform $name has not been created.")
        )
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        gl.attachShader(glShaderProgram.delegate, glShader.delegate)
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        gl.linkProgram(glShaderProgram.delegate)
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        gl.deleteProgram(glShaderProgram.delegate)
    }

    override fun getString(pname: Int): String? {
        return gl.getParameter(pname) as? String
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any {
        return gl.getProgramParameter(glShaderProgram.delegate, pname)!!
    }

    override fun getShaderParameter(glShader: GlShader, pname: Int): Any {
        return gl.getShaderParameter(glShader.delegate, pname)!!
    }

    override fun createShader(type: Int): GlShader {
        return GlShader(gl.createShader(type)!!)
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        gl.shaderSource(glShader.delegate, source)
    }

    override fun compileShader(glShader: GlShader) {
        gl.compileShader(glShader.delegate)
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        return gl.getShaderInfoLog(glShader.delegate) ?: ""
    }

    override fun deleteShader(glShader: GlShader) {
        gl.deleteShader(glShader.delegate)
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        return gl.getProgramInfoLog(glShader.delegate) ?: ""
    }

    override fun createBuffer(): GlBuffer {
        return GlBuffer(gl.createBuffer()!!)
    }

    override fun createFrameBuffer(): GlFrameBuffer {
        return GlFrameBuffer(gl.createFramebuffer()!!)
    }

    override fun createVertexArray(): GlVertexArray {
        throw RuntimeException("WebGL does not support 'createVertexArray'!")
    }

    override fun bindVertexArray(glVertexArray: GlVertexArray) {
        throw RuntimeException("WebGL does not support 'bindVertexArray'!")
    }

    override fun bindDefaultVertexArray() {
        throw RuntimeException("WebGL does not support 'bindDefaultVertexArray'!")
    }

    override fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        gl.bindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.delegate)
    }

    override fun bindDefaultFrameBuffer() {
        gl.bindFramebuffer(GL.FRAMEBUFFER, null)
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        return GlRenderBuffer(gl.createRenderbuffer()!!)
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        gl.bindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.delegate)
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        gl.renderbufferStorage(GL.RENDERBUFFER, internalformat, width, height)
    }

    override fun framebufferRenderbuffer(attachementType: Int, glRenderBuffer: GlRenderBuffer) {
        gl.framebufferRenderbuffer(GL.FRAMEBUFFER, attachementType, GL.RENDERBUFFER, glRenderBuffer.delegate)
    }

    override fun frameBufferTexture2D(attachmentPoint: Int, glTexture: GlTexture, level: Int) {
        gl.framebufferTexture2D(GL.FRAMEBUFFER, attachmentPoint, GL.TEXTURE_2D, glTexture.delegate, level)
    }

    override fun bindBuffer(target: Int, glBuffer: GlBuffer) {
        gl.bindBuffer(target, glBuffer.delegate)
    }

    override fun bindDefaultBuffer(target: Int) {
        gl.bindBuffer(target, null)
    }

    override fun deleteBuffer(glBuffer: GlBuffer) {
        gl.deleteBuffer(glBuffer.delegate)
    }

    override fun bufferData(target: Int, data: DataSource, usage: Int) {
        val source = when (data) {
            is DataSource.DoubleDataSource -> TODO("Not supported")
            is DataSource.FloatBufferDataSource -> Float32Array(data.buffer.array().toTypedArray())
            is DataSource.FloatDataSource -> Float32Array(data.floats.toTypedArray())
            is DataSource.IntDataSource -> Uint32Array(data.ints.toTypedArray())
            is DataSource.ShortBufferDataSource -> Uint16Array(data.buffer.array().toTypedArray())
            is DataSource.ShortDataSource -> Uint16Array(data.shorts.toTypedArray())
            is DataSource.UIntDataSource -> Uint32Array(data.ints.toTypedArray())
        }
        gl.bufferData(target, source, usage)
    }

    override fun depthFunc(func: Int) {
        gl.depthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        gl.depthMask(flag)
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    override fun enableVertexAttribArray(index: Int) {
        gl.enableVertexAttribArray(index)
    }

    override fun disableVertexAttribArray(index: Int) {
        gl.disableVertexAttribArray(index)
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        gl.useProgram(glShaderProgram.delegate)
    }

    override fun useDefaultProgram() {
        gl.useProgram(null)
    }

    override fun createTexture(): GlTexture {
        return GlTexture(gl.createTexture()!!)
    }

    override fun activeTexture(texture: Int) {
        gl.activeTexture(texture)
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        gl.bindTexture(target, glTexture.delegate)
    }

    override fun deleteTexture(glTexture: GlTexture) {
        gl.deleteTexture(glTexture.delegate)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        gl.uniformMatrix4fv(uniformLocation.delegate, transpose, data)
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        gl.uniform1i(uniformLocation.delegate, data)
    }

    override fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int) {
        gl.uniform2i(uniformLocation.delegate, x, y)
    }

    override fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int) {
        gl.uniform3i(uniformLocation.delegate, x, y, z)
    }

    override fun uniform1f(uniformLocation: UniformLocation, x: Float) {
        gl.uniform1f(uniformLocation.delegate, x)
    }

    override fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float) {
        gl.uniform2f(uniformLocation.delegate, x, y)
    }

    override fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float) {
        gl.uniform3f(uniformLocation.delegate, x, y, z)
    }

    override fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float) {
        gl.uniform4f(uniformLocation.delegate, x, y, z, w)
    }

    override fun drawArrays(mode: Int, offset: Int, count: Int) {
        gl.drawArrays(mode, offset, count)
    }

    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
        gl.drawElements(mode, count, type, offset)
    }

    override fun pixelStorei(pname: Int, param: Int) {
        gl.pixelStorei(pname, param)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        gl.viewport(x, y, width, height)
    }

    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteArray
    ) {
        gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, Uint8Array(source.toTypedArray()))
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        gl.texParameteri(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        gl.generateMipmap(target)
    }

}