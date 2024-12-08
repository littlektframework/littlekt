package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.Camera
import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.UnlitMaterial
import com.littlekt.graphics.g3d.shader.ModelShaderUtils.Unlit

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
                    VertexAttrUsage.TEX_COORDS ->
                        "@location(${attribute.shaderLocation}) uv: vec2f,"
                    VertexAttrUsage.TEX_COORDS ->
                        "@location(${attribute.shaderLocation}) uv2: vec2f,"
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
        fn linearTosRGB(linear : vec3f) -> vec3f {
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
    fun createVertexSource(entryPoint: String = "vs_main", layout: List<VertexAttribute>) =
        """
        ${createVertexInputStruct(layout)}
        ${createVertexOutputStruct(layout)}
        ${createCameraStruct()}
        
        @vertex
        fn $entryPoint(input: VertexInput) -> VertexOutput {
            var output: VertexOutput;
            
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
            baseColorFactor : vec4<f32>,
            alphaCutoff : f32,
        };
        @group($group) @binding(0) var<uniform> material : Material;
        
        @group($group) @binding(1) var baseColorTexture : texture_2d<f32>;
        @group($group) @binding(2) var baseColorSampler : sampler;
    """
                .trimIndent()

        /**
         * Creates the default WGSL fragment shader source that is used by the [UnlitMaterial].
         *
         * @param entryPoint name of the fragment function entry point
         */
        fun createFragmentSource(entryPoint: String = "fs_Main") =
            // language=wgsl
            """
        @fragment
        fn $entryPoint(input : VertexOutput) -> @location(0) vec4<f32> {
            let baseColorMap = textureSample(baseColorTexture, baseColorSampler, input.texcoord);
            if (baseColorMap.a < material.alphaCutoff) {
              discard;
            }
            let baseColor = input.color * material.baseColorFactor * baseColorMap;
            return vec4(linearTosRGB(baseColor.rgb), baseColor.a);
        };
    """
                .trimIndent()
    }
}
