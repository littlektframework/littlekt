package com.littlekt.graphics.g3d.util.shader

import com.littlekt.graphics.webgpu.MemoryAccessMode

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
open class ComputeShaderBuilder {
    private val parts = mutableListOf<String>()

    /**
     * Requires [camera], [ClusteredComputeShaderBuilder.cluster] with [MemoryAccessMode.READ],
     * [ClusteredComputeShaderBuilder.clusterLights] with [MemoryAccessMode.READ_WRITE], [light],
     * and [ClusteredComputeShaderBuilder.tileFunctions].
     */
    fun clusteredLight(block: ClusteredLightComputeShaderBuilder.() -> Unit) {
        val builder = ClusteredLightComputeShaderBuilder()
        builder.block()
        parts += builder.build()
    }

    /**
     * Requires [ClusteredComputeShaderBuilder.cluster] with [MemoryAccessMode.READ_WRITE] and
     * [camera].
     */
    fun clusteredBounds(block: ClusteredBoundsComputeShaderBuilder.() -> Unit) {
        val builder = ClusteredBoundsComputeShaderBuilder()
        builder.block()
        parts += builder.build()
    }

    fun build(): String {
        return parts.joinToString("\n")
    }
}
