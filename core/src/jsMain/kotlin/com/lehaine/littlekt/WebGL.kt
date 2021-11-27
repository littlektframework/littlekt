package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.io.*
import com.lehaine.littlekt.math.Mat4
import org.khronos.webgl.WebGLFramebuffer

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class WebGL(val gl: WebGL2RenderingContext, private val engineStats: EngineStats) : GL {
    private var lastBoundBuffer: GlBuffer? = null

    override fun getGLVersion(): GLVersion {
        return GLVersion.WEBGL_2
    }

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        engineStats.calls++
        gl.clearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        engineStats.calls++
        gl.clear(mask)
    }

    override fun clearDepth(depth: Number) {
        engineStats.calls++
        gl.clearDepth(depth.toFloat())
    }

    override fun enable(cap: Int) {
        engineStats.calls++
        gl.enable(cap)
    }

    override fun disable(cap: Int) {
        engineStats.calls++
        gl.disable(cap)
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        engineStats.calls++
        gl.blendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        engineStats.calls++
        gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun createProgram(): GlShaderProgram {
        engineStats.calls++
        return GlShaderProgram(gl.createProgram()!!)
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        engineStats.calls++
        return gl.getAttribLocation(glShaderProgram.delegate, name)
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        engineStats.calls++
        return UniformLocation(
            gl.getUniformLocation(glShaderProgram.delegate, name)
                ?: throw RuntimeException("Uniform $name has not been created.")
        )
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        gl.attachShader(glShaderProgram.delegate, glShader.delegate)
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        gl.linkProgram(glShaderProgram.delegate)
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        gl.deleteProgram(glShaderProgram.delegate)
    }

    override fun getString(pname: Int): String? {
        engineStats.calls++
        return gl.getParameter(pname) as? String
    }

    override fun getIntegerv(pname: Int, data: Uint32Buffer) {
        engineStats.calls++
        val result = gl.getParameter(pname) as Int
        data.put(result)
    }

    override fun getBoundFrameBuffer(data: Uint32Buffer): GlFrameBuffer {
        engineStats.calls++
        val result = gl.getParameter(GL.FRAMEBUFFER_BINDING) as WebGLFramebuffer
        return GlFrameBuffer(result)
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any {
        engineStats.calls++
        return gl.getProgramParameter(glShaderProgram.delegate, pname)!!
    }

    override fun getShaderParameter(glShader: GlShader, pname: Int): Any {
        engineStats.calls++
        return gl.getShaderParameter(glShader.delegate, pname)!!
    }

    override fun createShader(type: Int): GlShader {
        engineStats.calls++
        return GlShader(gl.createShader(type)!!)
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        engineStats.calls++
        gl.shaderSource(glShader.delegate, source)
    }

    override fun compileShader(glShader: GlShader) {
        engineStats.calls++
        gl.compileShader(glShader.delegate)
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        engineStats.calls++
        return gl.getShaderInfoLog(glShader.delegate) ?: ""
    }

    override fun deleteShader(glShader: GlShader) {
        engineStats.calls++
        gl.deleteShader(glShader.delegate)
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        engineStats.calls++
        return gl.getProgramInfoLog(glShader.delegate) ?: ""
    }

    override fun createBuffer(): GlBuffer {
        engineStats.calls++
        return GlBuffer(gl.createBuffer()!!)
    }

    override fun createFrameBuffer(): GlFrameBuffer {
        engineStats.calls++
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
        engineStats.calls++
        gl.bindFramebuffer(GL.FRAMEBUFFER, glFrameBuffer.delegate)
    }

    override fun bindDefaultFrameBuffer() {
        engineStats.calls++
        gl.bindFramebuffer(GL.FRAMEBUFFER, null)
    }

    override fun createRenderBuffer(): GlRenderBuffer {
        engineStats.calls++
        return GlRenderBuffer(gl.createRenderbuffer()!!)
    }

    override fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        gl.bindRenderbuffer(GL.RENDERBUFFER, glRenderBuffer.delegate)
    }

    override fun bindDefaultRenderBuffer() {
        engineStats.calls++
        gl.bindRenderbuffer(GL.RENDERBUFFER, null)
    }

    override fun renderBufferStorage(internalFormat: RenderBufferInternalFormat, width: Int, height: Int) {
        engineStats.calls++
        gl.renderbufferStorage(GL.RENDERBUFFER, internalFormat.glFlag, width, height)
    }

    override fun frameBufferRenderBuffer(
        attachementType: FrameBufferRenderBufferAttachment,
        glRenderBuffer: GlRenderBuffer
    ) {
        engineStats.calls++
        gl.framebufferRenderbuffer(GL.FRAMEBUFFER, attachementType.glFlag, GL.RENDERBUFFER, glRenderBuffer.delegate)
    }

    override fun deleteFrameBuffer(glFrameBuffer: GlFrameBuffer) {
        engineStats.calls++
        gl.deleteFramebuffer(glFrameBuffer.delegate)
    }

    override fun deleteRenderBuffer(glRenderBuffer: GlRenderBuffer) {
        engineStats.calls++
        gl.deleteRenderbuffer(glRenderBuffer.delegate)
    }

    override fun frameBufferTexture2D(
        attachementType: FrameBufferRenderBufferAttachment,
        glTexture: GlTexture,
        level: Int
    ) {
        engineStats.calls++
        gl.framebufferTexture2D(GL.FRAMEBUFFER, attachementType.glFlag, GL.TEXTURE_2D, glTexture.delegate, level)
    }

    override fun checkFrameBufferStatus(): FrameBufferStatus {
        engineStats.calls++
        return FrameBufferStatus(gl.checkFramebufferStatus(GL.FRAMEBUFFER))
    }

    override fun bindBuffer(target: Int, glBuffer: GlBuffer) {
        engineStats.calls++
        lastBoundBuffer = glBuffer
        gl.bindBuffer(target, glBuffer.delegate)
    }

    override fun bindDefaultBuffer(target: Int) {
        engineStats.calls++
        lastBoundBuffer = null
        gl.bindBuffer(target, null)
    }

    override fun deleteBuffer(glBuffer: GlBuffer) {
        engineStats.calls++
        if (lastBoundBuffer == glBuffer) {
            lastBoundBuffer = null
        }
        gl.deleteBuffer(glBuffer.delegate)
    }

    override fun bufferData(target: Int, data: DataSource, usage: Int) {
        engineStats.calls++
        val limit = data.buffer.limit
        val pos = data.buffer.position
        data.buffer.position = 0
        data.buffer.limit = data.buffer.capacity
        when (data) {
            is DataSource.Float32BufferDataSource -> {
                data.buffer as Float32BufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
            is DataSource.Uint8BufferDataSource -> {
                data.buffer as Uint8BufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity)
            }
            is DataSource.Uint16BufferDataSource -> {
                data.buffer as Uint16BufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 2)
            }
            is DataSource.Uint32BufferDataSource -> {
                data.buffer as Uint32BufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
        }
        data.buffer.limit = limit
        data.buffer.position = pos
    }

    override fun depthFunc(func: Int) {
        engineStats.calls++
        gl.depthFunc(func)
    }

    override fun depthMask(flag: Boolean) {
        engineStats.calls++
        gl.depthMask(flag)
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        engineStats.calls++
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    override fun enableVertexAttribArray(index: Int) {
        engineStats.calls++
        gl.enableVertexAttribArray(index)
    }

    override fun disableVertexAttribArray(index: Int) {
        engineStats.calls++
        gl.disableVertexAttribArray(index)
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        engineStats.shaderSwitches++
        gl.useProgram(glShaderProgram.delegate)
    }

    override fun useDefaultProgram() {
        engineStats.calls++
        engineStats.shaderSwitches++
        gl.useProgram(null)
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++
        gl.scissor(x, y, width, height)
    }

    override fun createTexture(): GlTexture {
        engineStats.calls++
        return GlTexture(gl.createTexture()!!)
    }

    override fun activeTexture(texture: Int) {
        engineStats.calls++
        gl.activeTexture(texture)
    }

    override fun bindTexture(target: Int, glTexture: GlTexture) {
        engineStats.calls++
        engineStats.textureBindings++
        gl.bindTexture(target, glTexture.delegate)
    }

    override fun deleteTexture(glTexture: GlTexture) {
        engineStats.calls++
        gl.deleteTexture(glTexture.delegate)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4) {
        engineStats.calls++
        val buffer = createFloat32Buffer(16) as Float32BufferImpl
        data.toBuffer(buffer)
        gl.uniformMatrix4fv(uniformLocation.delegate, transpose, buffer.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Float32Buffer) {
        engineStats.calls++
        data as Float32BufferImpl
        gl.uniformMatrix4fv(uniformLocation.delegate, transpose, data.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        gl.uniformMatrix4fv(uniformLocation.delegate, transpose, data)
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        engineStats.calls++
        gl.uniform1i(uniformLocation.delegate, data)
    }

    override fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int) {
        engineStats.calls++
        gl.uniform2i(uniformLocation.delegate, x, y)
    }

    override fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int) {
        engineStats.calls++
        gl.uniform3i(uniformLocation.delegate, x, y, z)
    }

    override fun uniform1f(uniformLocation: UniformLocation, x: Float) {
        engineStats.calls++
        gl.uniform1f(uniformLocation.delegate, x)
    }

    override fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float) {
        engineStats.calls++
        gl.uniform2f(uniformLocation.delegate, x, y)
    }

    override fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float) {
        engineStats.calls++
        gl.uniform3f(uniformLocation.delegate, x, y, z)
    }

    override fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float) {
        engineStats.calls++
        gl.uniform4f(uniformLocation.delegate, x, y, z, w)
    }

    override fun drawArrays(mode: Int, offset: Int, count: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        gl.drawArrays(mode, offset, count)
    }

    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
        engineStats.calls++
        engineStats.drawCalls++
        engineStats.vertices += count
        gl.drawElements(mode, count, type, offset)
    }

    override fun pixelStorei(pname: Int, param: Int) {
        engineStats.calls++
        gl.pixelStorei(pname, param)
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        engineStats.calls++
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
        source: Uint8Buffer?
    ) {
        engineStats.calls++
        if (source != null) {
            source as Uint8BufferImpl
            gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, source.buffer)
        } else {
            gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, null)
        }
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        engineStats.calls++
        gl.texParameteri(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        engineStats.calls++
        gl.generateMipmap(target)
    }

}