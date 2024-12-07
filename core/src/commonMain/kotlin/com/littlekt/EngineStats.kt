package com.littlekt

import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import com.littlekt.util.toString

/**
 * Graphic related engine stats.
 *
 * @author Colton Daily
 * @date 11/24/2021
 */
object EngineStats {

    private val _bufferAllocations = mutableMapOf<Long, BufferResourceInfo>()
    /** Each buffer and it's size */
    val bufferAllocations: Map<Long, BufferResourceInfo>
        get() = _bufferAllocations

    private val _textureAllocations = mutableMapOf<Long, TextureResourceInfo>()
    /** Each Texture and it's size */
    val textureAllocations: Map<Long, TextureResourceInfo>
        get() = _textureAllocations

    /** The total size of all buffers */
    var totalBufferSize = 0L
        private set

    /** The total size of all textures */
    var totalTextureSize = 0L
        private set

    /** The total triangles rendered. */
    var triangles = 0
        internal set

    /** The total draw calls invoked. */
    var drawCalls = 0
        internal set

    /** There total number of `setPipeline` calls. */
    var setPipelineCalls = 0
        internal set

    /** There total number of `setBindGroup` calls. */
    var setBindGroupCalls = 0
        internal set

    /** There total number of `setBuffer` calls. */
    var setBufferCalls = 0
        internal set

    private val extras = mutableMapOf<String, Int>()

    /** Inform the engine stats that a new buffer has been allocated and the size of the buffer. */
    fun bufferAllocated(info: BufferResourceInfo) {
        _bufferAllocations.put(info.id, info)?.let { totalBufferSize -= it.size }
        totalBufferSize += info.size
    }

    /** Inform the engine stats that a buffer has been deleted. */
    fun bufferDeleted(id: Long) {
        _bufferAllocations.remove(id)?.let { totalBufferSize -= it.size }
    }

    /** Inform the engine stats that a new texture has been allocated and the size of the buffer. */
    fun textureAllocated(info: TextureResourceInfo) {
        _textureAllocations.put(info.id, info)?.let { totalTextureSize -= it.size }
        totalTextureSize += info.size
    }

    /** Inform the engine stats that a texture has been deleted. */
    fun textureDeleted(id: Long) {
        _textureAllocations.remove(id)?.let { totalTextureSize -= it.size }
    }

    /**
     * Track extra data under [EngineStats].
     *
     * @param key the name of the data
     * @param diff the delta to add to the existing, if any, value.
     */
    fun extra(key: String, diff: Int) {
        if (extras.contains(key)) {
            extras[key] = extras.getValue(key) + diff
        } else {
            extras[key] = diff
        }
    }

    /** Reset all counts to 0. */
    fun resetPerFrameCounts() {
        drawCalls = 0
        triangles = 0
        setPipelineCalls = 0
        setBindGroupCalls = 0
        setBufferCalls = 0
        extras.clear()
    }

    internal fun clear() {
        resetPerFrameCounts()
        _textureAllocations.clear()
        _bufferAllocations.clear()
    }

    internal fun statsString(): String {
        val stats = buildString {
            appendLine("Draw calls: $drawCalls")
            appendLine("setPipeline calls: $setPipelineCalls")
            appendLine("setBindGroup calls: $setPipelineCalls")
            appendLine("setBuffer calls: $setBufferCalls")
            appendLine("~Triangles: $triangles")
            appendLine(
                "Buffers: ${bufferAllocations.size} with memory usage of ${(totalBufferSize.toDouble() / (1024.0 * 1024.0)).toString(1)}M"
            )
            appendLine(
                "Textures: ${textureAllocations.size} with memory usage of ${(totalTextureSize.toDouble() / (1024.0 * 1024.0)).toString(1)}M"
            )
            if (extras.isNotEmpty()) {
                append(
                    extras.map { "${it.key}: ${it.value} [Extra]" }.joinToString(separator = "\n")
                )
            }
        }
        return stats
    }

    override fun toString(): String {
        return buildString {
            appendLine("***************** ENGINE STATS *****************")
            appendLine(statsString())
            append("***************** END ENGINE STATS *************")
        }
    }
}
