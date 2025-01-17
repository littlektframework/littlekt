package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusteredBoundsComputeShaderBuilder(
    tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
    tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
    workGroupSizeX: Int = DEFAULT_WORK_GROUP_SIZE_X,
    workGroupSizeY: Int = DEFAULT_WORK_GROUP_SIZE_Y,
    workGroupSizeZ: Int = DEFAULT_WORK_GROUP_SIZE_Z,
    maxLightsPerCluster: Int = CommonSubShaderFunctions.DEFAULT_MAX_LIGHTS_PER_CLUSTER,
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
    /** Requires [cluster] with [MemoryAccessMode.READ_WRITE] and [camera]. */
    override fun main(entryPoint: String) {
        parts +=
            """
              fn lineIntersectionToZPlane(a : vec3<f32>, b : vec3<f32>, zDistance : f32) -> vec3<f32> {
                let normal = vec3(0.0, 0.0, 1.0);
                let ab =  b - a;
                let t = (zDistance - dot(normal, a)) / dot(normal, ab);
                return a + t * ab;
              }
    
              fn clipToView(clip : vec4<f32>) -> vec4<f32> {
                let view = camera.inverse_projection * clip;
                return view / vec4(view.w, view.w, view.w, view.w);
              }
    
              fn screen2View(screen : vec4<f32>) -> vec4<f32> {
                let texCoord = screen.xy / camera.output_size.xy;
                let clip = vec4(vec2(texCoord.x, 1.0 - texCoord.y) * 2.0 - vec2(1.0, 1.0), screen.z, screen.w);
                return clipToView(clip);
              }
    
              const tileCount = vec3(${tileCountX}u, ${tileCountY}u, ${tileCountZ}u);
              const eyePos = vec3(0.0);
    
              @compute @workgroup_size(${workGroupSizeX}, ${workGroupSizeY}, ${workGroupSizeZ})
              fn $entryPoint(@builtin(global_invocation_id) global_id : vec3<u32>) {
                let tileIndex: u32 = global_id.x +
                                      global_id.y * tileCount.x +
                                      global_id.z * tileCount.x * tileCount.y;
    
                let tileSize = vec2(camera.output_size.x / f32(tileCount.x),
                                    camera.output_size.y / f32(tileCount.y));
    
                let maxPoint_sS = vec4(vec2(f32(global_id.x+1u), f32(global_id.y+1u)) * tileSize, 0.0, 1.0);
                let minPoint_sS = vec4(vec2(f32(global_id.x), f32(global_id.y)) * tileSize, 0.0, 1.0);
    
                let maxPoint_vS = screen2View(maxPoint_sS).xyz;
                let minPoint_vS = screen2View(minPoint_sS).xyz;
    
                let tileNear: f32 = -camera.z_near * pow(camera.z_far/ camera.z_near, f32(global_id.z)/f32(tileCount.z));
                let tileFar: f32 = -camera.z_near * pow(camera.z_far/ camera.z_near, f32(global_id.z+1u)/f32(tileCount.z));
    
                let minPointNear = lineIntersectionToZPlane(eyePos, minPoint_vS, tileNear);
                let minPointFar = lineIntersectionToZPlane(eyePos, minPoint_vS, tileFar);
                let maxPointNear = lineIntersectionToZPlane(eyePos, maxPoint_vS, tileNear);
                let maxPointFar = lineIntersectionToZPlane(eyePos, maxPoint_vS, tileFar);
    
                clusters.bounds[tileIndex].minAABB = min(min(minPointNear, minPointFar),min(maxPointNear, maxPointFar));
                clusters.bounds[tileIndex].maxAABB = max(max(minPointNear, minPointFar),max(maxPointNear, maxPointFar));
              }
        """
                .trimIndent()
    }
}