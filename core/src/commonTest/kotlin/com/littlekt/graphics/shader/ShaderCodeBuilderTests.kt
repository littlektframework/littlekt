package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.shader.builder.shaderStruct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCodeBuilderTests {

    private val inputVertexStruct =
        shaderStruct("InputStruct") {
            """
                struct InputStruct {
                    position: vec3f,
                    uv: vec2f,
                };
            """
                .trimIndent()
        }
    private val vertexOutputStruct =
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
            main(input = inputVertexStruct, output = vertexOutputStruct) {
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

    private val fragmentOutputStruct =
        shaderStruct("FragmentOutput") {
            """
            struct FragmentOutput {
                @location(0) color: vec4f,
            }
        """
                .trimIndent()
        }

    private val defaultFragmentShader = shader {
        fragment {
            main(input = vertexOutputStruct, output = fragmentOutputStruct) {
                """
                var output: FragmentOutput;
                %color%
                output.color = vec4f(1.0, 1.0, 1.0, 1.0);
                %output%
                return output;
            """
                    .trimIndent()
            }
        }
    }

    private val dataStruct =
        shaderStruct("View") {
            """
            @group(0) @binding(0) var<storage, read_write> data: array<f32>;
        """
                .trimIndent()
        }

    private val defaultComputeShader = shader {
        include(dataStruct)
        compute {
            main(1) {
                """
            let i = id.x
            %update_data%
            data[i] = data[i] * 2.0;
            %output%
        """
                    .trimIndent()
            }
        }
    }

    private val expectedDefaultVertexShaderSrc =
        """
        @vertex fn main(input: InputStruct) -> OutputStruct {
        var output: OutputStruct;
        output.uv = input.uv;
        output.position = vec4(input.position, 1.0);
        return output;
        }
    """
            .trimIndent()

    private val expectedDefaultFragmentShaderSrc =
        """
        @fragment fn main(input: OutputStruct) -> FragmentOutput {
        var output: FragmentOutput;
        output.color = vec4f(1.0, 1.0, 1.0, 1.0);
        return output;
        }
    """
            .trimIndent()

    private val expectedComputeShaderSrc =
        """
        @group(0) @binding(0) var<storage, read_write> data: array<f32>;

        @compute @workgroup_size(1, 1, 1) fn main(@builtin(global_invocation_id) global_id : vec3u) {
        let i = id.x
        data[i] = data[i] * 2.0;
        }
    """
            .trimIndent()

    @Test
    fun testVertexShaderOnlyHasVertexEntryPoint() {
        assertEquals("main", defaultVertexShader.vertexEntryPoint)
        assertNull(defaultVertexShader.fragmentEntryPoint)
        assertNull(defaultVertexShader.computeEntryPoint)
    }

    @Test
    fun testFragmentShaderOnlyHasFragmentEntryPoint() {
        assertEquals("main", defaultFragmentShader.fragmentEntryPoint)
        assertNull(defaultFragmentShader.vertexEntryPoint)
        assertNull(defaultFragmentShader.computeEntryPoint)
    }

    @Test
    fun testComputeShaderOnlyHasComputeEntryPoint() {
        assertEquals("main", defaultComputeShader.computeEntryPoint)
        assertNull(defaultComputeShader.fragmentEntryPoint)
        assertNull(defaultComputeShader.vertexEntryPoint)
    }

    @Test
    fun buildVertexShaderWithStructs() {
        val shader = shader {
            vertex {
                main(input = inputVertexStruct, output = vertexOutputStruct) {
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

        assertEquals(expectedDefaultVertexShaderSrc, shader.src)
    }

    @Test
    fun extendExistingVertexShader() {
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

        val expected =
            """
            @vertex fn main(input: InputStruct) -> OutputStruct {
            var output: OutputStruct;
            output.uv = input.uv;
            output.position = vec4(input.position, 1.0);
            output.position.x += 2.0;
            output.position.y += 1.0;
            
            return output;
            }
        """
                .trimIndent()
        assertEquals(expected, shader.src)
    }

    @Test
    fun extendExistingVertexShaderButDoNothing() {
        val shader = shader(defaultVertexShader) { vertex {} }
        assertEquals(expectedDefaultVertexShaderSrc, shader.src)
    }

    @Test
    fun buildFragmentShaderWithStructs() {
        val shader = shader {
            fragment {
                main(input = vertexOutputStruct, output = fragmentOutputStruct) {
                    """
                var output: FragmentOutput;
                %color%
                output.color = vec4f(1.0, 1.0, 1.0, 1.0);
                %output%
                return output;
                """
                        .trimIndent()
                }
            }
        }

        assertEquals(expectedDefaultFragmentShaderSrc, shader.src)
    }

    @Test
    fun extendExistingFragmentShader() {
        val shader =
            shader(defaultFragmentShader) {
                fragment {
                    before("output") {
                        body {
                            """
                                output.color.r *= 0.5f;
                                output.color.g *= 0.25f;
                            """
                                .trimIndent()
                        }
                    }
                }
            }

        val expected =
            """
            @fragment fn main(input: OutputStruct) -> FragmentOutput {
            var output: FragmentOutput;
            output.color = vec4f(1.0, 1.0, 1.0, 1.0);
            output.color.r *= 0.5f;
            output.color.g *= 0.25f;
            
            return output;
            }
        """
                .trimIndent()
        assertEquals(expected, shader.src)
    }

    @Test
    fun extendExistingFragmentShaderButDoNothing() {
        val shader = shader(defaultFragmentShader) { fragment {} }
        assertEquals(expectedDefaultFragmentShaderSrc, shader.src)
    }

    @Test
    fun buildComputeShader() {
        val shader = shader {
            include(dataStruct)
            compute {
                main(1) {
                    """
            let i = id.x
            %update_data%
            data[i] = data[i] * 2.0;
            %output%
        """
                        .trimIndent()
                }
            }
        }

        assertEquals(expectedComputeShaderSrc, shader.src)
    }

    @Test
    fun extendExistingComputeShader() {
        val shader =
            shader(defaultComputeShader) {
                compute {
                    before("output") {
                        body {
                            """
                                data[i] = data[i] - 1.0;
                            """
                                .trimIndent()
                        }
                    }
                }
            }

        val expected =
            """
            @group(0) @binding(0) var<storage, read_write> data: array<f32>;

            @compute @workgroup_size(1, 1, 1) fn main(@builtin(global_invocation_id) global_id : vec3u) {
            let i = id.x
            data[i] = data[i] * 2.0;
            data[i] = data[i] - 1.0;
            
            }
        """
                .trimIndent()
        assertEquals(expected, shader.src)
    }

    @Test
    fun extendExistingComputeShaderButDoNothing() {
        val shader = shader(defaultComputeShader) { compute {} }
        assertEquals(expectedComputeShaderSrc, shader.src)
    }
}
