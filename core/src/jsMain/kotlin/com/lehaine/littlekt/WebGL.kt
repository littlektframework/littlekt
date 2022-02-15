package com.lehaine.littlekt

import com.lehaine.littlekt.file.*
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4
import org.khronos.webgl.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class WebGL(val gl: WebGL2RenderingContext, val platform: Context.Platform, private val engineStats: EngineStats) : GL {
    private var lastBoundBuffer: GlBuffer? = null
    override val version: GLVersion = GLVersion(platform, if (platform == Context.Platform.WEBGL2) "3.0" else "2.0")

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        engineStats.calls++
        gl.clearColor(r, g, b, a)
    }

    override fun clear(mask: Int) {
        engineStats.calls++
        gl.clear(mask)
    }

    override fun clearDepth(depth: Float) {
        engineStats.calls++
        gl.clearDepth(depth)
    }

    override fun clearStencil(stencil: Int) {
        engineStats.calls++
        gl.clearStencil(stencil)
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        engineStats.calls++
        gl.colorMask(red, green, blue, alpha)
    }

    override fun cullFace(mode: Int) {
        engineStats.calls++
        gl.cullFace(mode)
    }

    override fun enable(cap: Int) {
        engineStats.calls++
        gl.enable(cap)
    }

    override fun disable(cap: Int) {
        engineStats.calls++
        gl.disable(cap)
    }

    override fun finish() {
        engineStats.calls++
        gl.finish()
    }

    override fun flush() {
        engineStats.calls++
        gl.flush()
    }

    override fun frontFace(mode: Int) {
        engineStats.calls++
        gl.frontFace(mode)
    }

    override fun getError(): Int {
        engineStats.calls++
        return gl.getError()
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        engineStats.calls++
        gl.blendFunc(sfactor, dfactor)
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        engineStats.calls++
        gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun stencilFunc(func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        gl.stencilFunc(func, ref, mask)
    }

    override fun stencilMask(mask: Int) {
        engineStats.calls++
        gl.stencilMask(mask)
    }

    override fun stencilOp(fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        gl.stencilOp(fail, zfail, zpass)
    }

    override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
        engineStats.calls++
        gl.stencilFuncSeparate(face, func, ref, mask)
    }

    override fun stencilMaskSeparate(face: Int, mask: Int) {
        engineStats.calls++
        gl.stencilMaskSeparate(face, mask)
    }

    override fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
        engineStats.calls++
        gl.stencilOpSeparate(face, fail, zfail, zpass)
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

    override fun detachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        engineStats.calls++
        gl.detachShader(glShaderProgram.delegate, glShader.delegate)
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

    override fun hint(target: Int, mode: Int) {
        engineStats.calls++
        gl.hint(target, mode)
    }

    override fun lineWidth(width: Float) {
        engineStats.calls++
        gl.lineWidth(width)
    }

    override fun polygonOffset(factor: Float, units: Float) {
        engineStats.calls++
        gl.polygonOffset(factor, units)
    }

    override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
        engineStats.calls++
        gl.blendColor(red, green, blue, alpha)
    }

    override fun blendEquation(mode: Int) {
        engineStats.calls++
        gl.blendEquation(mode)
    }

    override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
        engineStats.calls++
        gl.blendEquationSeparate(modeRGB, modeAlpha)
    }

    override fun getIntegerv(pname: Int, data: IntBuffer) {
        engineStats.calls++
        when (pname) {
            GL.ACTIVE_TEXTURE, GL.ALPHA_BITS, GL.BLEND_DST_ALPHA, GL.BLEND_DST_RGB,
            GL.BLEND_EQUATION_ALPHA, GL.BLEND_EQUATION_RGB, GL.BLEND_SRC_ALPHA,
            GL.BLEND_SRC_RGB, GL.BLUE_BITS, GL.CULL_FACE_MODE, GL.DEPTH_BITS,
            GL.DEPTH_FUNC, GL.FRONT_FACE, GL.GENERATE_MIPMAP_HINT, GL.GREEN_BITS,
            GL.IMPLEMENTATION_COLOR_READ_FORMAT, GL.IMPLEMENTATION_COLOR_READ_TYPE,
            GL.MAX_COMBINED_TEXTURE_IMAGE_UNITS, GL.MAX_CUBE_MAP_TEXTURE_SIZE,
            GL.MAX_FRAGMENT_UNIFORM_VECTORS, GL.MAX_RENDERBUFFER_SIZE,
            GL.MAX_TEXTURE_IMAGE_UNITS, GL.MAX_TEXTURE_SIZE, GL.MAX_VARYING_VECTORS,
            GL.MAX_VERTEX_ATTRIBS, GL.MAX_VERTEX_TEXTURE_IMAGE_UNITS,
            GL.MAX_VERTEX_UNIFORM_VECTORS, GL.NUM_COMPRESSED_TEXTURE_FORMATS,
            GL.PACK_ALIGNMENT, GL.RED_BITS, GL.SAMPLE_BUFFERS,
            GL.SAMPLES, GL.STENCIL_BACK_FAIL, GL.STENCIL_BACK_FUNC,
            GL.STENCIL_BACK_PASS_DEPTH_FAIL, GL.STENCIL_BACK_PASS_DEPTH_PASS,
            GL.STENCIL_BACK_REF, GL.STENCIL_BACK_VALUE_MASK,
            GL.STENCIL_BACK_WRITEMASK, GL.STENCIL_BITS, GL.STENCIL_CLEAR_VALUE,
            GL.STENCIL_FAIL, GL.STENCIL_FUNC, GL.STENCIL_PASS_DEPTH_FAIL,
            GL.STENCIL_PASS_DEPTH_PASS, GL.STENCIL_REF, GL.STENCIL_VALUE_MASK,
            GL.STENCIL_WRITEMASK, GL.SUBPIXEL_BITS, GL.UNPACK_ALIGNMENT -> {
                data[0] = gl.getParameter(pname) as Int
                data.flip()
            }
            GL.VIEWPORT -> {
                val array = gl.getParameter(pname) as Int32Array
                data[0] = array[0]
                data[1] = array[1]
                data[2] = array[2]
                data[3] = array[3]
                data.flip()
            }
            GL.FRAMEBUFFER_BINDING -> {
                throw IllegalStateException("WebGL backend unable to return the framebuffer through getInteger. Use getBoundFrameBuffer(Uint32Buffer) method instead!")
            }
            else -> throw RuntimeException("getInteger for $pname is not supported by WebGL backend!")
        }
    }

    override fun getBoundFrameBuffer(data: IntBuffer): GlFrameBuffer {
        engineStats.calls++
        val result = gl.getParameter(GL.FRAMEBUFFER_BINDING) as WebGLFramebuffer?
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
        engineStats.calls++
        return GlVertexArray(if (platform == Context.Platform.WEBGL2) gl.createVertexArray() else gl.createVertexArrayOES())
    }

    override fun bindVertexArray(glVertexArray: GlVertexArray) {
        engineStats.calls++
        if (platform == Context.Platform.WEBGL2) {
            gl.bindVertexArray(glVertexArray.delegate)
        } else {
            gl.bindVertexArrayOES(glVertexArray.delegate)
        }
    }

    override fun bindDefaultVertexArray() {
        engineStats.calls++
        if (platform == Context.Platform.WEBGL2) {
            gl.bindVertexArray(null)
        } else {
            gl.bindVertexArrayOES(null)
        }
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

    override fun frameBufferTexture2D(
        target: Int,
        attachementType: FrameBufferRenderBufferAttachment,
        glTexture: GlTexture,
        level: Int
    ) {
        engineStats.calls++
        gl.framebufferTexture2D(target, attachementType.glFlag, GL.TEXTURE_2D, glTexture.delegate, level)
    }

    override fun readBuffer(mode: Int) {
        engineStats.calls++
        gl.readBuffer(mode)
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
            is DataSource.FloatBufferDataSource -> {
                data.buffer as FloatBufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
            is DataSource.ByteBufferDataSource -> {
                data.buffer as ByteBufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity)
            }
            is DataSource.ShortBufferDataSource -> {
                data.buffer as ShortBufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 2)
            }
            is DataSource.IntBufferDataSource -> {
                data.buffer as IntBufferImpl
                gl.bufferData(target, data.buffer.buffer, usage, 0, data.buffer.limit)
                engineStats.bufferAllocated(lastBoundBuffer!!.bufferId, data.buffer.capacity * 4)
            }
        }
        data.buffer.limit = limit
        data.buffer.position = pos
    }

    override fun bufferSubData(target: Int, offset: Int, data: DataSource) {
        engineStats.calls++
        val limit = data.buffer.limit
        val pos = data.buffer.position
        data.buffer.position = 0
        data.buffer.limit = data.buffer.capacity
        when (data) {
            is DataSource.FloatBufferDataSource -> {
                data.buffer as FloatBufferImpl
                gl.bufferSubData(target, offset, data.buffer.buffer)
            }
            is DataSource.ByteBufferDataSource -> {
                data.buffer as ByteBufferImpl
                gl.bufferSubData(target, offset, data.buffer.buffer)
            }
            is DataSource.ShortBufferDataSource -> {
                data.buffer as ShortBufferImpl
                gl.bufferSubData(target, offset, data.buffer.buffer)
            }
            is DataSource.IntBufferDataSource -> {
                data.buffer as IntBufferImpl
                gl.bufferSubData(target, offset, data.buffer.buffer)
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

    override fun depthRangef(zNear: Float, zFar: Float) {
        engineStats.calls++
        gl.depthRange(zNear, zFar)
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

    override fun validateProgram(glShaderProgram: GlShaderProgram) {
        engineStats.calls++
        gl.validateProgram(glShaderProgram.delegate)
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

    override fun bindDefaultTexture(target: TextureTarget) {
        engineStats.calls++
        gl.bindTexture(target.glFlag, null)
    }

    override fun deleteTexture(glTexture: GlTexture) {
        engineStats.calls++
        gl.deleteTexture(glTexture.delegate)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat3) {
        engineStats.calls++
        val buffer = createFloatBuffer(9) as FloatBufferImpl
        data.toBuffer(buffer)
        gl.uniformMatrix3fv(uniformLocation.delegate, transpose, buffer.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
        gl.uniformMatrix3fv(uniformLocation.delegate, transpose, data.buffer)
    }

    override fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        engineStats.calls++
        gl.uniformMatrix3fv(uniformLocation.delegate, transpose, data)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4) {
        engineStats.calls++
        val buffer = createFloatBuffer(16) as FloatBufferImpl
        data.toBuffer(buffer)
        gl.uniformMatrix4fv(uniformLocation.delegate, transpose, buffer.buffer)
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer) {
        engineStats.calls++
        data as FloatBufferImpl
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

    override fun compressedTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        source: ByteBuffer?
    ) {
        engineStats.calls++
        val dataView = (source as? ByteBufferImpl)?.buffer ?: Uint8Array(0)
        gl.compressedTexImage2D(target, level, internalFormat, width, height, 0, dataView)
    }

    override fun compressedTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        gl.compressedTexSubImage2D(target, level, xOffset, yOffset, width, height, format, source.buffer)
    }

    override fun copyTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) {
        engineStats.calls++
        gl.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
    }

    override fun copyTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        engineStats.calls++
        gl.copyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
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
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        gl.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, source.buffer)
    }

    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int
    ) {
        engineStats.calls++
        gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, null)
    }


    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        gl.texImage2D(
            target, level, internalFormat, width, height, 0, format, type,
            Uint8Array(source.toArray().toTypedArray()) // convert it to a uint8array or else webgl fails to render
        )
    }

    override fun compressedTexImage3D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        depth: Int,
        source: ByteBuffer?
    ) {
        engineStats.calls++
        val dataView = (source as? ByteBufferImpl)?.buffer ?: Uint8Array(0)
        gl.compressedTexImage3D(target, level, internalFormat, width, height, depth, 0, dataView)
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
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        gl.compressedTexSubImage3D(
            target,
            level,
            xOffset,
            yOffset,
            zOffset,
            width,
            height,
            depth,
            format,
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
        height: Int
    ) {
        engineStats.calls++
        gl.copyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height)
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
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl
        gl.texSubImage3D(target, level, xOffset, yOffset, zOffset, width, height, depth, format, type, source.buffer)
    }

    override fun texImage3D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        depth: Int,
        type: Int
    ) {
        engineStats.calls++
        gl.texImage3D(target, level, internalFormat, width, height, depth, 0, format, type, null as Int8Array?)
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
        source: ByteBuffer
    ) {
        engineStats.calls++
        source as ByteBufferImpl

        gl.texImage3D(
            target, level, internalFormat, width, height, depth, 0, format, type,
            Uint8Array(source.toArray().toTypedArray()) // convert it to a uint8array or else webgl fails to render
        )
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) {
        engineStats.calls++
        gl.texParameteri(target, pname, param)
    }

    override fun texParameterf(target: Int, pname: Int, param: Float) {
        engineStats.calls++
        gl.texParameterf(target, pname, param)
    }

    override fun generateMipmap(target: Int) {
        engineStats.calls++
        gl.generateMipmap(target)
    }

}