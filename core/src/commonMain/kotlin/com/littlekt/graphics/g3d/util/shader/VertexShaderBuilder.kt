package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
open class VertexShaderBuilder {
    private val parts = mutableListOf<String>()

    fun camera(group: Int, binding: Int) {
        parts +=
            """
        struct Camera {
            view_proj: mat4x4<f32>,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
        """
                .trimIndent()
    }

    fun model(group: Int, binding: Int) {
        parts +=
            """
        struct Model {
            transform: mat4x4<f32>,
        };
        @group($group) @binding($binding)
        var<uniform> model: Model;
        """
    }

    fun vertexInput(attributes: List<VertexAttribute>) {
        val inputs =
            attributes.map { attribute ->
                when (attribute.usage) {
                    VertexAttrUsage.POSITION ->
                        "@location(${attribute.shaderLocation}) position: vec4f,"
                    VertexAttrUsage.COLOR -> "@location(${attribute.shaderLocation}) color: vec4f,"
                    VertexAttrUsage.NORMAL ->
                        "@location(${attribute.shaderLocation}) normal: vec3f,"
                    VertexAttrUsage.TANGENT ->
                        "@location(${attribute.shaderLocation}) tangent: vec4f,"
                    VertexAttrUsage.TEX_COORDS -> {
                        if (attribute.index == 0)
                            "@location(${attribute.shaderLocation}) uv: vec2f,"
                        else "@location(${attribute.shaderLocation}) uv2: vec2f,"
                    }

                    VertexAttrUsage.JOINT -> "@location(${attribute.shaderLocation}) joints: vec4u,"
                    VertexAttrUsage.WEIGHT ->
                        "@location(${attribute.shaderLocation}) weight: vec4f,"
                    VertexAttrUsage.BINORMAL ->
                        "@location(${attribute.shaderLocation}) binormal: vec3f,"
                    else -> {
                        error(
                            "Unknown attribute usage type: ${attribute.usage}. If using custom attributes, then you must create your own input struct!"
                        )
                    }
                }
            }

        parts +=
            """
            struct VertexInput {
                ${inputs.joinToString("\n")}
            };
        """
                .trimIndent()
    }

    fun vertexOutput(attributes: List<VertexAttribute>) {
        parts +=
            """
        struct VertexOutput {
            @builtin(position) position: vec4f,
            @location(0) world_pos: vec3f,
            @location(1) normal: vec3f,
            @location(2) uv: vec2f,
            @location(3) uv2: vec2f,
            @location(4) color: vec4f,
            @location(5) normal: vec3f,
            ${
            if (attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
                """
                    @location(6) tangent: vec3f,
                    @location(7) bitangent: vec3f,
                """.trimIndent()
            } else ""
        }
        };
    """
                .trimIndent()
    }

    fun main(layout: List<VertexAttribute>, entryPoint: String = "vs_main") {
        parts +=
            """
        @vertex
        fn $entryPoint(input: VertexInput) -> VertexOutput {
            var output: VertexOutput;
            let model_matrix = model.transform;
            ${
            if (layout.any { it.usage == VertexAttrUsage.NORMAL }) {
                """
                        output.normal = normalize((model_matrix * vec4(input.normal, 0.0)).xyz);
                    """.trimIndent()
            } else {
                """
                        output.normal = normalize((model_matrix * vec4(0.0, 0.0, 1.0, 0.0)).xyz);
                    """.trimIndent()
            }
        }
            ${
            if (layout.any { it.usage == VertexAttrUsage.TANGENT }) {
                """
                        output.tangent = normalize((model_matrix * vec4(input.tangent.xyz, 0.0)).xyz);
                        output.bitangent = cross(output.normal, output.tangent) * input.tangent.w;
                    """.trimIndent()
            } else ""
        }
            ${
            if (layout.any { it.usage == VertexAttrUsage.COLOR }) {
                """
                        output.color = input.color;
                    """.trimIndent()
            } else {
                """
                        output.color = vec4(1.0);
                    """.trimIndent()
            }
        }
            ${
            if (layout.any { it.usage == VertexAttrUsage.TEX_COORDS && it.index == 0}) {
                """
                        output.uv = input.uv;
                    """.trimIndent()
            } else ""
        }
            ${
            if (layout.any { it.usage == VertexAttrUsage.TEX_COORDS && it.index == 1 }) {
                """
                        output.uv2 = input.uv2;
                    """.trimIndent()
            } else ""
        }
            let model_pos = model_matrix * input.position;
            output.world_pos = model_pos.xyz;
            output.position = camera.view_proj * model_pos;
            return output;
        }
    """
                .trimIndent()
    }

    fun build(): String {
        return parts.joinToString("\n")
    }
}
