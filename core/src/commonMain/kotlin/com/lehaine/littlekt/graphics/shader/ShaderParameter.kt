package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.gl.GlTexture
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.Vec3f
import kotlin.jvm.JvmName

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class ShaderParameter(val name: String) {
    abstract fun create(program: ShaderProgram)

    class UniformMat3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, matrix: Mat3) {
            program.gl.uniformMatrix3fv(program.getUniform(name), false, matrix)
        }
    }

    class UniformArrayMat3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        @JvmName("applyArray")
        fun apply(program: ShaderProgram, matrix: Array<Mat3>) = apply(program, *matrix)

        fun apply(program: ShaderProgram, matrix: List<Mat3>) = apply(program, matrix.toTypedArray())

        fun apply(program: ShaderProgram, vararg matrix: Mat3) {
            val tmpMatrix = Array(matrix.size * 16) { 0f }

            // Copy all matrix values, aligned
            matrix.forEachIndexed { x, mat ->
                val values = mat.toList()
                (0 until 16).forEach { y ->
                    tmpMatrix[x * 16 + y] = values[y]
                }
            }
            program.gl.uniformMatrix3fv(program.getUniform(name), false, tmpMatrix)
        }
    }

    class UniformMat4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, matrix: Mat4) {
            program.gl.uniformMatrix4fv(program.getUniform(name), false, matrix)
        }
    }

    class UniformArrayMat4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        @JvmName("applyArray")
        fun apply(program: ShaderProgram, matrix: Array<Mat4>) = apply(program, *matrix)

        fun apply(program: ShaderProgram, matrix: List<Mat4>) = apply(program, matrix.toTypedArray())

        fun apply(program: ShaderProgram, vararg matrix: Mat4) {
            val tmpMatrix = Array(matrix.size * 16) { 0f }

            // Copy all matrix values, aligned
            matrix.forEachIndexed { x, mat ->
                val values = mat.toList()
                (0 until 16).forEach { y ->
                    tmpMatrix[x * 16 + y] = values[y]
                }
            }
            program.gl.uniformMatrix4fv(program.getUniform(name), false, tmpMatrix)
        }
    }

    class UniformInt(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vararg value: Int) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.gl.uniform1i(program.getUniform(name), value[0])
                2 -> program.gl.uniform2i(program.getUniform(name), value[0], value[1])
                3 -> program.gl.uniform3i(program.getUniform(name), value[0], value[1], value[2])
            }
        }
    }

    class UniformVec2(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vec2: Vec2f) = apply(program, vec2.x, vec2.y)

        fun apply(program: ShaderProgram, vararg vec2: Float) {
            when (vec2.size) {
                2 -> program.gl.uniform2f(program.getUniform(name), vec2[0], vec2[1])
                else -> throw IllegalArgumentException("3 values are expected. ${vec2.size} received")
            }
        }
    }

    class UniformVec3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vec3: Vec3f) = apply(program, vec3.x, vec3.y, vec3.z)

        fun apply(program: ShaderProgram, vararg vec3: Float) {
            when (vec3.size) {
                3 -> program.gl.uniform3f(program.getUniform(name), vec3[0], vec3[1], vec3[2])
                else -> throw IllegalArgumentException("3 values are expected. ${vec3.size} received")
            }
        }
    }

    class UniformVec4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, color: Color) = apply(
            program,
            color.r,
            color.g,
            color.b,
            color.a
        )

        fun apply(program: ShaderProgram, color: Color, intensity: Float) = apply(
            program,
            color.r * intensity,
            color.g * intensity,
            color.b * intensity,
            color.a * intensity
        )

        fun apply(program: ShaderProgram, vararg vec4: Float) {
            when (vec4.size) {
                4 -> program.gl.uniform4f(program.getUniform(name), vec4[0], vec4[1], vec4[2], vec4[3])
                else -> throw IllegalArgumentException("4 values are expected. ${vec4.size} received")
            }
        }
    }

    class UniformFloat(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vararg value: Float) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.gl.uniform1f(program.getUniform(name), value[0])
                2 -> program.gl.uniform2f(program.getUniform(name), value[0], value[1])
                3 -> program.gl.uniform3f(program.getUniform(name), value[0], value[1], value[2])
                4 -> program.gl.uniform4f(program.getUniform(name), value[0], value[1], value[2], value[3])
            }
        }
    }

    class UniformSample2D(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, glTexture: GlTexture, unit: Int = 0) {
            program.gl.activeTexture(GL.TEXTURE0 + unit)
            program.gl.bindTexture(GL.TEXTURE_2D, glTexture)
            program.gl.uniform1i(program.getUniform(name), unit)
        }

        fun apply(program: ShaderProgram, unit: Int = 0) {
            program.gl.uniform1i(program.getUniform(name), unit)
        }
    }

    class AttributeVec2(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }
    }

    class AttributeVec3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }
    }

    class AttributeVec4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }
    }
}