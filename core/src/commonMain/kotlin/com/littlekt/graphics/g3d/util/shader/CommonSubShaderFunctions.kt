package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.webgpu.MemoryAccessMode

fun SubShaderBuilder.colorConversionFunctions(
    useApproximateSrgb: Boolean = true,
    gamma: Float = 2.2f,
) {
    parts +=
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
}

fun SubShaderBuilder.vertexInput(attributes: List<VertexAttribute>) {
    val inputs =
        attributes.map { attribute ->
            when (attribute.usage) {
                VertexAttrUsage.POSITION ->
                    "@location(${attribute.shaderLocation}) position: vec3f,"

                VertexAttrUsage.COLOR -> "@location(${attribute.shaderLocation}) color: vec4f,"
                VertexAttrUsage.NORMAL -> "@location(${attribute.shaderLocation}) normal: vec3f,"
                VertexAttrUsage.TANGENT -> "@location(${attribute.shaderLocation}) tangent: vec4f,"
                VertexAttrUsage.UV -> {
                    if (attribute.index == 0) "@location(${attribute.shaderLocation}) uv: vec2f,"
                    else "@location(${attribute.shaderLocation}) uv2: vec2f,"
                }

                VertexAttrUsage.JOINT -> "@location(${attribute.shaderLocation}) joints: vec4i,"
                VertexAttrUsage.WEIGHT -> "@location(${attribute.shaderLocation}) weights: vec4f,"
                else -> {
                    error(
                        "Unknown attribute usage type: ${attribute.usage}. If using custom attributes, then you must create your own input struct!"
                    )
                }
            }
        }

    parts +=
        """
            struct VertexInput {
                @builtin(instance_index) instance_index: u32,
                ${inputs.joinToString("\n")}
            };
        """
            .trimIndent()
}

fun SubShaderBuilder.vertexOutput(attributes: List<VertexAttribute>) {
    parts +=
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

/** Adds a Camera struct that only requires a 4x4 float matrix for the combined viewProjection. */
fun SubShaderBuilder.camera(group: Int, binding: Int) {
    parts +=
        """
        struct Camera {
            view_proj: mat4x4f,
        };
        @group($group) @binding($binding)
        var<uniform> camera: Camera;
        """
            .trimIndent()
}

/**
 * Adds a Camera struct that requires separated camera data such as the projection, inverse
 * projection, view, position, time, output size, near, and far. Expected to be used with PBR &
 * clustered shading.
 */
fun SubShaderBuilder.cameraComplex(group: Int, binding: Int) {
    parts +=
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

fun SubShaderBuilder.models(group: Int, binding: Int) {
    parts +=
        """
        struct Model {
            transform: mat4x4f,
            color: vec4f,
        };
        @group($group) @binding($binding)
        var <storage, read> models: array<Model>;
        """
}

fun SubShaderBuilder.light(group: Int, binding: Int) {
    parts +=
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
}

fun SubShaderBuilder.skin(group: Int) {
    parts +=
        """
        struct Joints {
          matrices : array<mat4x4f>
        };
        @group(${group}) @binding(0) var<storage, read> joint : Joints;
        @group(${group}) @binding(1) var<storage, read> inverse_blend : Joints;
    """
            .trimIndent()
}

fun SubShaderBuilder.getSkinMatrix() {
    parts +=
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

fun SubShaderBuilder.tileFunctions(
    tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
    tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
) {
    parts +=
        """
            const tile_count = vec3(${tileCountX}u, ${tileCountY}u, ${tileCountZ}u);

            fn linear_depth(depth_sample : f32) -> f32 {
              let depth_range = 2.0 * depth_sample - 1.0;
              let linear = 2.0 * camera.z_far * camera.z_near / (camera.z_far + camera.z_near - depth_range * (camera.z_far - camera.z_near));
              return linear;
            }
            
            fn get_tile(frag_coord : vec4f) -> vec3u {
              let zTile = u32((log2(abs(frag_coord.z) / camera.z_near) * f32(tile_count.z)) / log2(camera.z_far / camera.z_near));
              let tileSize = camera.output_size / vec2(f32(tile_count.x), f32(tile_count.y));
              return vec3(u32(frag_coord.x / tileSize.x),
                          u32(frag_coord.y / tileSize.y),
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

/** Adds the `ClusterBounds` and `Clusters` structs with the storage buffer for `Clusters`. */
fun SubShaderBuilder.cluster(
    group: Int,
    binding: Int,
    tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
    tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
    access: MemoryAccessMode = MemoryAccessMode.READ,
) {
    val totalTiles = tileCountX * tileCountY * tileCountZ
    parts +=
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

/**
 * Adds the `Clusterlights` and `ClusterlightGroup` structs with the `ClusterlightGroup` storage
 * buffer.
 */
fun SubShaderBuilder.clusterLights(
    group: Int,
    binding: Int,
    tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
    tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
    access: MemoryAccessMode = MemoryAccessMode.READ,
) {
    val totalTiles = tileCountX * tileCountY * tileCountZ
    parts +=
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

object CommonSubShaderFunctions {
    const val DEFAULT_TILE_COUNT_X = 32
    const val DEFAULT_TILE_COUNT_Y = 18
    const val DEFAULT_TILE_COUNT_Z = 48
    const val DEFAULT_MAX_LIGHTS_PER_CLUSTER = 256
}
