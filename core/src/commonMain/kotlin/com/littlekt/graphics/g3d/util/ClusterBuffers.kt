package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.IntBuffer
import com.littlekt.graphics.g3d.util.shader.ClusteredComputeShaderBuilder.Companion.DEFAULT_WORK_GROUP_SIZE_X
import com.littlekt.graphics.g3d.util.shader.ClusteredComputeShaderBuilder.Companion.DEFAULT_WORK_GROUP_SIZE_Y
import com.littlekt.graphics.g3d.util.shader.ClusteredComputeShaderBuilder.Companion.DEFAULT_WORK_GROUP_SIZE_Z
import com.littlekt.graphics.g3d.util.shader.CommonSubShaderFunctions
import com.littlekt.graphics.webgpu.*
import kotlin.math.ceil

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class ClusterBuffers(
    val device: Device,
    tileCountX: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_X,
    tileCountY: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Y,
    tileCountZ: Int = CommonSubShaderFunctions.DEFAULT_TILE_COUNT_Z,
    workGroupSizeX: Int = DEFAULT_WORK_GROUP_SIZE_X,
    workGroupSizeY: Int = DEFAULT_WORK_GROUP_SIZE_Y,
    workGroupSizeZ: Int = DEFAULT_WORK_GROUP_SIZE_Z,
) : Releasable {
    private val totalTiles = tileCountX * tileCountY * tileCountZ
    private val maxClusteredLights = totalTiles * 64
    /** Cluster x, y, z, size * 32 bytes per cluster. */
    private val clusterBoundsSize = totalTiles * 32
    val workGroupSizeX = ceil((tileCountX / workGroupSizeX).toDouble()).toInt()
    val workGroupSizeY = ceil(tileCountY / workGroupSizeY.toDouble()).toInt()
    val workGroupSizeZ = ceil(tileCountZ / workGroupSizeZ.toDouble()).toInt()
    private val clusterLightsSize = 4 + (8 * totalTiles) + (4 * maxClusteredLights)

    protected val emptyData = IntBuffer(1)

    /** The [GPUBuffer] that holds the cluster bounds storage data */
    val clusterBoundsStorageBuffer =
        device.createBuffer(
            BufferDescriptor(
                "cluster bounds",
                clusterBoundsSize.toLong(),
                BufferUsage.STORAGE,
                false,
            )
        )

    /** The [BufferBinding] for [clusterBoundsStorageBuffer]. */
    val clusterBoundsStorageBufferBinding = BufferBinding(clusterBoundsStorageBuffer)

    /** The [GPUBuffer] that holds the cluster bounds storage data */
    val clusterLightsStorageBuffer =
        device.createBuffer(
            BufferDescriptor(
                "cluster lights",
                clusterLightsSize.toLong(),
                BufferUsage.STORAGE or BufferUsage.COPY_DST,
                false,
            )
        )

    /** The [BufferBinding] for [clusterLightsStorageBuffer]. */
    val clusterLightsStorageBufferBinding = BufferBinding(clusterLightsStorageBuffer)

    /** Resets the [clusterLightsStorageBuffer] offset back to zero. */
    fun resetClusterLightsOffsetToZero() =
        device.queue.writeBuffer(clusterLightsStorageBuffer, emptyData)

    override fun release() {
        clusterBoundsStorageBuffer.release()
        clusterLightsStorageBuffer.release()
    }
}
