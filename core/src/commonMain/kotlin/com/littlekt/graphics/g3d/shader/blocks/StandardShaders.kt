package com.littlekt.graphics.g3d.shader.blocks

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.shader.builder.shaderBindGroup
import com.littlekt.graphics.shader.builder.shaderBlock
import com.littlekt.graphics.shader.builder.shaderStruct
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import kotlin.math.PI

object Standard {
    fun VertexInputStruct(attributes: List<VertexAttribute>) =
        shaderStruct("VertexInput") {
            val inputs =
                attributes.map { attribute ->
                    when (attribute.usage) {
                        VertexAttrUsage.POSITION ->
                            "@location(${attribute.shaderLocation}) position: vec3f,"

                        VertexAttrUsage.COLOR ->
                            "@location(${attribute.shaderLocation}) color: vec4f,"
                        VertexAttrUsage.NORMAL ->
                            "@location(${attribute.shaderLocation}) normal: vec3f,"

                        VertexAttrUsage.TANGENT ->
                            "@location(${attribute.shaderLocation}) tangent: vec4f,"

                        VertexAttrUsage.UV -> {
                            if (attribute.index == 0)
                                "@location(${attribute.shaderLocation}) uv: vec2f,"
                            else "@location(${attribute.shaderLocation}) uv2: vec2f,"
                        }

                        VertexAttrUsage.JOINT ->
                            "@location(${attribute.shaderLocation}) joints: vec4i,"
                        VertexAttrUsage.WEIGHT ->
                            "@location(${attribute.shaderLocation}) weights: vec4f,"

                        else -> {
                            error(
                                "Unknown attribute usage type: ${attribute.usage}. If using custom attributes, then you must create your own input struct!"
                            )
                        }
                    }
                }

            """
            struct VertexInput {
                @builtin(instance_index) instance_index: u32,
                ${inputs.joinToString("\n")}
            };
        """
                .trimIndent()
        }

