package com.littlekt.graphics.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextureBindGroupCacheTests {
    private val view = Any()
    private val sampler = Any()
    private val evicted = mutableListOf<String>()

    private fun cache(maxSize: Int = 8) = TextureBindGroupCache<String>(maxSize) { evicted += it }

    @Test
    fun test_get_returns_the_cached_value() {
        val cache = cache()
        cache.put(1, 10, view, sampler, "a")
        assertEquals("a", cache[1, 10, view, sampler])
        assertNull(cache[2, 10, view, sampler])
        assertNull(cache[1, 11, view, sampler])
    }

    @Test
    fun test_evict_texture_releases_every_shader_of_it() {
        val cache = cache()
        cache.put(1, 10, view, sampler, "a")
        cache.put(2, 10, view, sampler, "b")
        cache.put(1, 11, view, sampler, "c")
        cache.evictTexture(10)
        assertEquals(listOf("a", "b"), evicted)
        assertNull(cache[1, 10, view, sampler])
        assertNull(cache[2, 10, view, sampler])
        assertEquals("c", cache[1, 11, view, sampler])
        assertEquals(1, cache.size)
    }

    @Test
    fun test_reused_texture_id_with_a_new_view_or_sampler_is_evicted() {
        val cache = cache()
        cache.put(1, 10, view, sampler, "a")
        assertNull(cache[1, 10, Any(), sampler])
        assertEquals(listOf("a"), evicted)
        cache.put(1, 10, view, sampler, "b")
        assertNull(cache[1, 10, view, Any()])
        assertEquals(listOf("a", "b"), evicted)
        assertEquals(0, cache.size)
    }

    @Test
    fun test_put_replaces_and_evicts_the_previous_value() {
        val cache = cache()
        cache.put(1, 10, view, sampler, "a")
        cache.put(1, 10, view, sampler, "b")
        assertEquals(listOf("a"), evicted)
        assertEquals("b", cache[1, 10, view, sampler])
        assertEquals(1, cache.size)
    }

    @Test
    fun test_max_size_evicts_the_least_recently_used() {
        val cache = cache(maxSize = 2)
        cache.put(1, 1, view, sampler, "a")
        cache.put(1, 2, view, sampler, "b")
        cache[1, 1, view, sampler]
        cache.put(1, 3, view, sampler, "c")
        assertEquals(listOf("b"), evicted)
        assertEquals("a", cache[1, 1, view, sampler])
        assertEquals("c", cache[1, 3, view, sampler])
        assertEquals(2, cache.size)
    }

    @Test
    fun test_lowering_max_size_trims_the_cache() {
        val cache = cache()
        for (i in 0 until 5) cache.put(1, i, view, sampler, "$i")
        cache.maxSize = 2
        assertEquals(listOf("0", "1", "2"), evicted)
        assertEquals(2, cache.size)
        assertTrue(cache[1, 4, view, sampler] == "4")
    }

    @Test
    fun test_clear_evicts_everything() {
        val cache = cache()
        cache.put(1, 1, view, sampler, "a")
        cache.put(2, 1, view, sampler, "b")
        cache.clear()
        assertEquals(2, evicted.size)
        assertEquals(0, cache.size)
        cache.put(1, 1, view, sampler, "c")
        assertEquals("c", cache[1, 1, view, sampler])
    }

    @Test
    fun test_max_size_must_be_positive() {
        assertFailsWith<IllegalArgumentException> { cache(maxSize = 0) }
        assertFailsWith<IllegalArgumentException> { cache().maxSize = 0 }
    }
}
