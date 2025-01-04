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
            base_color_factor : vec4f,
            alpha_cutoff : f32,
        };
        @group($group) @binding(0) var<uniform> material : Material;
        
        @group($group) @binding(1) var base_color_texture : texture_2d<f32>;
        @group($group) @binding(2) var base_color_sampler : sampler;
    """
    }

    override fun main(entryPoint: String) {
        colorConversionFunctions()
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
