package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
class UnlitMaterialBuilder : SubFragmentShaderBuilder() {
    override fun material(group: Int) {
        parts +=
            """
        struct Material {
            base_color_factor : vec4<f32>,
            alpha_cutoff : f32,
        };
        @group($group) @binding(0) var<uniform> material : Material;
        
        @group($group) @binding(1) var base_color_texture : texture_2d<f32>;
        @group($group) @binding(2) var base_color_sampler : sampler;
    """
    }

    private fun createLinearToSRGBFunction(
        useApproximateSrgb: Boolean = true,
        gamma: Float = 2.2f,
    ) {
        parts +=
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
    }

    override fun main(entryPoint: String) {
        createLinearToSRGBFunction()
        parts +=
            """
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
