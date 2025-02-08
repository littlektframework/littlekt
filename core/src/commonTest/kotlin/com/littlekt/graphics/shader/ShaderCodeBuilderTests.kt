package com.littlekt.graphics.shader

import com.littlekt.graphics.shader.builder.*
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCodeBuilderTests {

    private val bindGroupBindingUsage = BindingUsage("Bind Group")

    private fun bindGroup1(group: Int, binding: Int) =
        shaderBindGroup(
            group,
            bindGroupBindingUsage,
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        binding,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    )
                )
            ),
        ) {
            """
                struct BindGroup1 {
                    pos: vec4f,
                }
                
                @group($group) @binding($binding)
                var <storage, read> bg1: BindGroup1;
            """
                .trimIndent()
        }

    private val bindGroup2BindingUsage = BindingUsage("Bind Group 2")

    private fun bindGroup2(group: Int, binding: Int) =
        shaderBindGroup(
            group,
            bindGroup2BindingUsage,
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        binding,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE),
                    ),
                    BindGroupLayoutEntry(
                        binding + 1,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.UNIFORM),
                    ),
                    BindGroupLayoutEntry(
                        binding + 2,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(BufferBindingType.STORAGE),
                    ),
                )
            ),
        ) {
            """
                struct BindGroup2 {
                    pos: vec4f,
                }
                
                struct BindGroup3 {
                    pos: vec4f,
                }
                
                @group($group) @binding($binding)
                var <storage, read> bg1: BindGroup1;
                @group($group) @binding(${binding + 1})
                var <uniform> bg2: BindGroup2;
                @group($group) @binding(${binding + 2})
                var <storage, read_write> bg3: BindGroup3;
            """
                .trimIndent()
        }

    private val inputVertexStruct =
        shaderStructOld("InputStruct") {
            """
                struct InputStruct {
                    position: vec3f,
                    uv: vec2f,
                };
            """
                .trimIndent()
        }
    private val vertexOutputStruct =
        shaderStructOld("OutputStruct") {
            """
                struct OutputStruct {
                    @location(0) uv: vec2<f32>,
                    @builtin(position) position: vec4<f32>,
                };
            """
                .trimIndent()
        }

    private val commonStructOne =
        shaderStructOld("CommonStructOne") {
            """
            struct CommonStructOne {
                i: u32,
            }
        """
                .trimIndent()
        }

    private val commonStructTwo =
        shaderStructOld("CommonStructTwo") {
            """
            struct CommonStructTwo {
                other: vec3f,
            }
        """
                .trimIndent()
        }

    private val commonStructs = shaderBlock {
        include(commonStructOne)
        include(commonStructTwo)
    }

    private val common = shaderBlock {
        include(commonStructs)
        body {
            """
                fn test(): CommonStructTwo {
                    var commonStruct: CommonStructTwo;
                    commonStruct.i = 3u;
                    return commonStruct;
                }
                
                fun another() {
                   // do work
                   var commonStruct: CommonStructOne;
                   commonStruct.other = vec3(1.0, 0.0, 0.0);
                }
            """
                .trimIndent()
        }
    }

    private val expectedCommonSrc = common.src

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
        shaderStructOld("FragmentOutput") {
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
        shaderStructOld("View") {
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
            .format()

    private val expectedDefaultFragmentShaderSrc =
        """
        @fragment fn main(input: OutputStruct) -> FragmentOutput {
        var output: FragmentOutput;
        output.color = vec4f(1.0, 1.0, 1.0, 1.0);
        return output;
        }
    """
            .trimIndent()
            .format()

    private val expectedComputeShaderSrc =
        """
        @group(0) @binding(0) var<storage, read_write> data: array<f32>;

        @compute @workgroup_size(1, 1, 1) fn main(@builtin(global_invocation_id) global_id : vec3u) {
        let i = id.x
        data[i] = data[i] * 2.0;
        }
    """
            .trimIndent()
            .format()

    private fun String.format(): String {
        val indentSize = 4
        var indentLevel = 0
        val formattedCode = StringBuilder()
        var lastLineWasBlank = false
        val code = this

        code.lines().forEach { line ->
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty()) {
                if (!lastLineWasBlank) {
                    formattedCode.appendLine()
                    lastLineWasBlank = true
                }
                return@forEach
            }

            lastLineWasBlank = false

            if (trimmedLine.startsWith("}")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            formattedCode.append(" ".repeat(indentLevel * indentSize))
            formattedCode.appendLine(trimmedLine)

            if (trimmedLine.endsWith("{")) {
                indentLevel++
            }
        }

        return formattedCode.toString()
    }

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
                .format()
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
                .format()
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
                .format()
        assertEquals(expected, shader.src)
    }

    @Test
    fun extendExistingComputeShaderButDoNothing() {
        val shader = shader(defaultComputeShader) { compute {} }
        assertEquals(expectedComputeShaderSrc, shader.src)
    }

    @Test
    fun extendShaderAndAddBeforeTopLevel() {
        val shader = shader(defaultVertexShader) { include(common) }
        assertEquals("$expectedCommonSrc\n${defaultVertexShader.src}".format(), "$shader\n")
    }

    @Test
    fun includeCustomBlockAtShaderBuild() {
        val shader = shader {
            include {
                body {
                    """
                        struct Test {
                           i: u32,
                        };
                    """
                        .trimIndent()
                }
            }

            vertex {
                main(input = inputVertexStruct, output = vertexOutputStruct) {
                    """
                            var output: OutputStruct;
                            var test: Test;
                            test.i = input.position.x;
                            
                            output.position = vec4f(input.position, test.i);
                            return output;
                        """
                        .trimIndent()
                }
            }
        }

        val expected =
            """
            struct Test {
               i: u32,
            };

            @vertex fn main(input: InputStruct) -> OutputStruct {
            var output: OutputStruct;
            var test: Test;
            test.i = input.position.x;

            output.position = vec4f(input.position, test.i);
            return output;
            }
        """
                .trimIndent()
                .format()
        assertEquals(expected, shader.src)
    }

    @Test
    fun bindGroups() {
        val shader =
            shader(defaultVertexShader) {
                include(inputVertexStruct)
                include(vertexOutputStruct)
                include(bindGroup1(5, 0))
                include(bindGroup2(0, 0))
            }

        assertEquals(0, shader.bindGroupUsageToGroupIndex[bindGroup2BindingUsage])
        assertEquals(5, shader.bindGroupUsageToGroupIndex[bindGroupBindingUsage])
        assertEquals(bindGroup2BindingUsage, shader.bindGroupLayoutUsageLayout[0])
        assertEquals(bindGroupBindingUsage, shader.bindGroupLayoutUsageLayout[1])
    }

    @Test
    fun bindGroupsCombine() {
        val shader =
            shader(defaultVertexShader) {
                include(inputVertexStruct)
                include(vertexOutputStruct)
                include(bindGroup1(0, 0))
                include(bindGroup2(0, 1))
            }

        val combinedUsage = bindGroupBindingUsage or bindGroup2BindingUsage
        assertEquals(0, shader.bindGroupUsageToGroupIndex[combinedUsage])
        assertEquals(combinedUsage, shader.bindGroupLayoutUsageLayout[0])
        assertEquals(4, shader.layout[combinedUsage]?.entries?.size)
    }

    @Test
    fun shaderStructsAndOffsets() {
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
