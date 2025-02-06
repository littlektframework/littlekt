package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 2/5/2025
 */
class DepthSliceMaterialBuilder : SubFragmentShaderBuilder() {
    override fun material(group: Int) = Unit

    override fun main(entryPoint: String) {
        tileFunctions()
        parts +=
            // language=wgsl
            """
                var<private> color_set : array<vec3f, 9> = array<vec3f, 9>(
                  vec3f(1.0, 0.0, 0.0),
                  vec3f(1.0, 0.5, 0.0),
                  vec3f(0.5, 1.0, 0.0),
                  vec3f(0.0, 1.0, 0.0),
                  vec3f(0.0, 1.0, 0.5),
                  vec3f(0.0, 0.5, 1.0),
                  vec3f(0.0, 0.0, 1.0),
                  vec3f(0.5, 0.0, 1.0),
                  vec3f(1.0, 0.0, 0.5)
                );

                @fragment
                fn fs_main(input: VertexOutput) -> @location(0) vec4f {
                  var tile : vec3u = get_tile(input.position);
                  return vec4f(color_set[tile.z % 9u], 1.0);
                }
        """
                .trimIndent()
    }
}
