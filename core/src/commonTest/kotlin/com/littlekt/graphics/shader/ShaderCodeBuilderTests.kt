package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.ShaderStructParameterType
import com.littlekt.graphics.shader.builder.shaderStruct
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCodeBuilderTests {

    @Test
    fun testSimpleShader() {
        val inputStruct =
            shaderStruct("VertexInput") {
                mapOf(
                    "position" to ShaderStructParameterType.WgslType.vec3f,
                    "color" to ShaderStructParameterType.WgslType.vec4f,
                )
            }
        val outputStruct =
            shaderStruct("VertexOutput") {
                mapOf(
                    "position" to
                        ShaderStructParameterType.BuiltIn.Position(
                            ShaderStructParameterType.WgslType.vec4f
                        ),
                    "color" to
                        ShaderStructParameterType.Location(
                            0,
                            ShaderStructParameterType.WgslType.vec4f,
                        ),
                )
            }

        println(inputStruct.src)
        println(outputStruct.src)
    }

    @Test
    fun testSimpleStructOffsets() {
        val ex1 =
            shaderStruct("Ex1") {
                mapOf(
                    "velocity" to ShaderStructParameterType.WgslType.f32,
                    "acceleration" to ShaderStructParameterType.WgslType.f32,
                    "frameCount" to ShaderStructParameterType.WgslType.u32,
                )
            }
        assertEquals(ex1.layout["velocity"]?.offset, 0)
        assertEquals(ex1.layout["acceleration"]?.offset, 4)
        assertEquals(ex1.layout["frameCount"]?.offset, 8)
    }

    @Test
    fun testStructOffsetsWithVecAndMatrix() {
        val ex2 =
            shaderStruct("Ex2") {
                mapOf(
                    "scale" to ShaderStructParameterType.WgslType.f32,
                    "offset" to ShaderStructParameterType.WgslType.vec3f,
                    "projection" to ShaderStructParameterType.WgslType.mat4x4f,
                )
            }
        assertEquals(ex2.layout["scale"]?.offset, 0)
        assertEquals(ex2.layout["offset"]?.offset, 16)
        assertEquals(ex2.layout["projection"]?.offset, 32)
    }

    @Test
    fun testStructWithArrayParamsOffsets() {
        val ex3 =
            shaderStruct("Ex3") {
                mapOf(
                    "transform" to ShaderStructParameterType.WgslType.mat3x3f,
                    "directions" to
                        ShaderStructParameterType.Array(ShaderStructParameterType.WgslType.vec3f, 4),
                )
            }

        assertEquals(ex3.layout["transform"]?.offset, 0)
        assertEquals(ex3.layout["directions"]?.offset, 48)
    }

    @Test
    fun testStructsOffsetWithStructAndArrayParamsOffsets() {
        val ex4a =
            shaderStruct("Ex4a") { mapOf("velocity" to ShaderStructParameterType.WgslType.vec3f) }
        val ex4 =
            shaderStruct("Ex4") {
                mapOf(
                    "orientation" to ShaderStructParameterType.WgslType.vec3f,
                    "size" to ShaderStructParameterType.WgslType.f32,
                    "direction" to
                        ShaderStructParameterType.Array(
                            ShaderStructParameterType.WgslType.vec3f,
                            1,
                        ),
                    "scale" to ShaderStructParameterType.WgslType.f32,
                    "info" to ShaderStructParameterType.Struct(ex4a),
                    "friction" to ShaderStructParameterType.WgslType.f32,
                )
            }

        assertEquals(ex4.layout["orientation"]?.offset, 0)
        assertEquals(ex4.layout["size"]?.offset, 12)
        assertEquals(ex4.layout["direction"]?.offset, 16)
        assertEquals(ex4.layout["scale"]?.offset, 32)
        assertEquals(ex4.layout["info"]?.offset, 48)
        assertEquals(ex4.layout["friction"]?.offset, 64)
    }
}
