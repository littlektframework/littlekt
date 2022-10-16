package com.lehaine.littlekt

import com.lehaine.littlekt.util.toString

/**
 * OpenGL related engine stats.
 * @author Colton Daily
 * @date 11/24/2021
 */
class EngineStats {
    /**
     * Each buffer and it's size
     */
    val bufferAllocations = mutableMapOf<Long, Int>()

    /**
     * THe total size of all buffers
     */
    var totalBufferSize = 0L
        private set

    /**
     * The total times a shader has been bound / switched.
     */
    var shaderSwitches = 0
        internal set

    /**
     * The total vertices rendered.
     */
    var vertices = 0
        internal set

    /**
     * The total textures bound.
     */
    var textureBindings = 0
        internal set

    /**
     * The total OpenGL draw calls invoked.
     */
    var drawCalls = 0
        internal set

    /**
     * The total OpenGL calls invoked.
     */
    var calls = 0
        internal set

    /**
     * Inform the engine stats that a new buffer has been allocated and the size of the buffer.
     */
    fun bufferAllocated(bufferId: Long, size: Int) {
        bufferAllocations.put(bufferId, size)?.let { totalBufferSize -= it }
        totalBufferSize += size
    }

    /**
     * Inform the engine stats that a buffer has been deleted.
     */
    fun bufferDeleted(bufferId: Long) {
        bufferAllocations.remove(bufferId)?.let { totalBufferSize -= it }
    }

    /**
     * Reset all counts to 0.
     */
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