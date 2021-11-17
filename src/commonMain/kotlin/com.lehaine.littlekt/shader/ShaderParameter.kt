package com.lehaine.littlekt.shader

import com.lehaine.littlekt.GL
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vector2
import com.lehaine.littlekt.math.Vector3
import com.lehaine.littlekt.graphics.render.Color
import kotlin.jvm.JvmName

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
sealed class ShaderParameter(val name: String) {
    abstract fun create(program: ShaderProgram)

    class UniformMat4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, matrix: Mat4) {
            program.uniformMatrix4fv(program.getUniform(name), false, matrix)
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
                val values = mat.asGLArray()
                (0 until 16).forEach { y ->
                    tmpMatrix[x * 16 + y] = values[y]
                }
            }
            program.uniformMatrix4fv(program.getUniform(name), false, tmpMatrix)
        }
    }

    class UniformInt(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vararg value: Int) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.uniform1i(program.getUniform(name), value[0])
                2 -> program.uniform2i(program.getUniform(name), value[0], value[1])
                3 -> program.uniform3i(program.getUniform(name), value[0], value[1], value[2])
            }
        }
    }

    class UniformVec2(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vec2: Vector2) = apply(program, vec2.x, vec2.y)

        fun apply(program: ShaderProgram, vararg vec2: Float) {
            when (vec2.size) {
                2 -> program.uniform2f(program.getUniform(name), vec2[0], vec2[1])
                else -> throw IllegalArgumentException("3 values are expected. ${vec2.size} received")
            }
        }
    }

    class UniformVec3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, vec3: Vector3) = apply(program, vec3.x, vec3.y, vec3.z)

        fun apply(program: ShaderProgram, vararg vec3: Float) {
            when (vec3.size) {
                3 -> program.uniform3f(program.getUniform(name), vec3[0], vec3[1], vec3[2])
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
            color.red.toFloat(),
            color.green.toFloat(),
            color.blue.toFloat(),
            color.alpha.toFloat()
        )

        fun apply(program: ShaderProgram, color: Color, intensity: Float) = apply(
            program,
            color.red.toFloat() * intensity,
            color.green.toFloat() * intensity,
            color.blue.toFloat() * intensity,
            color.alpha.toFloat() * intensity
        )

        fun apply(program: ShaderProgram, vararg vec4: Float) {
            when (vec4.size) {
                4 -> program.uniform4f(program.getUniform(name), vec4[0], vec4[1], vec4[2], vec4[3])
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
                1 -> program.uniform1f(program.getUniform(name), value[0])
                2 -> program.uniform2f(program.getUniform(name), value[0], value[1])
                3 -> program.uniform3f(program.getUniform(name), value[0], value[1], value[2])
                4 -> program.uniform4f(program.getUniform(name), value[0], value[1], value[2], value[3])
            }
        }
    }

    class AttributeVec2(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }

        fun apply(program: ShaderProgram, source: Buffer) {
            program.bindBuffer(GL.ARRAY_BUFFER, source)
            program.vertexAttribPointer(
                index = program.getAttrib(name),
                size = 2,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0
            )
            program.enableVertexAttribArray(program.getAttrib(name))
        }
    }

    class AttributeVec3(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }

        fun apply(program: ShaderProgram, source: Buffer) {
            program.bindBuffer(GL.ARRAY_BUFFER, source)
            program.vertexAttribPointer(
                index = program.getAttrib(name),
                size = 3,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0
            )
            program.enableVertexAttribArray(program.getAttrib(name))
        }
    }

    class AttributeVec4(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createAttrib(name)
        }

        fun apply(program: ShaderProgram, source: Buffer) {
            program.bindBuffer(GL.ARRAY_BUFFER, source)
            program.vertexAttribPointer(
                index = program.getAttrib(name),
                size = 4,
                type = GL.FLOAT,
                normalized = false,
                stride = 0,
                offset = 0
            )
            program.enableVertexAttribArray(program.getAttrib(name))
        }
    }

    class UniformSample2D(name: String) : ShaderParameter(name) {
        override fun create(program: ShaderProgram) {
            program.createUniform(name)
        }

        fun apply(program: ShaderProgram, texture: TextureReference, unit: Int = 0) {
            program.activeTexture(GL.TEXTURE0 + unit)
            program.bindTexture(GL.TEXTURE_2D, texture)
            program.uniform1i(program.getUniform(name), unit)
        }
    }
}