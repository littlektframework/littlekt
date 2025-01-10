package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import kotlin.math.PI

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
class PBRMaterialBuilder : SubFragmentShaderBuilder() {
    override fun material(group: Int) {
        parts +=
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
    }

    fun surfaceInfo(attributes: List<VertexAttribute>) {
        parts +=
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
                let tbn = mat3x3 (input.tangent, input.bitangent, input.normal);
                let N = textureSample (normal_texture, normal_sampler, input.uv).rgb;
                surface.normal = normalize(tbn * (2.0 * N - vec3(1.0)));
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
        
            surface.albedo = surface.base_color.rgb;
//            if (input.instanceColor.a == 0.0) {
//              surface.albedo = surface.albedo + input.instanceColor.rgb;
//            } else {
//              surface.albedo = surface.albedo * input.instanceColor.rgb;
//            }
        
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

    fun pbrFunctions(fullyRough: Boolean) {
        parts +=
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
              let denominator = max(4.0 * max(dot(surface.normal, surface.v), 0.0) * NdotL, 0.001);
              let specular = numerator / vec3(denominator);

              // add to outgoing radiance Lo
              let radiance = light.color * light.intensity * lightAttenuation(light);
              return (kD * surface.albedo / vec3(PI) + specular) * radiance * NdotL;
            }
        """
                .trimIndent()
    }

    fun main(
        entryPoint: String = "fs_main",
        fullyRough: Boolean = true,
        bloomEnabled: Boolean = false,
        shadowsEnabled: Boolean = false,
    ) {
        pbrFunctions(fullyRough)
        colorConversionFunctions()
        parts +=
            """
            struct FragmentOutput {
                @location(0) color : vec4<f32>,
                ${if (bloomEnabled) "@location(1) emissive : vec4<f32>," else ""}
            };
            @fragment
            fn $entryPoint(input : VertexOutput) -> FragmentOutput {
                let surface = GetSurfaceInfo(input);
            
                // reflectance equation
                var Lo = vec3(0.0, 0.0, 0.0);
            
                // Process the directional light if one is present
                if (global_lights.dir_intensity > 0.0) {
                  var light : PunctualLight;
                  light.lightType = LightType_Directional;
                  light.pointToLight = global_lights.dir_direction;
                  light.color = global_lights.dir_color;
                  light.intensity = global_lights.dir_intensity;
            
                  ${if(shadowsEnabled) "let lightVis = dirLightVisibility(input.world_pos);" else "let lightVis = 1.0;"}
            
                  // calculate per-light radiance and add to outgoing radiance Lo
                  Lo = Lo + lightRadiance(light, surface) * lightVis;
                }
            
                // Process each other light in the scene.
                let clusterIndex = get_cluster_index(input.position);
                let lightOffset  = clusterLights.lights[clusterIndex].offset;
                let lightCount   = clusterLights.lights[clusterIndex].count;
            
                for (var lightIndex = 0u; lightIndex < lightCount; lightIndex = lightIndex + 1u) {
                  let i = clusterLights.indices[lightOffset + lightIndex];
            
                  var light : PunctualLight;
                  light.lightType = LightType_Point;
                  light.pointToLight = global_lights.lights[i].position.xyz - input.world_pos;
                  light.range = global_lights.lights[i].range;
                  light.color = global_lights.lights[i].color;
                  light.intensity = global_lights.lights[i].intensity;
            
                  ${if(shadowsEnabled) "let lightVis = pointLightVisibility(i, input.world_pos, light.pointToLight);" else "let lightVis = 1.0;"}
            
                  // calculate per-light radiance and add to outgoing radiance Lo
                  Lo = Lo + lightRadiance(light, surface) * lightVis;
                }
            
                let ambient = global_lights.ambient * surface.albedo * surface.ao;
                let color = linear_to_sRGB(Lo + ambient + surface.emissive);
            
                var out : FragmentOutput;
                out.color = vec4(color, surface.base_color.a);
                ${if(bloomEnabled) "out.emissive = vec4(surface.emissive, surface.base_color.a);" else ""}
                
                return out;
            };
        """
    }

    override fun main(entryPoint: String) =
        main(
            entryPoint = entryPoint,
            fullyRough = true,
            bloomEnabled = false,
            shadowsEnabled = false,
        )
}
