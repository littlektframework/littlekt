package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusteredLightComputerShader(
    tileCountX: Int = DEFAULT_TILE_COUNT_X,
    tileCountY: Int = DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = DEFAULT_TILE_COUNT_Z,
    workGroupSizeX: Int = DEFAULT_WORK_GROUP_SIZE_X,
    workGroupSizeY: Int = DEFAULT_WORK_GROUP_SIZE_Y,
    workGroupSizeZ: Int = DEFAULT_WORK_GROUP_SIZE_Z,
    maxLightsPerCluster: Int = DEFAULT_MAX_LIGHTS_PER_CLUSTER,
) :
    ClusteredComputeShaderBuilder(
        tileCountX,
        tileCountY,
        tileCountZ,
        workGroupSizeX,
        workGroupSizeY,
        workGroupSizeZ,
        maxLightsPerCluster,
    ) {
    /**
     * Requires [camera], [cluster] with [MemoryAccessMode.READ], [clusterLights] with
     * [MemoryAccessMode.READ_WRITE], [light], and [tileFunctions].
     */
    override fun main(entryPoint: String) {
        parts +=
            """
               fn sqDistPointAABB(p : vec3<f32>, minAABB : vec3<f32>, maxAABB : vec3<f32>) -> f32 {
                 var sqDist = 0.0;
                 // const minAABB = clusters.bounds[tileIndex].minAABB;
                 // const maxAABB = clusters.bounds[tileIndex].maxAABB;

                 // Wait, does this actually work? Just porting code, but it seems suspect?
                 for(var i : i32 = 0; i < 3; i = i + 1) {
                   let v = p[i];
                   if(v < minAABB[i]){
                     sqDist = sqDist + (minAABB[i] - v) * (minAABB[i] - v);
                   }
                   if(v > maxAABB[i]){
                     sqDist = sqDist + (v - maxAABB[i]) * (v - maxAABB[i]);
                   }
                 }

                 return sqDist;
               }

               @compute @workgroup_size(${workGroupSizeX}, ${workGroupSizeY}, ${workGroupSizeZ})
               fn computeMain(@builtin(global_invocation_id) global_id : vec3<u32>) {
                 let tileIndex = global_id.x +
                                 global_id.y * tileCount.x +
                                 global_id.z * tileCount.x * tileCount.y;

                 // TODO: Look into improving threading using local invocation groups?
                 var clusterLightCount = 0u;
                 var cluserLightIndices : array<u32, ${maxLightsPerCluster}>;
                 for (var i = 0u; i < globalLights.lightCount; i = i + 1u) {
                   let range = globalLights.lights[i].range;
                   // Lights without an explicit range affect every cluster, but this is a poor way to handle that.
                   var lightInCluster : bool = range <= 0.0;

                   if (!lightInCluster) {
                     let lightViewPos = camera.view * vec4(globalLights.lights[i].position, 1.0);
                     let sqDist = sqDistPointAABB(lightViewPos.xyz, clusters.bounds[tileIndex].minAABB, clusters.bounds[tileIndex].maxAABB);
                     lightInCluster = sqDist <= (range * range);
                   }

                   if (lightInCluster) {
                     // Light affects this cluster. Add it to the list.
                     cluserLightIndices[clusterLightCount] = i;
                     clusterLightCount = clusterLightCount + 1u;
                   }

                   if (clusterLightCount == ${maxLightsPerCluster}u) {
                     break;
                   }
                 }

                 // TODO: Stick a barrier here and track cluster lights with an offset into a global light list
                 let lightCount = clusterLightCount;
                 var offset = atomicAdd(&clusterLights.offset, lightCount);

                 if (offset >= ${maxClusteredLights}u) {
                     return;
                 }

                 for(var i = 0u; i < clusterLightCount; i = i + 1u) {
                   clusterLights.indices[offset + i] = cluserLightIndices[i];
                 }
                 clusterLights.lights[tileIndex].offset = offset;
                 clusterLights.lights[tileIndex].count = clusterLightCount;
               }
        """
                .trimIndent()
    }
}