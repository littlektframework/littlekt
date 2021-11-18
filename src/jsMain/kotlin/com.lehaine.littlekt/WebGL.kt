package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.shader.*

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
class WebGL : GL {
    override fun clearColor(r: Percent, g: Percent, b: Percent, a: Percent) {
        TODO("Not yet implemented")
    }

    override fun clear(mask: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun clearDepth(depth: Number) {
        TODO("Not yet implemented")
    }

    override fun enable(mask: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun disable(mask: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun blendFunc(sfactor: ByteMask, dfactor: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun createProgram(): ShaderProgram {
        TODO("Not yet implemented")
    }

    override fun getAttribLocation(shaderProgram: ShaderProgram, name: String): Int {
        TODO("Not yet implemented")
    }

    override fun getUniformLocation(shaderProgram: ShaderProgram, name: String): Uniform {
        TODO("Not yet implemented")
    }

    override fun attachShader(shaderProgram: ShaderProgram, shader: Shader) {
        TODO("Not yet implemented")
    }

    override fun linkProgram(shaderProgram: ShaderProgram) {
        TODO("Not yet implemented")
    }

    override fun getString(parameterName: Int): String? {
        TODO("Not yet implemented")
    }

    override fun getProgramParameter(shaderProgram: ShaderProgram, mask: ByteMask): Any {
        TODO("Not yet implemented")
    }

    override fun getShaderParameter(shader: Shader, mask: ByteMask): Any {
        TODO("Not yet implemented")
    }

    override fun createShader(type: ByteMask): Shader {
        TODO("Not yet implemented")
    }

    override fun shaderSource(shader: Shader, source: String) {
        TODO("Not yet implemented")
    }

    override fun compileShader(shader: Shader) {
        TODO("Not yet implemented")
    }

    override fun getShaderInfoLog(shader: Shader): String {
        TODO("Not yet implemented")
    }

    override fun deleteShader(shader: Shader) {
        TODO("Not yet implemented")
    }

    override fun getProgramInfoLog(shader: ShaderProgram): String {
        TODO("Not yet implemented")
    }

    override fun createBuffer(): Buffer {
        TODO("Not yet implemented")
    }

    override fun createFrameBuffer(): FrameBufferReference {
        TODO("Not yet implemented")
    }

    override fun bindFrameBuffer(frameBufferReference: FrameBufferReference) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultFrameBuffer() {
        TODO("Not yet implemented")
    }

    override fun createRenderBuffer(): RenderBufferReference {
        TODO("Not yet implemented")
    }

    override fun bindRenderBuffer(renderBufferReference: RenderBufferReference) {
        TODO("Not yet implemented")
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun framebufferRenderbuffer(attachementType: Int, renderBufferReference: RenderBufferReference) {
        TODO("Not yet implemented")
    }

    override fun frameBufferTexture2D(attachmentPoint: Int, textureReference: TextureReference, level: Int) {
        TODO("Not yet implemented")
    }

    override fun bindBuffer(target: ByteMask, buffer: Buffer) {
        TODO("Not yet implemented")
    }

    override fun bufferData(target: ByteMask, data: DataSource, usage: Int) {
        TODO("Not yet implemented")
    }

    override fun depthFunc(target: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun enableVertexAttribArray(index: Int) {
        TODO("Not yet implemented")
    }

    override fun useProgram(shaderProgram: ShaderProgram) {
        TODO("Not yet implemented")
    }

    override fun createTexture(): TextureReference {
        TODO("Not yet implemented")
    }

    override fun activeTexture(byteMask: ByteMask) {
        TODO("Not yet implemented")
    }

    override fun bindTexture(target: Int, textureReference: TextureReference) {
        TODO("Not yet implemented")
    }

    override fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: Array<Float>) {
        TODO("Not yet implemented")
    }

    override fun uniform1i(uniform: Uniform, data: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform2i(uniform: Uniform, a: Int, b: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform3i(uniform: Uniform, a: Int, b: Int, c: Int) {
        TODO("Not yet implemented")
    }

    override fun uniform1f(uniform: Uniform, first: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform2f(uniform: Uniform, first: Float, second: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform3f(uniform: Uniform, first: Float, second: Float, third: Float) {
        TODO("Not yet implemented")
    }

    override fun uniform4f(uniform: Uniform, first: Float, second: Float, third: Float, fourth: Float) {
        TODO("Not yet implemented")
    }

    override fun drawArrays(mask: ByteMask, offset: Int, vertexCount: Int) {
        TODO("Not yet implemented")
    }

    override fun drawElements(mask: ByteMask, vertexCount: Int, type: Int, offset: Int) {
        TODO("Not yet implemented")
    }

    override fun viewport(x: Pixel, y: Pixel, width: Pixel, height: Pixel) {
        TODO("Not yet implemented")
    }

    override fun texImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        format: Int,
        type: Int,
        source: TextureData
    ) {
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