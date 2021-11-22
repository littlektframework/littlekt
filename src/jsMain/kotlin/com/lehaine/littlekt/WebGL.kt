package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.gl.GlShaderProgram
import com.lehaine.littlekt.graphics.gl.GlShader
import com.lehaine.littlekt.graphics.gl.UniformLocation
import com.lehaine.littlekt.graphics.shader.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class WebGL : GL {
    override fun getGLVersion(): GLVersion {
        TODO("Not yet implemented")
    }

    override fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        TODO("Not yet implemented")
    }

    override fun clear(mask: Int) {
        TODO("Not yet implemented")
    }

    override fun clearDepth(depth: Number) {
        TODO("Not yet implemented")
    }

    override fun enable(mask: Int) {
        TODO("Not yet implemented")
    }

    override fun disable(mask: Int) {
        TODO("Not yet implemented")
    }

    override fun blendFunc(sfactor: Int, dfactor: Int) {
        TODO("Not yet implemented")
    }

    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        TODO("Not yet implemented")
    }

    override fun createProgram(): GlShaderProgram {
        TODO("Not yet implemented")
    }

    override fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int {
        TODO("Not yet implemented")
    }

    override fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation {
        TODO("Not yet implemented")
    }

    override fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader) {
        TODO("Not yet implemented")
    }

    override fun linkProgram(glShaderProgram: GlShaderProgram) {
        TODO("Not yet implemented")
    }

    override fun deleteProgram(glShaderProgram: GlShaderProgram) {
        TODO("Not yet implemented")
    }

    override fun getString(parameterName: Int): String? {
        TODO("Not yet implemented")
    }

    override fun getProgramParameter(glShaderProgram: GlShaderProgram, mask: Int): Any {
        TODO("Not yet implemented")
    }

    override fun getShaderParameter(glShader: GlShader, mask: Int): Any {
        TODO("Not yet implemented")
    }

    override fun createShader(type: Int): GlShader {
        TODO("Not yet implemented")
    }

    override fun shaderSource(glShader: GlShader, source: String) {
        TODO("Not yet implemented")
    }

    override fun compileShader(glShader: GlShader) {
        TODO("Not yet implemented")
    }

    override fun getShaderInfoLog(glShader: GlShader): String {
        TODO("Not yet implemented")
    }

    override fun deleteShader(glShader: GlShader) {
        TODO("Not yet implemented")
    }

    override fun getProgramInfoLog(glShader: GlShaderProgram): String {
        TODO("Not yet implemented")
    }

    override fun createBuffer(): com.lehaine.littlekt.graphics.gl.GlBuffer {
        TODO("Not yet implemented")
    }

    override fun createFrameBuffer(): com.lehaine.littlekt.graphics.gl.GlFrameBuffer {
        TODO("Not yet implemented")
    }

    override fun createVertexArray(): com.lehaine.littlekt.graphics.gl.GlVertexArray {
        TODO("Not yet implemented")
    }

    override fun bindVertexArray(glVertexArray: com.lehaine.littlekt.graphics.gl.GlVertexArray) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultVertexArray() {
        TODO("Not yet implemented")
    }

    override fun bindFrameBuffer(glFrameBuffer: com.lehaine.littlekt.graphics.gl.GlFrameBuffer) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultFrameBuffer() {
        TODO("Not yet implemented")
    }

    override fun createRenderBuffer(): com.lehaine.littlekt.graphics.gl.GlRenderBuffer {
        TODO("Not yet implemented")
    }

    override fun bindRenderBuffer(glRenderBuffer: com.lehaine.littlekt.graphics.gl.GlRenderBuffer) {
        TODO("Not yet implemented")
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun framebufferRenderbuffer(attachementType: Int, glRenderBuffer: com.lehaine.littlekt.graphics.gl.GlRenderBuffer) {
        TODO("Not yet implemented")
    }

    override fun frameBufferTexture2D(attachmentPoint: Int, glTexture: com.lehaine.littlekt.graphics.gl.GlTexture, level: Int) {
        TODO("Not yet implemented")
    }

    override fun bindBuffer(target: Int, glBuffer: com.lehaine.littlekt.graphics.gl.GlBuffer) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultBuffer(target: Int) {
        TODO("Not yet implemented")
    }

    override fun deleteBuffer(glBuffer: com.lehaine.littlekt.graphics.gl.GlBuffer) {
        TODO("Not yet implemented")
    }

    override fun bufferData(target: Int, data: DataSource, usage: Int) {
        TODO("Not yet implemented")
    }

    override fun depthFunc(target: Int) {
        TODO("Not yet implemented")
    }

    override fun depthMask(flag: Boolean) {
        TODO("Not yet implemented")
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun enableVertexAttribArray(index: Int) {
        TODO("Not yet implemented")
    }

    override fun disableVertexAttribArray(index: Int) {
        TODO("Not yet implemented")
    }

    override fun useProgram(glShaderProgram: GlShaderProgram) {
        TODO("Not yet implemented")
    }

    override fun useDefaultProgram() {
        TODO("Not yet implemented")
    }

    override fun createTexture(): com.lehaine.littlekt.graphics.gl.GlTexture {
        TODO("Not yet implemented")
    }

    override fun activeTexture(int: Int) {
        TODO("Not yet implemented")
    }

    override fun bindTexture(target: Int, glTexture: com.lehaine.littlekt.graphics.gl.GlTexture) {
        TODO("Not yet implemented")
    }

    override fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>) {
        TODO("Not yet implemented")
    }

    override fun uniform1i(uniformLocation: UniformLocation, data: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform2i(uniformLocation: UniformLocation, a: Int, b: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform3i(uniformLocation: UniformLocation, a: Int, b: Int, c: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform1f(uniformLocation: UniformLocation, first: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform2f(uniformLocation: UniformLocation, first: Float, second: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform3f(uniformLocation: UniformLocation, first: Float, second: Float, third: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform4f(uniformLocation: UniformLocation, first: Float, second: Float, third: Float, fourth: Float) {
        TODO("Not yet implemented")
    }

    override fun drawArrays(mode: Int, offset: Int, vertexCount: Int) {
        TODO("Not yet implemented")
    }

    override fun drawElements(mode: Int, vertexCount: Int, type: Int, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun pixelStorei(pname: Int, param: Int) {
        TODO("Not yet implemented")
    }

    override fun vertex2f(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun viewport(x: Int, y: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun texParameteri(target: Int, paramName: Int, paramValue: Int) {
        TODO("Not yet implemented")
    }

    override fun generateMipmap(target: Int) {
        TODO("Not yet implemented")
    }

}