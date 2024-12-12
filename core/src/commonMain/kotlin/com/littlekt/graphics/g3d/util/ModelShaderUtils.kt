package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.Camera
import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.g3d.util.ModelShaderUtils.Unlit

/**
 * Utilities for creating shaders to be used rendering a [Model].
 *
 * @see Unlit
 * @author Colton Daily
 * @date 12/8/2024
 */
object ModelShaderUtils {

    /**
     * Creates a WGSL struct for passing in a [Camera.viewProjection] matrix.
     *
     * @param group wgsl group the uniform belongs to
     * @param binding wgsl binding order the un
     */
    fun createCameraStruct(group: Int = 0, binding: Int = 0) =
        """
        struct Camera {
            view_proj: mat4x4<f32>,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
        """
            .trimIndent()

    /**
     * Creates a WGSL struct for passing in a [Node3D.globalTransform] matrix.
     *
     * @param group wgsl group the uniform belongs to
     * @param binding wgsl binding order the un
     */
    fun createModelStruct(group: Int = 1, binding: Int = 0) =
        """
        struct Model {
            transform: mat4x4<f32>,
        };
        @group($group) @binding($binding)
        var<uniform> model: Model;
        """
            .trimIndent()

    /**
     * Generates the default vertex input struct. This assumes the [VertexAttribute] being used is
     * one of the predefined [VertexAttrUsage] types. If using a custom or
     * [VertexAttrUsage.GENERIC], then you must create your own input struct functionality, as this
     * will throw an error.
     */
    fun createVertexInputStruct(attributes: List<VertexAttribute>): String {
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

        return """
            struct VertexInput {
                ${inputs.joinToString("\n")}
            };
        """
            .trimIndent()
    }

    /** Generates the default vertex out struct. */
    fun createVertexOutputStruct(attributes: List<VertexAttribute>) =
        """
        struct VertexOutput {
            @builtin(position) position: vec4f,
            @location(0) world_pos: vec3f,
            @location(1) view: vec3f, // vector from vertex to camera
            @location(2) normal: vec3f,
            @location(3) uv: vec2f,
            @location(4) uv2: vec2f,
            @location(5) color: vec4f,
            @location(6) normal: vec3f,
            ${
                if (attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
                """
                    @location(7) tangent: vec3f,
                    @location(8) bitangent: vec3f,
                """.trimIndent()
                } else ""
            }
        };
    """
            .trimIndent()

    /**
     * Creates a WGSL function for converting linear colors to SRGB.
     *
     * @param useApproximateSrgb if `true`, will generate WGSL source to use an approximate
     *   calculation.
     * @param gamma value of the gamma to be use with the approximate srgb calculation, if
     *   [useApproximateSrgb] is `true`.
     */
    fun createLinearToSRGBFunction(useApproximateSrgb: Boolean = true, gamma: Float = 2.2f) =
        """
        fn linear_to_sRGB(linear : vec3f) -> vec3f {
            ${
                if(useApproximateSrgb) {
                    """
                        let INV_GAMMA = 1.0 / $gamma;
                        return pow(linear, vec3(INV_GAMMA));
                    """.trimIndent()
                } else {
                    """
                    if (all(linear <= vec3(0.0031308))) {
                        return linear * 12.92;
                    }
                    return (pow(abs(linear), vec3(1.0/2.4)) * 1.055) - vec3(0.055);
                    """.trimIndent()
                }
            }
        }
    """
            .trimIndent()

    /**
     * Create the default vertex shader source.
     *
     * @param entryPoint name of the vertex function entry point.
     */
    fun createVertexSource(layout: List<VertexAttribute>, entryPoint: String = "vs_main") =
        """
        ${createVertexInputStruct(layout)}
        ${createVertexOutputStruct(layout)}
        ${createCameraStruct()}
        ${createModelStruct()}
        
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
            output.view = camera.position - model_pos.xyz;
            output.position = camera.view_proj * model_pos;
            return output;
        }
    """
            .trimIndent()

    /** Shader utilities for creating structs and fragment sources for an [UnlitMaterial]. */
    object Unlit {
        /** Creates the WGSL struct that is used by the [UnlitMaterial] */
        fun createMaterialStruct(group: Int) =
            // language=wgsl
            """
        struct Material {
            base_color_factor : vec4<f32>,
            alpha_cutoff : f32,
        };
        @group($group) @binding(0) var<uniform> material : Material;
        
        @group($group) @binding(1) var base_color_texture : texture_2d<f32>;
        @group($group) @binding(2) var base_color_sampler : sampler;
    """
                .trimIndent()

        /**
         * Creates the default WGSL fragment shader source that is used by the [UnlitMaterial].
         *
         * @param entryPoint name of the fragment function entry point
         */
        fun createFragmentSource(layout: List<VertexAttribute>, entryPoint: String = "fs_Main") =
            // language=wgsl
            """
            ${createVertexOutputStruct(layout)}
            ${createMaterialStruct(0)}
            @fragment
            fn $entryPoint(input : VertexOutput) -> @location(0) vec4<f32> {
                let base_color_map = textureSample(base_color_texture, base_color_sampler, input.uv);
                if (base_color_map.a < material.alpha_cutoff) {
                  discard;
                }
                let base_color = input.color * material.base_color_factor * base_color_map;
                return vec4(linear_to_sRGB(base_color.rgb), base_color.a);
            };
            """
                .trimIndent()
    }
}
