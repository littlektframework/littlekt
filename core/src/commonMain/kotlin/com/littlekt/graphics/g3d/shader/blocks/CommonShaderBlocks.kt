package com.littlekt.graphics.g3d.shader.blocks

import com.littlekt.graphics.shader.builder.*
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.MemoryAccessMode

object CommonShaderBlocks {
    val JointsStruct =
        shaderStruct("Joints") {
            mapOf(
                "matrices" to
                    ShaderStructParameterType.array(ShaderStructParameterType.WgslType.mat4x4f)
            )
        }

    fun Joints(group: Int, binding: Int) = shaderBlock {
        include(JointsStruct)
        bindGroup(group, BindingUsage.SKIN) {
            bind(binding, "joint", JointsStruct, ShaderBindingType.Storage(MemoryAccessMode.READ))
            bind(
                binding + 1,
                "inverse_blend",
                JointsStruct,
                ShaderBindingType.Storage(MemoryAccessMode.READ),
            )
        }
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

    fun Skin(group: Int, binding: Int) = shaderBlock {
        include(Joints(group, binding))
        SkinFunctions()
    }

    val CameraStruct =
        shaderStruct("Camera") {
            mapOf(
                "proj" to ShaderStructParameterType.WgslType.mat4x4f,
                "inverse_projection" to ShaderStructParameterType.WgslType.mat4x4f,
                "view" to ShaderStructParameterType.WgslType.mat4x4f,
                "position" to ShaderStructParameterType.WgslType.vec3f,
                "time" to ShaderStructParameterType.WgslType.f32,
                "output_size" to ShaderStructParameterType.WgslType.vec2f,
                "z_near" to ShaderStructParameterType.WgslType.f32,
                "z_far" to ShaderStructParameterType.WgslType.f32,
            )
        }

    fun Camera(group: Int, binding: Int) = shaderBlock {
        include(CameraStruct)
        bindGroup(group, BindingUsage.CAMERA) {
            bind(binding, "camera", CameraStruct, ShaderBindingType.Uniform)
        }
    }

    val ModelStruct =
        shaderStruct("Model") {
            mapOf(
                "transform" to ShaderStructParameterType.WgslType.mat4x4f,
                "color" to ShaderStructParameterType.WgslType.vec4f,
            )
        }

    fun Model(group: Int, binding: Int) = shaderBlock {
        include(ModelStruct)
        bindGroup(group, BindingUsage.MODEL) {
            bindArray(
                binding,
                "models",
                ModelStruct,
                ShaderBindingType.Storage(MemoryAccessMode.READ),
            )
        }
    }

    val LightStruct =
        shaderStruct("Light") {
            mapOf(
                "position" to ShaderStructParameterType.WgslType.vec3f,
                "range" to ShaderStructParameterType.WgslType.f32,
                "color" to ShaderStructParameterType.WgslType.vec3f,
                "intensity" to ShaderStructParameterType.WgslType.f32,
            )
        }

    val GlobalLightsStruct =
        shaderStruct("GlobalLights") {
            mapOf(
                "ambient" to ShaderStructParameterType.WgslType.vec3f,
                "dir_color" to ShaderStructParameterType.WgslType.vec3f,
                "dir_intensity" to ShaderStructParameterType.WgslType.f32,
                "dir_direction" to ShaderStructParameterType.WgslType.vec3f,
                "light_count" to ShaderStructParameterType.WgslType.u32,
                "lights" to ShaderStructParameterType.array(LightStruct),
            )
        }

    fun Lights(group: Int, binding: Int) = shaderBlock {
        include(LightStruct)
        include(GlobalLightsStruct)
        bindGroup(group, BindingUsage.LIGHT) {
            bind(
                binding,
                "global_lights",
                GlobalLightsStruct,
                ShaderBindingType.Storage(MemoryAccessMode.READ),
            )
        }
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

    val ClusterBoundsStruct =
        shaderStruct("ClusterBounds") {
            mapOf(
                "minAABB" to ShaderStructParameterType.WgslType.vec3f,
                "maxAABB" to ShaderStructParameterType.WgslType.vec3f,
            )
        }

    fun ClustersStruct(boundsArrayLength: Int) =
        shaderStruct("Clusters") {
            mapOf(
                "bounds" to ShaderStructParameterType.array(ClusterBoundsStruct, boundsArrayLength)
            )
        }

    val ClusterLightsStruct =
        shaderStruct("ClusterLights") {
            mapOf(
                "offset" to ShaderStructParameterType.WgslType.u32,
                "count" to ShaderStructParameterType.WgslType.u32,
            )
        }

    fun ClusterLightGroupStruct(lightsArrayLength: Int, atomicOffset: Boolean) =
        shaderStruct("ClusterLightGroup") {
            mapOf(
                "offset" to
                    if (atomicOffset) ShaderStructParameterType.WgslType.atomic.u32
                    else ShaderStructParameterType.WgslType.u32,
                "lights" to ShaderStructParameterType.array(LightStruct, lightsArrayLength),
                "indices" to ShaderStructParameterType.array(ShaderStructParameterType.WgslType.u32),
            )
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
            include(ClusterBoundsStruct)
            include(ClustersStruct(totalTiles))
            bindGroup(group, BindingUsage.CLUSTER_BOUNDS) {
                bind(
                    binding,
                    "clusters",
                    ClustersStruct(totalTiles),
                    ShaderBindingType.Storage(access),
                )
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
        val clusterLightGroupStruct =
            ClusterLightGroupStruct(totalTiles, access != MemoryAccessMode.READ)
        return shaderBlock {
            include(ClusterLightsStruct)
            include(clusterLightGroupStruct)
            bindGroup(group, BindingUsage.CLUSTER_LIGHTS) {
                bind(
                    binding,
                    "clusterLights",
                    clusterLightGroupStruct,
                    ShaderBindingType.Storage(access),
                )
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
