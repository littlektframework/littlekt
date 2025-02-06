package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.shader.builder.shaderStruct
import kotlin.test.Test

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCodeBuilderTests {

    private val inputStruct =
        shaderStruct("InputStruct") {
            """
                struct InputStruct {
                    position: vec3f,
                    uv: vec2f,
                };
            """
                .trimIndent()
        }
    private val outputStruct =
        shaderStruct("OutputStruct") {
            """
                struct OutputStruct {
                    @location(0) uv: vec2<f32>,
                    @builtin(position) position: vec4<f32>,
                };
            """
                .trimIndent()
        }

    private val defaultVertexShader = shader {
        vertex {
            main(input = inputStruct, output = outputStruct) {
                """
                    var output: OutputStruct;
                    %uv%
                    output.uv = input.uv;
                    %position%
                    output.position = vec4(input.position, 1.0);
                    %output%
                    return output;
                """
                    .trimIndent()
            }
        }
    }

    @Test
    fun buildShaderWithStructs() {
        val shader = shader {
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    """
                    var output: OutputStruct;
                    %uv%
                    output.uv = input.uv;
                    %position%
                    output.position = vec4(input.position, 1.0);
                    %output%
                    return output;
                """
                        .trimIndent()
                }
            }
        }

        println(shader.src)
    }

    @Test
    fun extendExistingShader() {
        val shader =
            shader(defaultVertexShader) {
                vertex {
                    before("output") {
                        body {
                            """
                output.position.x += 2.0;
                output.position.y += 1.0;
            """
                                .trimIndent()
                        }
                    }
                }
            }

        println(shader.src)
    }
}
