package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.BufferReference
import com.lehaine.littlekt.graphics.VertexArrayReference
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

    override fun createProgram(): ShaderProgramReference {
        TODO("Not yet implemented")
    }

    override fun getAttribLocation(shaderProgram: ShaderProgramReference, name: String): Int {
        TODO("Not yet implemented")
    }

    override fun getUniformLocation(shaderProgram: ShaderProgramReference, name: String): Uniform {
        TODO("Not yet implemented")
    }

    override fun attachShader(shaderProgram: ShaderProgramReference, shaderReference: ShaderReference) {
        TODO("Not yet implemented")
    }

    override fun linkProgram(shaderProgram: ShaderProgramReference) {
        TODO("Not yet implemented")
    }

    override fun deleteProgram(shaderProgram: ShaderProgramReference) {
        TODO("Not yet implemented")
    }

    override fun getString(parameterName: Int): String? {
        TODO("Not yet implemented")
    }

    override fun getProgramParameter(shaderProgram: ShaderProgramReference, mask: Int): Any {
        TODO("Not yet implemented")
    }

    override fun getShaderParameter(shaderReference: ShaderReference, mask: Int): Any {
        TODO("Not yet implemented")
    }

    override fun createShader(type: Int): ShaderReference {
        TODO("Not yet implemented")
    }

    override fun shaderSource(shaderReference: ShaderReference, source: String) {
        TODO("Not yet implemented")
    }

    override fun compileShader(shaderReference: ShaderReference) {
        TODO("Not yet implemented")
    }

    override fun getShaderInfoLog(shaderReference: ShaderReference): String {
        TODO("Not yet implemented")
    }

    override fun deleteShader(shaderReference: ShaderReference) {
        TODO("Not yet implemented")
    }

    override fun getProgramInfoLog(shader: ShaderProgramReference): String {
        TODO("Not yet implemented")
    }

    override fun createBuffer(): com.lehaine.littlekt.graphics.BufferReference {
        TODO("Not yet implemented")
    }

    override fun createFrameBuffer(): com.lehaine.littlekt.graphics.FrameBufferReference {
        TODO("Not yet implemented")
    }

    override fun createVertexArray(): VertexArrayReference {
        TODO("Not yet implemented")
    }

    override fun bindVertexArray(vertexArrayReference: VertexArrayReference) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultVertexArray() {
        TODO("Not yet implemented")
    }

    override fun bindFrameBuffer(frameBufferReference: com.lehaine.littlekt.graphics.FrameBufferReference) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultFrameBuffer() {
        TODO("Not yet implemented")
    }

    override fun createRenderBuffer(): com.lehaine.littlekt.graphics.RenderBufferReference {
        TODO("Not yet implemented")
    }

    override fun bindRenderBuffer(renderBufferReference: com.lehaine.littlekt.graphics.RenderBufferReference) {
        TODO("Not yet implemented")
    }

    override fun renderBufferStorage(internalformat: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun framebufferRenderbuffer(attachementType: Int, renderBufferReference: com.lehaine.littlekt.graphics.RenderBufferReference) {
        TODO("Not yet implemented")
    }

    override fun frameBufferTexture2D(attachmentPoint: Int, textureReference: com.lehaine.littlekt.graphics.TextureReference, level: Int) {
        TODO("Not yet implemented")
    }

    override fun bindBuffer(target: Int, bufferReference: com.lehaine.littlekt.graphics.BufferReference) {
        TODO("Not yet implemented")
    }

    override fun bindDefaultBuffer(target: Int) {
        TODO("Not yet implemented")
    }

    override fun deleteBuffer(bufferReference: BufferReference) {
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

    override fun useProgram(shaderProgram: ShaderProgramReference) {
        TODO("Not yet implemented")
    }

    override fun useDefaultProgram() {
        TODO("Not yet implemented")
    }

    override fun createTexture(): com.lehaine.littlekt.graphics.TextureReference {
        TODO("Not yet implemented")
    }

    override fun activeTexture(Int: Int) {
        TODO("Not yet implemented")
    }

    override fun bindTexture(target: Int, textureReference: com.lehaine.littlekt.graphics.TextureReference) {
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

    override fun drawArrays(mask: Int, offset: Int, vertexCount: Int) {
        TODO("Not yet implemented")
    }

    override fun drawElements(mask: Int, vertexCount: Int, type: Int, offset: Int) {
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