package com.lehaine.littlekt.graphics.shader

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.gl.GlTexture
import com.lehaine.littlekt.graphics.gl.TextureTarget
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderParameter

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    class UniformMat3(name: String) : ShaderParameter(name) {
        fun apply(program: ShaderProgram<*, *>, matrix: Mat3) {
            program.gl?.uniformMatrix3fv(program.getUniform(name), false, matrix)
        }
    }

    class UniformArrayMat3(name: String) : ShaderParameter(name) {
        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, matrix: Array<Mat3>) = apply(program, *matrix)

        fun apply(program: ShaderProgram<*, *>, matrix: List<Mat3>) = apply(program, matrix.toTypedArray())

        fun apply(program: ShaderProgram<*, *>, vararg matrix: Mat3) {
            val tmpMatrix = Array(matrix.size * 16) { 0f }

            // Copy all matrix values, aligned
            matrix.forEachIndexed { x, mat ->
                (0 until 16).forEach { y ->
                    tmpMatrix[x * 16 + y] = mat.data[y]
                }
            }
            program.gl?.uniformMatrix3fv(program.getUniform(name), false, tmpMatrix)
        }
    }

    class UniformMat4(name: String) : ShaderParameter(name) {
        fun apply(program: ShaderProgram<*, *>, matrix: Mat4) {
            program.gl?.uniformMatrix4fv(program.getUniform(name), false, matrix)
        }
    }

    class UniformArrayMat4(name: String) : ShaderParameter(name) {

        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, matrix: Array<Mat4>) = apply(program, *matrix)

        fun apply(program: ShaderProgram<*, *>, matrix: List<Mat4>) = apply(program, matrix.toTypedArray())

        fun apply(program: ShaderProgram<*, *>, vararg matrix: Mat4) {
            val tmpMatrix = Array(matrix.size * 16) { 0f }

            // Copy all matrix values, aligned
            matrix.forEachIndexed { x, mat ->
                (0 until 16).forEach { y ->
                    tmpMatrix[x * 16 + y] = mat.data[y]
                }
            }
            program.gl?.uniformMatrix4fv(program.getUniform(name), false, tmpMatrix)
        }
    }

    class UniformInt(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, vararg value: Int) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.gl?.uniform1i(program.getUniform(name), value[0])
                2 -> program.gl?.uniform2i(program.getUniform(name), value[0], value[1])
                3 -> program.gl?.uniform3i(program.getUniform(name), value[0], value[1], value[2])
            }
        }
    }

    class UniformVec2(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, vec2: Vec2f) = apply(program, vec2.x, vec2.y)

        fun apply(program: ShaderProgram<*, *>, vararg vec2: Float) {
            when (vec2.size) {
                2 -> program.gl?.uniform2f(program.getUniform(name), vec2[0], vec2[1])
                else -> throw IllegalArgumentException("3 values are expected. ${vec2.size} received")
            }
        }
    }

    class UniformVec3(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, vec3: Vec3f) = apply(program, vec3.x, vec3.y, vec3.z)

        fun apply(program: ShaderProgram<*, *>, vararg vec3: Float) {
            when (vec3.size) {
                3 -> program.gl?.uniform3f(program.getUniform(name), vec3[0], vec3[1], vec3[2])
                else -> throw IllegalArgumentException("3 values are expected. ${vec3.size} received")
            }
        }
    }

    class UniformVec4(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, color: Color) = apply(
            program,
            color.r,
            color.g,
            color.b,
            color.a
        )

        fun apply(program: ShaderProgram<*, *>, color: Color, intensity: Float) = apply(
            program,
            color.r * intensity,
            color.g * intensity,
            color.b * intensity,
            color.a * intensity
        )

        fun apply(program: ShaderProgram<*, *>, vararg vec4: Float) {
            when (vec4.size) {
                4 -> program.gl?.uniform4f(program.getUniform(name), vec4[0], vec4[1], vec4[2], vec4[3])
                else -> throw IllegalArgumentException("4 values are expected. ${vec4.size} received")
            }
        }
    }

    class UniformFloat(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, vararg value: Float) {
            when (value.size) {
                0 -> throw IllegalArgumentException("At least one int is expected")
                1 -> program.gl?.uniform1f(program.getUniform(name), value[0])
                2 -> program.gl?.uniform2f(program.getUniform(name), value[0], value[1])
                3 -> program.gl?.uniform3f(program.getUniform(name), value[0], value[1], value[2])
                4 -> program.gl?.uniform4f(program.getUniform(name), value[0], value[1], value[2], value[3])
            }
        }
    }

    class UniformArrayFloat(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, f: Array<Float>) {
            program.gl?.uniform1fv(program.getUniform(name), f)
        }

        fun apply(program: ShaderProgram<*, *>, f: FloatArray) {
            program.gl?.uniform1fv(program.getUniform(name), f)
        }

        fun apply(program: ShaderProgram<*, *>, f: List<Float>) = apply(program, f.toTypedArray())
    }

    class UniformArrayVec2(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, f: Array<Float>) {
            program.gl?.uniform2fv(program.getUniform(name), f)
        }

        fun apply(program: ShaderProgram<*, *>, f: FloatArray) {
            program.gl?.uniform2fv(program.getUniform(name), f)
        }

        fun apply(program: ShaderProgram<*, *>, f: List<Float>) = apply(program, f.toTypedArray())
    }

    class UniformArrayVec3(name: String) : ShaderParameter(name) {

        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, f: Array<Float>) {
            program.gl?.uniform3fv(program.getUniform(name), f)
        }

        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, f: FloatArray) {
            program.gl?.uniform3fv(program.getUniform(name), f)
        }

        fun apply(program: ShaderProgram<*, *>, f: List<Float>) = apply(program, f.toTypedArray())
    }

    class UniformArrayVec4(name: String) : ShaderParameter(name) {

        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, f: Array<Float>) {
            program.gl?.uniform4fv(program.getUniform(name), f)
        }

        @JvmName("applyArray")
        fun apply(program: ShaderProgram<*, *>, f: FloatArray) {
            program.gl?.uniform4fv(program.getUniform(name), f)
        }

        @JvmName("applyColors")
        fun apply(program: ShaderProgram<*, *>, colors: List<Color>) {
            val floats = colors.flatMap { c -> listOf(c.r, c.g, c.b, c.a) }
            apply(program, floats)
        }

        fun apply(program: ShaderProgram<*, *>, f: List<Float>) = apply(program, f.toTypedArray())
    }

    class UniformSample2D(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, glTexture: GlTexture, unit: Int = 0) {
            program.gl?.activeTexture(GL.TEXTURE0 + unit)
            program.gl?.bindTexture(TextureTarget._2D, glTexture)
            program.gl?.uniform1i(program.getUniform(name), unit)
        }

        fun apply(program: ShaderProgram<*, *>, unit: Int = 0) {
            program.gl?.uniform1i(program.getUniform(name), unit)
        }
    }

    class UniformBoolean(name: String) : ShaderParameter(name) {

        fun apply(program: ShaderProgram<*, *>, value: Boolean) {
            program.gl?.uniform1i(program.getUniform(name), if (value) 1 else 0)
        }
    }

    class Attribute(name: String) : ShaderParameter(name)
}