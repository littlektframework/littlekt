package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
abstract class ClusteredComputeShaderBuilder(
    val tileCountX: Int,
    val tileCountY: Int,
    val tileCountZ: Int,
    val workGroupSizeX: Int,
    val workGroupSizeY: Int,
    val workGroupSizeZ: Int,
    val maxLightsPerCluster: Int,
) : SubComputeShaderBuilder() {
    protected val totalTiles = tileCountX * tileCountY * tileCountZ
    protected val maxClusteredLights = totalTiles * 64

    /** Adds the `ClusterBounds` and `Clusters` structs with the storage buffer for `Clusters`. */
    fun cluster(group: Int, binding: Int, access: MemoryAccessMode = MemoryAccessMode.READ) {
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
    fun clusterLights(group: Int, binding: Int, access: MemoryAccessMode = MemoryAccessMode.READ) {
        parts +=
            """
              struct ClusterLights {
                offset : u32,
                count : u32,
              };
              struct ClusterLightGroup {
                offset : ${if(access == MemoryAccessMode.READ) "u32" else "atomic<u32>"},
                lights : array<ClusterLights, ${totalTiles}>,
                indices : array<u32, ${maxLightsPerCluster}>,
              };
              @group(${group}) @binding(${binding}) 
              var<storage, ${access.value}> clusterLights : ClusterLightGroup;
        """
                .trimIndent()
    }

    /** Adds functions related to calculating and getting the tile based on [camera] struct. */
    fun tileFunctions() {
        parts +=
            """
            const tileCount = vec3(${tileCountX}u, ${tileCountY}u, ${tileCountZ}u);

            fn linearDepth(depthSample : f32) -> f32 {
              return camera.zFar * camera.zNear / fma(depthSample, camera.zNear-camera.zFar, camera.zFar);
            }
            
            fn getTile(fragCoord : vec4<f32>) -> vec3<u32> {
              // TODO: scale and bias calculation can be moved outside the shader to save cycles.
              let sliceScale = f32(tileCount.z) / log2(camera.zFar / camera.zNear);
              let sliceBias = -(f32(tileCount.z) * log2(camera.zNear) / log2(camera.zFar / camera.zNear));
              let zTile = u32(max(log2(linearDepth(fragCoord.z)) * sliceScale + sliceBias, 0.0));
            
              return vec3(u32(fragCoord.x / (camera.outputSize.x / f32(tileCount.x))),
                          u32(fragCoord.y / (camera.outputSize.y / f32(tileCount.y))),
                          zTile);
            }
            
            fn getClusterIndex(fragCoord : vec4<f32>) -> u32 {
              let tile = getTile(fragCoord);
              return tile.x +
                     tile.y * tileCount.x +
                     tile.z * tileCount.x * tileCount.y;
            }
        """
                .trimIndent()
    }

    companion object {
        const val DEFAULT_TILE_COUNT_X = 32
        const val DEFAULT_TILE_COUNT_Y = 18
        const val DEFAULT_TILE_COUNT_Z = 48
        const val DEFAULT_WORK_GROUP_SIZE_X = 4
        const val DEFAULT_WORK_GROUP_SIZE_Y = 2
        const val DEFAULT_WORK_GROUP_SIZE_Z = 4
        const val DEFAULT_MAX_LIGHTS_PER_CLUSTER = 256
    }
}