    fun VertexOutputStruct(attributes: List<VertexAttribute>) =
        shaderStruct("VertexOutput") {
            """
        struct VertexOutput {
            @builtin(position) position: vec4f,
            @location(0) world_pos: vec3f,
            @location(1) view: vec3f,
            @location(2) uv: vec2f,
            @location(3) uv2: vec2f,
            @location(4) color: vec4f,
            @location(5) instance_color: vec4f,
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

        vertex {
            main(input = input, output = output) {
                before("vertex_layout_block", vertexLayoutBlock(layout))
                """
                var output: VertexOutput;
                %model%
                let model_matrix = models[input.instance_index].transform;
                %vertex_layout_block%
                %output%
                return output; 
                """
                    .trimIndent()
            }
        }
    }

    fun SkinnedVertexShader(layout: List<VertexAttribute>) = shader {
        val input = VertexInputStruct(layout)
        val output = VertexOutputStruct(layout)
        include(input)
        include(output)
        include(CommonShaderBlocks.Camera(0, 0))
        include(CommonShaderBlocks.Model(1, 0))
        include(CommonShaderBlocks.Skin(2, 0))

        vertex {
            main(input = input, output = output) {
                before("vertex_layout_block", vertexLayoutBlock(layout))
                """
                var output: VertexOutput;
                %model%
                let model_matrix = get_skin_matrix(input);
                %vertex_layout_block%
                %output%
                return output; 
                """
                    .trimIndent()
            }
        }
    }

    object Unlit {
        fun Material(group: Int) =
            shaderStruct("Material") {
                """
            struct Material {
                base_color_factor : vec4f,
                alpha_cutoff : f32,
            };
            @group($group) @binding(0) var<uniform> material : Material;
            
            @group($group) @binding(1) var base_color_texture : texture_2d<f32>;
            @group($group) @binding(2) var base_color_sampler : sampler;
            """
                    .trimIndent()
            }

        fun FragmentOutput() =
            shaderStruct("FragmentOutput") {
                """
            struct FragmentOutput {
                @location(0) color : vec4f,
            };
            """
                    .trimIndent()
            }

        fun FragmentShader(layout: List<VertexAttribute>) = shader {
            val input = VertexInputStruct(layout)
            val output = FragmentOutput()
            include(input)
            include(output)
            include(CommonShaderBlocks.ColorConversionFunctions())
            fragment {
                main(input, output) {
                    """
                    var output: FragmentOutput;
                    %base_color%    
                    let base_color_map = textureSample(base_color_texture, base_color_sampler, input.uv);
                    %alpha_cutoff%
                    if (base_color_map.a < material.alpha_cutoff) {
                      discard;
                    }
                    let base_color = input.color * material.base_color_factor * base_color_map * input.instance_color;
                    %color%
                    output.color = vec4(linear_to_sRGB(base_color.rgb), base_color.a);
                    %output%
                    return output;
                """
                        .trimIndent()
                }
            }
        }
    }

    object PBR {
        fun Material(group: Int) =
            shaderBindGroup(
                group,
                BindingUsage.MATERIAL,
                BindGroupLayoutDescriptor(
                    listOf(
                        // material uniform
                        BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                        // baseColorTexture
                        BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        // baseColorSampler
                        BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        // normal texture
                        BindGroupLayoutEntry(3, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        // normal sampler
                        BindGroupLayoutEntry(4, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        // metallic roughness texture
                        BindGroupLayoutEntry(5, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        // metallic roughness sampler
                        BindGroupLayoutEntry(6, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        // occlusion texture
                        BindGroupLayoutEntry(7, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        // occlusion sampler
                        BindGroupLayoutEntry(8, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        // emissive texture
                        BindGroupLayoutEntry(9, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        // emissive sampler
                        BindGroupLayoutEntry(10, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    )
                ),
            ) {
                """
        struct Material {
            base_color_factor : vec4f,
            metallic_roughness_factor: vec2f,
            occlusion_strength: f32,
            emissive_factor: vec3f,
            alpha_cutoff : f32,
        };
        @group($group) @binding(0) var<uniform> material : Material;
        
        @group(${group}) @binding(1) var base_color_texture : texture_2d<f32>;
        @group(${group}) @binding(2) var base_color_sampler : sampler;
        @group(${group}) @binding(3) var normal_texture : texture_2d<f32>;
        @group(${group}) @binding(4) var normal_sampler : sampler;
        @group(${group}) @binding(5) var metallic_roughness_texture : texture_2d<f32>;
        @group(${group}) @binding(6) var metallic_roughness_sampler : sampler;
        @group(${group}) @binding(7) var occlusion_texture : texture_2d<f32>;
        @group(${group}) @binding(8) var occlusion_sampler : sampler;
        @group(${group}) @binding(9) var emissive_texture : texture_2d<f32>;
        @group(${group}) @binding(10) var emissive_sampler : sampler;
        """
                    .trimIndent()
            }

        fun SurfaceInfo(attributes: List<VertexAttribute>) = shaderBlock {
            body {
                """
          struct SurfaceInfo {
            base_color : vec4f,
            albedo : vec3f,
            metallic : f32,
            roughness : f32,
            normal : vec3f,
            f0 : vec3f,
            ao : f32,
            emissive : vec3f,
            v : vec3f,
          };
        
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

        fun FragmentOutput(bloomEnabled: Boolean = false) =
            shaderStruct("FragmentOutput") {
                """
            struct FragmentOutput {
                @location(0) color : vec4f,
                ${if (bloomEnabled) "@location(1) emissive : vec4f," else ""}
            };
            """
                    .trimIndent()
            }

        fun FragmentShader(
            layout: List<VertexAttribute>,
            bloomEnabled: Boolean = false,
            shadowsEnabled: Boolean = false,
            fullyRough: Boolean = false,
        ) = shader {
            val input = VertexOutputStruct(layout)
            val output = FragmentOutput(bloomEnabled)
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
                main(input, output) {
                    """
                    %surface%
                    let surface = GetSurfaceInfo(input);
                
                    // reflectance equation
                    var Lo = vec3(0.0, 0.0, 0.0);
                
                    %global_lights%
                    // Process the directional light if one is present
                    if (global_lights.dir_intensity > 0.0) {
                      var light : PunctualLight;
                      light.lightType = LightType_Directional;
                      light.pointToLight = global_lights.dir_direction;
                      light.color = global_lights.dir_color;
                      light.intensity = global_lights.dir_intensity;
                
                      ${if (shadowsEnabled) "let lightVis = dirLightVisibility(input.world_pos);" else "let lightVis = 1.0;"}
                
                      // calculate per-light radiance and add to outgoing radiance Lo
                      Lo = Lo + lightRadiance(light, surface) * lightVis;
                    }
                
                    // Process each other light in the scene.
                    let clusterIndex = get_cluster_index(input.position);
                    let lightOffset  = clusterLights.lights[clusterIndex].offset;
                    let lightCount   = clusterLights.lights[clusterIndex].count;
                
                    %point_lights%
                    for (var lightIndex = 0u; lightIndex < lightCount; lightIndex = lightIndex + 1u) {
                      let i = clusterLights.indices[lightOffset + lightIndex];
                
                      var light : PunctualLight;
                      light.lightType = LightType_Point;
                      light.pointToLight = global_lights.lights[i].position.xyz - input.world_pos;
                      light.range = global_lights.lights[i].range;
                      light.color = global_lights.lights[i].color;
                      light.intensity = global_lights.lights[i].intensity;
                
                      ${if (shadowsEnabled) "let lightVis = pointLightVisibility(i, input.world_pos, light.pointToLight);" else "let lightVis = 1.0;"}
                
                      // calculate per-light radiance and add to outgoing radiance Lo
                      Lo = Lo + lightRadiance(light, surface) * lightVis;
                    }
                
                    %ambient%
                    let ambient = global_lights.ambient * surface.albedo * surface.ao;
                    %color%
                    let color = linear_to_sRGB(Lo + ambient + surface.emissive);
                
                    var out : FragmentOutput;
                    out.color = vec4(color, surface.base_color.a); // vec4(surface.normal * 0.5 + 0.5, 1.0);
                    ${if (bloomEnabled) "out.emissive = vec4(surface.emissive, surface.base_color.a);" else ""}
                    %output%
                    return out;
                    """
                        .trimIndent()
                }
            }
        }
    }
}
