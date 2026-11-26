package com.littlekt.graphics.util

import com.littlekt.util.datastructure.IntMap

/**
 * A least recently used cache of values, such as bind groups, by shader id & texture id. A value
 * is evicted when its texture is evicted with [evictTexture], when the view or sampler it was
 * created with changes, or when the cache grows over [maxSize]. Evicted values are passed to
 * [onEvict].
 *
 * @param maxSize the max number of values to cache
 * @param onEvict invoked with each value that is evicted
 * @author Colton Daily
 * @date 11/26/2026
 */
internal class TextureBindGroupCache<V : Any>(maxSize: Int, private val onEvict: (V) -> Unit) {
    private class Entry<V : Any> {
        var shaderId = 0
        var textureId = 0
        var value: V? = null
        var view: Any? = null
        var sampler: Any? = null
        var prev: Entry<V>? = null
        var next: Entry<V>? = null
    }

    private val byShader = IntMap<IntMap<Entry<V>>>()
    private val freeEntries = ArrayList<Entry<V>>()
    private var head: Entry<V>? = null
    private var tail: Entry<V>? = null

    /** The number of cached values. */
    var size: Int = 0
        private set

    /** The max number of cached values. Lowering it evicts the least recently used values. */
    var maxSize: Int = maxSize
        set(value) {
            require(value > 0) { "TextureBindGroupCache.maxSize must be greater than zero!" }
            field = value
            trim()
        }

    init {
        require(maxSize > 0) { "TextureBindGroupCache.maxSize must be greater than zero!" }
    }

    /**
     * @return the cached value of [shaderId] & [textureId], or `null` if there is none or if it
     *   was created with another [view] or [sampler], in which case it's evicted.
     */
    operator fun get(shaderId: Int, textureId: Int, view: Any, sampler: Any): V? {
        val entry = byShader[shaderId]?.get(textureId) ?: return null
        if (entry.view !== view || entry.sampler !== sampler) {
            remove(entry)
            return null
        }
        if (entry !== head) {
            unlink(entry)
            linkFirst(entry)
        }
        return entry.value
    }

    /** Caches [value] for [shaderId] & [textureId], created with [view] & [sampler]. */
    fun put(shaderId: Int, textureId: Int, view: Any, sampler: Any, value: V) {
        val textures = byShader[shaderId] ?: IntMap<Entry<V>>().also { byShader[shaderId] = it }
        textures[textureId]?.let { remove(it) }
        val entry = if (freeEntries.isEmpty()) Entry() else freeEntries.removeAt(freeEntries.lastIndex)
        entry.shaderId = shaderId
        entry.textureId = textureId
        entry.value = value
        entry.view = view
        entry.sampler = sampler
        textures[textureId] = entry
        linkFirst(entry)
        size++
        trim()
    }

    /** Evicts the values of every shader for [textureId]. */
    fun evictTexture(textureId: Int) {
        byShader.forEachValue { textures -> textures[textureId]?.let { remove(it) } }
    }

    /** Evicts every value. */
    fun clear() {
        while (true) remove(tail ?: return)
    }

    private fun trim() {
        while (size > maxSize) remove(tail ?: return)
    }

    private fun remove(entry: Entry<V>) {
        byShader[entry.shaderId]?.remove(entry.textureId)
        unlink(entry)
        size--
        val value = entry.value
        entry.value = null
        entry.view = null
        entry.sampler = null
        freeEntries += entry
        if (value != null) onEvict(value)
    }

    private fun linkFirst(entry: Entry<V>) {
        entry.prev = null
        entry.next = head
        head?.prev = entry
        head = entry
        if (tail == null) tail = entry
    }

    private fun unlink(entry: Entry<V>) {
        val prev = entry.prev
        val next = entry.next
        if (prev == null) head = next else prev.next = next
        if (next == null) tail = prev else next.prev = prev
        entry.prev = null
        entry.next = null
    }
}
