package com.lehaine.littlekt

import com.lehaine.littlekt.util.toString

/**
 * OpenGL related engine stats.
 * @author Colton Daily
 * @date 11/24/2021
 */
class EngineStats {
    val bufferAllocations = mutableMapOf<Long, Int>()
    var totalBufferSize = 0L
        private set

    var shaderSwitches = 0
        internal set
    var vertices = 0
        internal set
    var textureBindings = 0
        internal set
    var drawCalls = 0
        internal set
    var calls = 0
        internal set

    fun bufferAllocated(bufferId: Long, size: Int) {
        bufferAllocations.put(bufferId, size)?.let { totalBufferSize -= it }
        totalBufferSize += size
    }

    fun bufferDeleted(bufferId: Long) {
        bufferAllocations.remove(bufferId)?.let { totalBufferSize -= it }
    }

    fun resetPerFrameCounts() {
        drawCalls = 0
        calls = 0
        textureBindings = 0
        vertices = 0
        shaderSwitches = 0
    }

    internal fun statsString(): String {
        return """GL calls: $calls
           Draw calls: $drawCalls
           Vertices: $vertices
           Textures: $textureBindings
           Shaders: $shaderSwitches
           Buffers: ${bufferAllocations.size} with memory usage of ${(totalBufferSize.toDouble() / (1024.0 * 1024.0)).toString(1)}M
       """
    }

    override fun toString(): String {
        return """
           
           ***************** ENGINE STATS *****************
           ${statsString()}
           ***************** END ENGINE STATS *************
       """.trimIndent()
    }

}