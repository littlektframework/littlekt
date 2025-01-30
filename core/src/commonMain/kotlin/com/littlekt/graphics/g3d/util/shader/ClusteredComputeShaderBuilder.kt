package com.littlekt.graphics.g3d.util.shader

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

    companion object {
        const val DEFAULT_WORK_GROUP_SIZE_X = 4
        const val DEFAULT_WORK_GROUP_SIZE_Y = 2
        const val DEFAULT_WORK_GROUP_SIZE_Z = 4
    }
}
