package com.lehaine.littlekt.shader

import com.lehaine.littlekt.GL

class ShaderProgram(val gl: GL, val program: PlatformShaderProgram) : GL by gl {

    private val attributes = mutableMapOf<String, Int>()

    private val uniforms = mutableMapOf<String, Uniform>()

    fun createAttrib(name: String) {
        attributes[name] = gl.getAttribLocation(this, name)
    }

    fun createUniform(name: String) {
        uniforms[name] = gl.getUniformLocation(this, name)
    }

    fun getAttrib(name: String): Int =
        attributes[name] ?: throw IllegalStateException("Attributes '$name' not created!")

    fun getUniform(name: String): Uniform {
        return uniforms[name] ?: throw IllegalStateException("Uniform '$name' not created!")
    }
}