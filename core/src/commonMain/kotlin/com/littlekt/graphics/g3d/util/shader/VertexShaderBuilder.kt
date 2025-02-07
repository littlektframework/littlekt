package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
open class VertexShaderBuilder : SubShaderBuilder() {

    fun main(
        layout: List<VertexAttribute>,
        cameraViewProjCombined: Boolean = true,
        skinned: Boolean = false,
        skinGroup: Int = 2,
        entryPoint: String = "vs_main",
    ) {
        if (skinned) {
            skin(skinGroup)
            getSkinMatrix()
        }
        val cameraViewProj =
            if (cameraViewProjCombined) "camera.view_proj" else "camera.proj * camera.view"
        parts +=
            // language=wgsl
            """
        @vertex
        fn $entryPoint(input: VertexInput) -> VertexOutput {
            var output: VertexOutput;
            let model_matrix = ${if (skinned) "get_skin_matrix(input);" else "models[input.instance_index].transform;"}
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
                if (layout.any { it.usage == VertexAttrUsage.UV && it.index == 0}) {
                    """
                            output.uv = input.uv;
                        """.trimIndent()
                } else ""
            }
            ${
                if (layout.any { it.usage == VertexAttrUsage.UV && it.index == 1 }) {
                    """
                            output.uv2 = input.uv2;
                        """.trimIndent()
                } else ""
            }
            let model_pos = model_matrix * vec4(input.position, 1.0);
            output.world_pos = model_pos.xyz;
            output.instance_color = models[input.instance_index].color;
            ${ if(!cameraViewProjCombined) "output.view = camera.position - model_pos.xyz;" else ""}
            output.position = $cameraViewProj * model_pos;
            return output;
        }
    """
                .trimIndent()
    }
}
