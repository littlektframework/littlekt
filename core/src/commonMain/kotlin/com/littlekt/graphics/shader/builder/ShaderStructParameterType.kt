package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
sealed class ShaderStructParameterType {
    data class Struct(val struct: ShaderStruct) : ShaderStructParameterType()

    sealed class WgslType(val elements: Int, val size: Int, val alignment: Int, val type: String) :
        ShaderStructParameterType() {
        data object bool : WgslType(0, 0, 1, "bool")

        data object i32 : WgslType(1, 4, 4, "i32")

        data object u32 : WgslType(1, 4, 4, "u32")

        data object f16 : WgslType(1, 2, 2, "f16")

        data object f32 : WgslType(1, 4, 4, "f32")

        data object vec2f : WgslType(2, 8, 8, "vec2f")

        data object vec3f : WgslType(3, 12, 16, "vec3f")

        data object vec4f : WgslType(4, 16, 16, "vec4f")

        data object vec2i : WgslType(2, 8, 8, "vec2i")

        data object vec3i : WgslType(3, 12, 16, "vec3i")

        data object vec4i : WgslType(4, 16, 16, "vec4i")

        data object vec2u : WgslType(2, 8, 8, "vec2u")

        data object vec3u : WgslType(3, 12, 16, "vec3u")

        data object vec4u : WgslType(4, 16, 16, "vec4u")

        data object vec2h : WgslType(2, 4, 4, "vec2h")

        data object vec3h : WgslType(3, 6, 8, "vec3h")

        data object vec4h : WgslType(4, 8, 8, "vec4h")

        data object mat2x2f : WgslType(4, 16, 8, "mat2x2f")

        data object mat2x3f : WgslType(8, 32, 16, "mat2x3f")

        data object mat2x4f : WgslType(8, 32, 16, "mat2x4f")

        data object mat3x2f : WgslType(6, 24, 8, "mat3x2f")

        data object mat3x3f : WgslType(12, 48, 16, "mat3x3f")

        data object mat3x4f : WgslType(12, 48, 16, "mat3x4f")

        data object mat4x2f : WgslType(8, 32, 8, "mat4x2f")

        data object mat4x3f : WgslType(16, 64, 16, "mat4x3f")

        data object mat4x4f : WgslType(16, 64, 16, "mat4x4f")

        data object mat2x2h : WgslType(4, 8, 4, "mat2x2h")

        data object mat2x3h : WgslType(8, 16, 8, "mat2x3h")

        data object mat2x4h : WgslType(8, 16, 8, "mat2x4h")

        data object mat3x2h : WgslType(6, 12, 4, "mat3x2h")

        data object mat3x3h : WgslType(12, 24, 8, "mat3x3h")

        data object mat3x4h : WgslType(12, 24, 8, "mat3x4h")

        data object mat4x2h : WgslType(8, 16, 4, "mat4x2h")

        data object mat4x3h : WgslType(16, 32, 8, "mat4x3h")

        data object mat4x4h : WgslType(16, 32, 8, "mat4x4h")

        sealed class atomic(elements: Int, size: Int, alignment: Int, type: String) :
            WgslType(elements, size, alignment, type) {
            data object u32 : atomic(1, 4, 4, "atomic<u32>")

            data object i32 : atomic(1, 4, 4, "atomic<i32>")
        }
    }

    data class Array(val type: ShaderStructParameterType, val length: Int) :
        ShaderStructParameterType()
}
