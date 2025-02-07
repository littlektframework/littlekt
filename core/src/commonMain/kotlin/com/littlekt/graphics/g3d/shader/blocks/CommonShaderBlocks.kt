package com.littlekt.graphics.g3d.shader.blocks

import com.littlekt.graphics.shader.builder.ShaderBlock
import com.littlekt.graphics.shader.builder.shaderBlock
import com.littlekt.graphics.shader.builder.shaderStruct
import com.littlekt.graphics.webgpu.MemoryAccessMode

object CommonShaderBlocks {
    fun Material(group: Int) =
        shaderStruct("Material") {
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

    fun Joints(group: Int) =
        shaderStruct("Skin") {
            """
        struct Joints {
          matrices : array<mat4x4f>
        };
        @group(${group}) @binding(0) var<storage, read> joint : Joints;
        @group(${group}) @binding(1) var<storage, read> inverse_blend : Joints;
    """
                .trimIndent()
        }

    fun SkinFunctions() = shaderBlock {
        body {
            """
              fn get_skin_matrix(input : VertexInput) -> mat4x4f {
                let joint0 = joint.matrices[input.joints.x] * inverse_blend.matrices[input.joints.x];
                let joint1 = joint.matrices[input.joints.y] * inverse_blend.matrices[input.joints.y];
                let joint2 = joint.matrices[input.joints.z] * inverse_blend.matrices[input.joints.z];
                let joint3 = joint.matrices[input.joints.w] * inverse_blend.matrices[input.joints.w];
    
                let skin_matrix = joint0 * input.weights.x +
                                 joint1 * input.weights.y +
                                 joint2 * input.weights.z +
                                 joint3 * input.weights.w;
                return skin_matrix;
              }
            """
                .trimIndent()
        }
    }

    fun Skin(group: Int) = shaderBlock {
        include(Joints(group))
        SkinFunctions()
    }

    fun Camera(group: Int, binding: Int) =
        shaderStruct("Camera") {
            """
        struct Camera {
            proj: mat4x4f,
            inverse_projection: mat4x4f,
            view: mat4x4f,
            position: vec3f,
            time: f32,
            output_size: vec2f,
            z_near: f32,
            z_far: f32,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
    """
                .trimIndent()
        }

    fun Model(group: Int, binding: Int) =
        shaderStruct("Model") {
            """
        struct Model {
            transform: mat4x4f,
            color: vec4f,
        };
        @group($group) @binding($binding)
        var <storage, read> models: array<Model>;
    """
                .trimIndent()
        }

    fun Lights(group: Int, binding: Int) = shaderBlock {
        """
        struct Light {
          position : vec3f,
          range : f32,
          color : vec3f,
          intensity : f32,
        };
    
        struct GlobalLights {
          ambient : vec3f,
          dir_color : vec3f,
          dir_intensity : f32,
          dir_direction : vec3f,
          light_count : u32,
          lights : array<Light>,
        };
        @group($group) @binding($binding)
        var<storage, read> global_lights : GlobalLights;
        """
            .trimIndent()
    }

    fun TileFunctions(
        tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
        tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
        tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
    ) = shaderBlock {
        body {
            """
            const tile_count = vec3(${tileCountX}u, ${tileCountY}u, ${tileCountZ}u);

            fn linear_depth(depth_sample : f32) -> f32 {
              let depth_range = 2.0 * depth_sample - 1.0;
              let linear = 2.0 * camera.z_far * camera.z_near / (camera.z_far + camera.z_near - depth_range * (camera.z_far - camera.z_near));
              return linear;
            }
            
            fn get_tile(frag_coord : vec4f) -> vec3u {
              let sliceScale = f32(tile_count.z) / log2(camera.z_far / camera.z_near);
              let sliceBias = -(f32(tile_count.z) * log2(camera.z_near) / log2(camera.z_far / camera.z_near));
              let zTile = u32(max(log2(linear_depth(frag_coord.z)) * sliceScale + sliceBias, 0.0));
            
              return vec3<u32>(u32(frag_coord.x / (camera.output_size.x / f32(tile_count.x))),
                               u32(frag_coord.y / (camera.output_size.y / f32(tile_count.y))),
                               zTile);
            }
            
            fn get_cluster_index(frag_coord : vec4f) -> u32 {
              let tile = get_tile(frag_coord);
              return tile.x +
                     tile.y * tile_count.x +
                     tile.z * tile_count.x * tile_count.y;
            }
        """
                .trimIndent()
        }
    }

    fun ClusterBounds(
        group: Int,
        binding: Int,
        tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
        tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
        tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
        access: MemoryAccessMode = MemoryAccessMode.READ,
    ): ShaderBlock {
        val totalTiles = tileCountX * tileCountY * tileCountZ
        return shaderBlock {
            body {
                """
              struct ClusterBounds {
                minAABB : vec3<f32>,
                maxAABB : vec3<f32>,
              };
              struct Clusters {
                bounds : array<ClusterBounds, ${totalTiles}>
              };
              @group(${group}) @binding(${binding}) 
              var<storage, ${access.value}> clusters : Clusters;
            """
                    .trimIndent()
            }
        }
    }

    fun ClusterLights(
        group: Int,
        binding: Int,
        tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
        tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
        tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
        access: MemoryAccessMode = MemoryAccessMode.READ,
    ): ShaderBlock {
        val totalTiles = tileCountX * tileCountY * tileCountZ
        return shaderBlock {
            body {
                """
              struct ClusterLights {
                offset : u32,
                count : u32,
              };
              struct ClusterLightGroup {
                offset : ${if (access == MemoryAccessMode.READ) "u32" else "atomic<u32>"},
                lights : array<ClusterLights, ${totalTiles}>,
                indices : array<u32>,
              };
              @group(${group}) @binding(${binding}) 
              var<storage, ${access.value}> clusterLights : ClusterLightGroup;
               """
                    .trimIndent()
            }
        }
    }

    fun ColorConversionFunctions(useApproximateSrgb: Boolean = true, gamma: Float = 2.2f) =
        shaderBlock {
            body {
                """
        fn linear_to_sRGB(linear : vec3f) -> vec3f {
            ${
                if (useApproximateSrgb) {
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
            }
        }

    object CommonSubShaderFunctions {
        const val DEFAULT_TILE_COUNT_X = 32
        const val DEFAULT_TILE_COUNT_Y = 18
        const val DEFAULT_TILE_COUNT_Z = 48
        const val DEFAULT_MAX_LIGHTS_PER_CLUSTER = 256
    }
}
