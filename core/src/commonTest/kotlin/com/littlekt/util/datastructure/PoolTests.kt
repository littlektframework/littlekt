package com.littlekt.util.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PoolTests {

    private class Item(val id: Int) {
        var value = 0
    }

    @Test
    fun test_alloc_generates_with_incrementing_ids() {
        val pool = Pool { Item(it) }
        assertEquals(0, pool.alloc().id)
        assertEquals(1, pool.alloc().id)
        assertEquals(2, pool.totalAllocatedItems)
        assertEquals(2, pool.totalItemsInUse)
    }

    @Test
    fun test_free_then_alloc_reuses_and_resets() {
        val pool = Pool(reset = { it.value = 0 }) { Item(it) }
        val item = pool.alloc()
        item.value = 5
        pool.free(item)
        assertEquals(1, pool.itemsInPool)
        val again = pool.alloc()
        assertSame(item, again)
        assertEquals(0, again.value)
        assertEquals(1, pool.totalAllocatedItems)
    }

    @Test
    fun test_preallocate() {
        val pool = Pool(preallocate = 3) { Item(it) }
        assertEquals(3, pool.itemsInPool)
        assertEquals(3, pool.totalAllocatedItems)
        assertEquals(0, pool.totalItemsInUse)
        pool.alloc()
        assertEquals(2, pool.itemsInPool)
        assertEquals(3, pool.totalAllocatedItems)
    }

    @Test
    fun test_free_is_lifo() {
        val pool = Pool { Item(it) }
        val a = pool.alloc()
        val b = pool.alloc()
        pool.free(a, b)
        assertSame(b, pool.alloc())
        assertSame(a, pool.alloc())
    }

    @Test
    fun test_free_list_and_iterable() {
        val pool = Pool { Item(it) }
        val items = listOf(pool.alloc(), pool.alloc())
        pool.free(items)
        pool.free(setOf(pool.alloc()))
        assertEquals(2, pool.itemsInPool)
        assertEquals(0, pool.totalItemsInUse)
    }

    @Test
    fun test_alloc_callback() {
        val pool = Pool { Item(it) }
        assertEquals(0, pool { it.id })
        assertEquals(1, pool.allocThis { id })
    }

    @Test
    fun test_alloc_multiple_reuses_temp_list() {
        val pool = Pool { Item(it) }
        val temp = mutableListOf(Item(99))
        val size = pool.allocMultiple(3, temp) { it.size }
        assertEquals(3, size)
        assertEquals(listOf(0, 1, 2), temp.map { it.id })
    }

    @Test
    fun test_pool_builder_passes_pool() {
        var seen: Pool<Item>? = null
        val created = pool(preallocate = 1) {
            seen = this
            Item(it)
        }
        assertSame(created, seen)
        assertEquals(1, created.itemsInPool)
    }

    @Test
    fun test_alloc_without_generator_throws() {
        val pool = Pool<Item>()
        assertFailsWith<IllegalStateException> { pool.alloc() }
    }

    @Test
    fun test_alloc_after_drain_creates_new() {
        val pool = Pool(preallocate = 1) { Item(it) }
        val a = pool.alloc()
        val b = pool.alloc()
        assertNotSame(a, b)
        assertEquals(1, b.id)
    }
}
