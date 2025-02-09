package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.ShaderBindingType
import com.littlekt.graphics.shader.builder.ShaderStructParameterType
import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.shader.builder.shaderStruct
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.MemoryAccessMode
import com.littlekt.graphics.webgpu.ShaderStage
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

        val shader = shader {
            include(inputStruct)
            include(outputStruct)
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    """
                        var output: VertexOutput;
                        output.position = vec4(input.position, 1.0);
                        output.color = input.color;
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                return output;
            }
        """
                    .trimIndent()
            )
        assertEquals(expected.src, shader.src)
        assertEquals(shader.visibility, ShaderStage.VERTEX)
    }

    @Test
    fun testExtendShader() {
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

        val vertexShader = shader {
            include(inputStruct)
            include(outputStruct)
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    """
                        var output: VertexOutput;
                        output.position = vec4(input.position, 1.0);
                        output.color = input.color;
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val extraStruct =
            shaderStruct("Info") { mapOf("index" to ShaderStructParameterType.WgslType.f32) }
        val extendedShader = shader(vertexShader) { include(extraStruct) }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };

            struct Info {
                index: f32
            };

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, extendedShader.src)
        assertEquals(extendedShader.visibility, ShaderStage.VERTEX)
    }

    @Test
    fun testExtendFromVertexAndFragmentShaders() {
        val vertexInputStruct =
            shaderStruct("VertexInput") {
                mapOf(
                    "position" to ShaderStructParameterType.WgslType.vec3f,
                    "color" to ShaderStructParameterType.WgslType.vec4f,
                )
            }
        val vertexOutputStruct =
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

        val vertexShader = shader {
            include(vertexInputStruct)
            include(vertexOutputStruct)
            vertex {
                main(input = vertexInputStruct, output = vertexOutputStruct) {
                    """
                        var output: VertexOutput;
                        output.position = vec4(input.position, 1.0);
                        output.color = input.color;
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val fragmentOutputStruct =
            shaderStruct("FragmentOutput") {
                mapOf("color" to ShaderStructParameterType.WgslType.vec4f)
            }

        val fragmentShader = shader {
            include(vertexOutputStruct)
            include(fragmentOutputStruct)
            fragment {
                main(input = vertexOutputStruct, output = fragmentOutputStruct) {
                    """
                        var output: FragmentOutput;
                        output.color = input.color;
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val extraStruct =
            shaderStruct("Info") { mapOf("index" to ShaderStructParameterType.WgslType.f32) }
        val extendedShader = shader {
            include(extraStruct)
            vertex(vertexShader.vertex)
            fragment(fragmentShader.fragment)
        }

        val expected =
            ShaderTestSrc(
                """
            struct Info {
                index: f32
            };
            
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };
            
            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };
            
            struct FragmentOutput {
                color: vec4f
            };
            
            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                return output;
            }
            @fragment fn main(input: VertexOutput) -> FragmentOutput {
                var output: FragmentOutput;
                output.color = input.color;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, extendedShader.src)
        assertEquals(extendedShader.visibility, ShaderStage.VERTEX or ShaderStage.FRAGMENT)
    }

    @Test
    fun testRulesBefore() {
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

        val vertexShader = shader {
            include(inputStruct)
            include(outputStruct)
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    before("output") {
                        body {
                            """
                                output.color.r *= 0.5;
                            """
                                .trimIndent()
                        }
                    }
                    """
                        var output: VertexOutput;
                        %position%
                        output.position = vec4(input.position, 1.0);
                        %color%
                        output.color = input.color;
                        %output%
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                output.color.r *= 0.5;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, vertexShader.src)
    }

    @Test
    fun testRulesAfter() {
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

        val vertexShader = shader {
            include(inputStruct)
            include(outputStruct)
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    after("color") {
                        body {
                            """
                                output.color.r *= 0.5;
                            """
                                .trimIndent()
                        }
                    }
                    """
                        var output: VertexOutput;
                        %position%
                        output.position = vec4(input.position, 1.0);
                        %color%
                        output.color = input.color;
                        %output%
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                output.color.r *= 0.5;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, vertexShader.src)
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
                        ShaderStructParameterType.array(ShaderStructParameterType.WgslType.vec3f, 4),
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
                        ShaderStructParameterType.array(
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

    @Test
    fun testSingleBindGroup() {
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

        val viewStruct =
            shaderStruct("View") {
                mapOf("view_proj" to ShaderStructParameterType.WgslType.mat4x4f)
            }

        val vertexShader = shader {
            include(inputStruct)
            include(outputStruct)
            include(viewStruct)
            bindGroup(0, BindingUsage("View")) {
                bind(0, "view", viewStruct, ShaderBindingType.Uniform)
            }
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    """
                        var output: VertexOutput;
                        %position%
                        output.position = vec4(input.position, 1.0);
                        %color%
                        output.color = input.color;
                        %output%
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };
            
            struct View {
                view_proj: mat4x4f
            };

            @group(0) @binding(0) var<uniform> view: View;

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, vertexShader.src)
        assertEquals(vertexShader.visibility, ShaderStage.VERTEX)
    }

    @Test
    fun testMultipleBindGroups() {
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

        val viewStruct =
            shaderStruct("View") {
                mapOf("view_proj" to ShaderStructParameterType.WgslType.mat4x4f)
            }

        val dataStruct =
            shaderStruct("Data") {
                mapOf("transform" to ShaderStructParameterType.WgslType.mat4x4f)
            }
        val viewBindingUsage = BindingUsage("View")
        val dataBindingUsage = BindingUsage("Data")
        val vertexShader = shader {
            include(inputStruct)
            include(outputStruct)
            include(viewStruct)
            bindGroup(0, viewBindingUsage) {
                bind(0, "view", viewStruct, ShaderBindingType.Uniform)
            }
            bindGroup(1, dataBindingUsage) {
                bind(0, "data", dataStruct, ShaderBindingType.Storage(MemoryAccessMode.READ))
            }
            vertex {
                main(input = inputStruct, output = outputStruct) {
                    """
                        var output: VertexOutput;
                        %position%
                        output.position = vec4(input.position, 1.0);
                        %color%
                        output.color = input.color;
                        %output%
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
            struct VertexInput {
                position: vec3f,
                color: vec4f
            };

            struct VertexOutput {
                @builtin(position) position: vec4f,
                @location(0) color: vec4f
            };
            
            struct View {
                view_proj: mat4x4f
            };

            @group(0) @binding(0) var<uniform> view: View;

            @group(1) @binding(0) var<storage, read> data: Data;

            @vertex fn main(input: VertexInput) -> VertexOutput {
                var output: VertexOutput;
                output.position = vec4(input.position, 1.0);
                output.color = input.color;
                return output;
            }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, vertexShader.src)
        assertEquals(vertexShader.visibility, ShaderStage.VERTEX)
        assertEquals(vertexShader.bindGroupUsageToGroupIndex[viewBindingUsage], 0)
        assertEquals(vertexShader.bindGroupUsageToGroupIndex[dataBindingUsage], 1)
        assertEquals(vertexShader.layout[viewBindingUsage]?.entries?.size, 1)
        assertEquals(vertexShader.layout[dataBindingUsage]?.entries?.size, 1)
    }

    @Test
    fun testTexture2dAndSamplerBindings() {
        val vertexOutputStruct =
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
                    "uv" to
                        ShaderStructParameterType.Location(
                            1,
                            ShaderStructParameterType.WgslType.vec2f,
                        ),
                )
            }

        val fragmentOutputStruct =
            shaderStruct("FragmentOutput") {
                mapOf("color" to ShaderStructParameterType.WgslType.vec4f)
            }

        val materialBindingUsage = BindingUsage("Material")
        val fragmentShader = shader {
            include(vertexOutputStruct)
            include(fragmentOutputStruct)
            bindGroup(0, materialBindingUsage) {
                bindTexture2d(0, "my_texture")
                bindSampler(1, "my_sampler")
            }
            fragment {
                main(input = vertexOutputStruct, output = fragmentOutputStruct) {
                    """
                        var output: FragmentOutput;
                        %color%
                        output.color = textureSample(my_texture, my_sampler, input.uv) * input.color
                        %output%
                        return output;
                    """
                        .trimIndent()
                }
            }
        }

        val expected =
            ShaderTestSrc(
                """
                struct VertexOutput {
                    @builtin(position) position: vec4f,
                    @location(0) color: vec4f,
                    @location(1) uv: vec2f
                };
                
                struct FragmentOutput {
                    color: vec4f
                };
                
                @group(0) @binding(0) var my_texture: texture_2d<f32>;
                @group(0) @binding(1) var my_sampler: sampler;
                
                @fragment fn main(input: VertexOutput) -> FragmentOutput {
                    var output: FragmentOutput;
                    output.color = textureSample(my_texture, my_sampler, input.uv) * input.color
                    return output;
                }
        """
                    .trimIndent()
            )

        assertEquals(expected.src, fragmentShader.src)
        assertEquals(fragmentShader.visibility, ShaderStage.FRAGMENT)
        assertEquals(fragmentShader.bindGroupUsageToGroupIndex[materialBindingUsage], 0)
        assertEquals(fragmentShader.layout[materialBindingUsage]?.entries?.size, 2)
    }
}
