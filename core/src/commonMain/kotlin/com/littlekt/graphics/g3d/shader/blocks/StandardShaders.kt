package com.littlekt.graphics.g3d.shader.blocks

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.shader.builder.*
import com.littlekt.graphics.util.BindingUsage
import kotlin.math.PI

object Standard {
    fun VertexInputStruct(attributes: List<VertexAttribute>): ShaderStruct {
        val inputs: MutableMap<String, ShaderStructParameterType> =
            attributes
                .associate { attribute ->
                    when (attribute.usage) {
                        VertexAttrUsage.POSITION ->
                            "position" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec3f,
                                )

                        VertexAttrUsage.COLOR ->
                            "color" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec4f,
                                )

                        VertexAttrUsage.NORMAL ->
                            "normal" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec3f,
                                )

                        VertexAttrUsage.TANGENT ->
                            "tangent" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec4f,
                                )

                        VertexAttrUsage.UV -> {
                            if (attribute.index == 0)
                                "uv" to
                                    ShaderStructParameterType.Location(
                                        attribute.shaderLocation,
                                        ShaderStructParameterType.WgslType.vec2f,
                                    )
                            else
                                "uv2" to
                                    ShaderStructParameterType.Location(
                                        attribute.shaderLocation,
                                        ShaderStructParameterType.WgslType.vec2f,
                                    )
                        }

                        VertexAttrUsage.JOINT ->
                            "joints" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec4i,
                                )

                        VertexAttrUsage.WEIGHT ->
                            "weights" to
                                ShaderStructParameterType.Location(
                                    attribute.shaderLocation,
                                    ShaderStructParameterType.WgslType.vec4f,
                                )

                        else -> {
                            error(
                                "Unknown attribute usage type: ${attribute.usage}. If using custom attributes, then you must create your own input struct!"
                            )
                        }
                    }
                }
                .toMutableMap()

        inputs["instance_index"] =
            ShaderStructParameterType.BuiltIn.InstanceIndex(ShaderStructParameterType.WgslType.u32)

        return shaderStruct("VertexInput") { inputs }
    }

    fun VertexOutputStruct(attributes: List<VertexAttribute>): ShaderStruct {
        val input =
            mutableMapOf(
                "position" to
                    ShaderStructParameterType.BuiltIn.Position(
                        ShaderStructParameterType.WgslType.vec4f
                    ),
                "world_pos" to
                    ShaderStructParameterType.Location(0, ShaderStructParameterType.WgslType.vec3f),
                "view" to
                    ShaderStructParameterType.Location(1, ShaderStructParameterType.WgslType.vec3f),
                "uv" to
                    ShaderStructParameterType.Location(2, ShaderStructParameterType.WgslType.vec2f),
                "uv2" to
                    ShaderStructParameterType.Location(3, ShaderStructParameterType.WgslType.vec2f),
                "color" to
                    ShaderStructParameterType.Location(4, ShaderStructParameterType.WgslType.vec4f),
                "instance_color" to
                    ShaderStructParameterType.Location(5, ShaderStructParameterType.WgslType.vec4f),
                "normal" to
                    ShaderStructParameterType.Location(6, ShaderStructParameterType.WgslType.vec3f),
            )

        if (attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
            input["tangent"] =
                ShaderStructParameterType.Location(7, ShaderStructParameterType.WgslType.vec3f)
            input["bitangent"] =
                ShaderStructParameterType.Location(8, ShaderStructParameterType.WgslType.vec3f)
        }

        return shaderStruct("VertexOutput") { input }
    }

    private fun vertexLayoutBlock(layout: List<VertexAttribute>) = shaderBlock {
        body {
            """
        %normal%
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
        %tangent%
        ${
                if (layout.any { it.usage == VertexAttrUsage.TANGENT }) {
                    """
                                output.tangent = normalize((model_matrix * vec4(input.tangent.xyz, 0.0)).xyz);
                                output.bitangent = cross(output.normal, output.tangent) * input.tangent.w;
                            """.trimIndent()
                } else ""
            }
        %color%
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
        %uv%
        ${
                if (layout.any { it.usage == VertexAttrUsage.UV && it.index == 0 }) {
                    """
                output.uv = input.uv;
                """.trimIndent()
                } else ""
            }
        %uv2%
        ${
                if (layout.any { it.usage == VertexAttrUsage.UV && it.index == 1 }) {
                    """
                output.uv2 = input.uv2;
                """.trimIndent()
                } else ""
            }
        %model_position%
        let model_pos = model_matrix * vec4(input.position, 1.0);
        %world_position%
        output.world_pos = model_pos.xyz;
        %instance_color%
        output.instance_color = models[input.instance_index].color;
        %view%
        output.view = camera.position - model_pos.xyz;
        %position%
        output.position = camera.proj * camera.view * model_pos;
        """
                .trimIndent()
        }
    }

    fun VertexShader(layout: List<VertexAttribute>) = shader {
        val input = VertexInputStruct(layout)
        val output = VertexOutputStruct(layout)
        include(input)
        include(output)
        include(CommonShaderBlocks.Camera(0, 0))
        include(CommonShaderBlocks.Model(1, 0))

        //        vertex {
        //            main(input = input, output = output) {
        //                before("vertex_layout_block", vertexLayoutBlock(layout))
        //                """
        //                var output: VertexOutput;
        //                %model%
        //                let model_matrix = models[input.instance_index].transform;
        //                %vertex_layout_block%
        //                %output%
        //                return output;
        //                """
        //                    .trimIndent()
        //            }
        //        }
    }

    fun SkinnedVertexShader(layout: List<VertexAttribute>) = shader {
        val input = VertexInputStruct(layout)
        val output = VertexOutputStruct(layout)
        include(input)
        include(output)
        include(CommonShaderBlocks.Camera(0, 0))
        include(CommonShaderBlocks.Model(1, 0))
        include(CommonShaderBlocks.Skin(2, 0))

        //        vertex {
        //            main(input = input, output = output) {
        //                before("vertex_layout_block", vertexLayoutBlock(layout))
        //                """
        //                var output: VertexOutput;
        //                %model%
        //                let model_matrix = get_skin_matrix(input);
        //                %vertex_layout_block%
        //                %output%
        //                return output;
        //                """
        //                    .trimIndent()
        //            }
        //        }
    }

    object Unlit {
        val MaterialStruct =
            shaderStruct("Material") {
                mapOf(
                    "base_color_factor" to ShaderStructParameterType.WgslType.vec4f,
                    "alpha_cutoff" to ShaderStructParameterType.WgslType.f32,
                )
            }

        fun Material(group: Int) = shaderBlock {
            include(MaterialStruct)
            bindGroup(group, BindingUsage.MATERIAL) {
                bind(0, "material", MaterialStruct, ShaderBindingType.Uniform)
                bindTexture2d(1, "base_color_texture")
                bindSampler(2, "base_color_sampler")
            }
        }

        val FragmentOutputStruct =
            shaderStruct("FragmentOutput") {
                mapOf(
                    "color" to
                        ShaderStructParameterType.Location(
                            0,
                            ShaderStructParameterType.WgslType.vec4f,
                        )
                )
            }

        fun FragmentShader(layout: List<VertexAttribute>) = shader {
            val input = VertexInputStruct(layout)
            val output = FragmentOutputStruct
            include(input)
            include(output)
            include(CommonShaderBlocks.ColorConversionFunctions())
            fragment {
                //                main(input, output) {
                //                    """
                //                    var output: FragmentOutput;
                //                    %base_color%
                //                    let base_color_map = textureSample(base_color_texture,
                // base_color_sampler, input.uv);
                //                    %alpha_cutoff%
                //                    if (base_color_map.a < material.alpha_cutoff) {
                //                      discard;
                //                    }
                //                    let base_color = input.color * material.base_color_factor *
                // base_color_map * input.instance_color;
                //                    %color%
                //                    output.color = vec4(linear_to_sRGB(base_color.rgb),
                // base_color.a);
                //                    %output%
                //                    return output;
                //                """
                //                        .trimIndent()
                //                }
            }
        }
    }

    object PBR {

        val MaterialStruct =
            shaderStruct("Material") {
                mapOf(
                    "base_color_factor" to ShaderStructParameterType.WgslType.vec4f,
                    "metallic_roughness_factor" to ShaderStructParameterType.WgslType.vec2f,
                    "occlusion_strength" to ShaderStructParameterType.WgslType.f32,
                    "emissive_factor" to ShaderStructParameterType.WgslType.vec3f,
                    "alpha_cutoff" to ShaderStructParameterType.WgslType.f32,
                )
            }

        fun Material(group: Int) = shaderBlock {
            include(MaterialStruct)
            bindGroup(group, BindingUsage.MATERIAL) {
                bind(0, "material", MaterialStruct, ShaderBindingType.Uniform)
                bindTexture2d(1, "base_color_texture")
                bindSampler(2, "base_color_sampler")
                bindTexture2d(3, "normal_texture")
                bindSampler(4, "normal_sampler")
                bindTexture2d(5, "metallic_roughness_texture")
                bindSampler(6, "metallic_roughness_sampler")
                bindTexture2d(7, "occlusion_texture")
                bindSampler(8, "occlusion_sampler")
                bindTexture2d(9, "emissive_texture")
                bindSampler(10, "emissive_sampler")
            }
        }

        val SurfaceInfoStruct =
            shaderStruct("SurfaceInfo") {
                mapOf(
                    "base_color" to ShaderStructParameterType.WgslType.vec4f,
                    "albedo" to ShaderStructParameterType.WgslType.vec3f,
                    "metallic" to ShaderStructParameterType.WgslType.f32,
                    "roughness" to ShaderStructParameterType.WgslType.f32,
                    "normal" to ShaderStructParameterType.WgslType.vec3f,
                    "f0" to ShaderStructParameterType.WgslType.vec3f,
                    "ao" to ShaderStructParameterType.WgslType.f32,
                    "emissive" to ShaderStructParameterType.WgslType.vec3f,
                    "v" to ShaderStructParameterType.WgslType.vec3f,
                )
            }

        fun SurfaceInfo(attributes: List<VertexAttribute>) = shaderBlock {
            include(SurfaceInfoStruct)
            body {
                """
          fn GetSurfaceInfo(input : VertexOutput) -> SurfaceInfo {
            var surface : SurfaceInfo;
            surface.v = normalize(input.view);
        
            ${
                if (attributes.any { it.usage == VertexAttrUsage.TANGENT }) {
                    """
                let N = textureSample(normal_texture, normal_sampler, input.uv).rgb;
                let tbn = mat3x3(input.tangent, input.bitangent, input.normal);
                surface.normal = normalize(tbn * (2.0 * N - 1.0));
                """
                } else {
                    "surface.normal = normalize(input.normal);"
                }
            }
        
            // Need to do all the texture samples before any conditional discard statements.
            let base_color_map = textureSample(base_color_texture, base_color_sampler, input.uv);
            let metallic_roughness_map = textureSample(metallic_roughness_texture, metallic_roughness_sampler, input.uv);
            let occlusion_map = textureSample(occlusion_texture, occlusion_sampler, input.uv);
            let emissive_map = textureSample(emissive_texture, emissive_sampler, input.uv);
        
            surface.base_color = input.color * material.base_color_factor * base_color_map;
        
            if (surface.base_color.a < material.alpha_cutoff) {
              discard;
            }
        
            surface.albedo = surface.base_color.rgb * input.instance_color.rgb;
        
            surface.metallic = material.metallic_roughness_factor.x * metallic_roughness_map.b;
            surface.roughness = material.metallic_roughness_factor.y * metallic_roughness_map.g;
        
            let dielectric_spec = vec3(0.04);
            surface.f0 = mix(dielectric_spec, surface.albedo, vec3(surface.metallic));
        
            surface.ao = material.occlusion_strength * occlusion_map.r;
            surface.emissive = material.emissive_factor * emissive_map.rgb;
        
            return surface;
          }
        """
                    .trimIndent()
            }
        }

        fun Lighting(fullyRough: Boolean = false) = shaderBlock {
            body {
                """
                    const PI = $PI;

                    const LightType_Point = 0u;
                    const LightType_Spot = 1u;
                    const LightType_Directional = 2u;
        
                    struct PunctualLight {
                      lightType : u32,
                      pointToLight : vec3f,
                      range : f32,
                      color : vec3f,
                      intensity : f32,
                    };
        
                    fn FresnelSchlick(cosTheta : f32, F0 : vec3f) -> vec3f {
                        return F0 + (vec3(1.0) - F0) * pow(1.0 - cosTheta, 5.0);
                    }
        
                    fn DistributionGGX(N : vec3f, H : vec3f, roughness : f32) -> f32 {
                      let a      = roughness*roughness;
                      let a2     = a*a;
                      let NdotH  = max(dot(N, H), 0.0);
                      let NdotH2 = NdotH*NdotH;
        
                      let num    = a2;
                      let denom  = (NdotH2 * (a2 - 1.0) + 1.0);
        
                      return num / (PI * denom * denom);
                    }
        
                    fn GeometrySchlickGGX(NdotV : f32, roughness : f32) -> f32 {
                      let r = (roughness + 1.0);
                      let k = (r*r) / 8.0;
        
                      let num   = NdotV;
                      let denom = NdotV * (1.0 - k) + k;
        
                      return num / denom;
                    }
        
                    fn GeometrySmith(N : vec3f, V : vec3f, L : vec3f, roughness : f32) -> f32 {
                      let NdotV = max(dot(N, V), 0.0);
                      let NdotL = max(dot(N, L), 0.0);
                      let ggx2  = GeometrySchlickGGX(NdotV, roughness);
                      let ggx1  = GeometrySchlickGGX(NdotL, roughness);
        
                      return ggx1 * ggx2;
                    }
        
                    fn lightAttenuation(light : PunctualLight) -> f32 {
                      if (light.lightType == LightType_Directional) {
                        return 1.0;
                      }
        
                      let distance = length(light.pointToLight);
                      if (light.range <= 0.0) {
                          // Negative range means no cutoff
                          return 1.0 / pow(distance, 2.0);
                      }
                      return clamp(1.0 - pow(distance / light.range, 4.0), 0.0, 1.0) / pow(distance, 2.0);
                    }
        
                    fn lightRadiance(light : PunctualLight, surface : SurfaceInfo) -> vec3f {
                      let L = normalize(light.pointToLight);
                      let H = normalize(surface.v + L);
        
                    // cook-torrance brdf
                    ${
                        if (fullyRough) {
                            """
                            let NDF = 1.0 / PI;
                            let G = 1.0;
                        """.trimIndent()
                        } else {
                            """
                            let NDF = DistributionGGX(surface.normal, H, surface.roughness);
                            let G = GeometrySmith(surface.normal, surface.v, L, surface.roughness);
                        """.trimIndent()
                        }
                    }
                    let F = FresnelSchlick(max(dot(H, surface.v), 0.0), surface.f0);
        
                    let kD = (vec3(1.0) - F) * (1.0 - surface.metallic);
                    let NdotL = max(dot(surface.normal, L), 0.0);
        
                    let numerator = NDF * G * F;
                    let denominator = max(4.0 * max(dot(surface.normal, surface.v), 0.0) * NdotL, 0.0001);
                    let specular = numerator / vec3(denominator);
        
                    // add to outgoing radiance Lo
                    let radiance = light.color * light.intensity * lightAttenuation(light);
                    return (kD * surface.albedo / vec3(PI) + specular) * radiance * NdotL;
                }
                """
                    .trimIndent()
            }
        }

        fun FragmentOutputStruct(bloomEnabled: Boolean = false) =
            shaderStruct("FragmentOutput") {
                mutableMapOf(
                        "color" to
                            ShaderStructParameterType.Location(
                                0,
                                ShaderStructParameterType.WgslType.vec4f,
                            )
                    )
                    .also {
                        if (bloomEnabled) {
                            it["emissive"] =
                                ShaderStructParameterType.Location(
                                    1,
                                    ShaderStructParameterType.WgslType.vec4f,
                                )
                        }
                    }
            }

        fun FragmentShader(
            layout: List<VertexAttribute>,
            bloomEnabled: Boolean = false,
            shadowsEnabled: Boolean = false,
            fullyRough: Boolean = false,
        ) = shader {
            val input = VertexOutputStruct(layout)
            val output = FragmentOutputStruct(bloomEnabled)
            include(input)
            include(output)
            include(CommonShaderBlocks.Camera(0, 0))
            include(CommonShaderBlocks.Lights(0, 1))
            include(CommonShaderBlocks.ClusterLights(0, 2))
            include(Material(1))
            include(CommonShaderBlocks.TileFunctions())
            include(Lighting(fullyRough))
            include(SurfaceInfo(layout))

            fragment {
                //                main(input, output) {
                //                    """
                //                    %surface%
                //                    let surface = GetSurfaceInfo(input);
                //
                //                    // reflectance equation
                //                    var Lo = vec3(0.0, 0.0, 0.0);
                //
                //                    %global_lights%
                //                    // Process the directional light if one is present
                //                    if (global_lights.dir_intensity > 0.0) {
                //                      var light : PunctualLight;
                //                      light.lightType = LightType_Directional;
                //                      light.pointToLight = global_lights.dir_direction;
                //                      light.color = global_lights.dir_color;
                //                      light.intensity = global_lights.dir_intensity;
                //
                //                      ${if (shadowsEnabled) "let lightVis =
                // dirLightVisibility(input.world_pos);" else "let lightVis = 1.0;"}
                //
                //                      // calculate per-light radiance and add to outgoing radiance
                // Lo
                //                      Lo = Lo + lightRadiance(light, surface) * lightVis;
                //                    }
                //
                //                    // Process each other light in the scene.
                //                    let clusterIndex = get_cluster_index(input.position);
                //                    let lightOffset  = clusterLights.lights[clusterIndex].offset;
                //                    let lightCount   = clusterLights.lights[clusterIndex].count;
                //
                //                    %point_lights%
                //                    for (var lightIndex = 0u; lightIndex < lightCount; lightIndex
                // = lightIndex + 1u) {
                //                      let i = clusterLights.indices[lightOffset + lightIndex];
                //
                //                      var light : PunctualLight;
                //                      light.lightType = LightType_Point;
                //                      light.pointToLight = global_lights.lights[i].position.xyz -
                // input.world_pos;
                //                      light.range = global_lights.lights[i].range;
                //                      light.color = global_lights.lights[i].color;
                //                      light.intensity = global_lights.lights[i].intensity;
                //
                //                      ${if (shadowsEnabled) "let lightVis =
                // pointLightVisibility(i, input.world_pos, light.pointToLight);" else "let lightVis
                // = 1.0;"}
                //
                //                      // calculate per-light radiance and add to outgoing radiance
                // Lo
                //                      Lo = Lo + lightRadiance(light, surface) * lightVis;
                //                    }
                //
                //                    %ambient%
                //                    let ambient = global_lights.ambient * surface.albedo *
                // surface.ao;
                //                    %color%
                //                    let color = linear_to_sRGB(Lo + ambient + surface.emissive);
                //
                //                    var out : FragmentOutput;
                //                    out.color = vec4(color, surface.base_color.a); //
                // vec4(surface.normal * 0.5 + 0.5, 1.0);
                //                    ${if (bloomEnabled) "out.emissive = vec4(surface.emissive,
                // surface.base_color.a);" else ""}
                //                    %output%
                //                    return out;
                //                    """
                //                        .trimIndent()
                //                }
            }
        }
    }
}
