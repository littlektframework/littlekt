package com.littlekt.math.spatial

import com.littlekt.math.Ray
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpatialHashTests {

    private fun ray(ox: Float, oy: Float, oz: Float, dx: Float, dy: Float, dz: Float) =
        Ray().apply {
            origin.set(ox, oy, oz)
            direction.set(dx, dy, dz).norm()
        }

    private fun SpatialHash<String>.box(item: String, x: Float, y: Float, z: Float, half: Float = 0.5f) =
        put(item, x - half, y - half, z - half, x + half, y + half, z + half)

    @Test
    fun test_query_returns_overlapping_items_once() {
        val hash = SpatialHash<String>(1f)
        hash.put("big", 0f, 0f, 0f, 5f, 5f, 0f)
        hash.putRect("small", 10f, 10f, 1f, 1f)
        val result = hash.queryRect(-1f, -1f, 8f, 8f, mutableListOf())
        assertEquals(listOf("big"), result)
    }

    @Test
    fun test_query_filters_by_bounds_not_cells() {
        val hash = SpatialHash<String>(10f)
        hash.putRect("a", 1f, 1f, 1f, 1f)
        hash.putRect("b", 8f, 8f, 1f, 1f)
        assertEquals(listOf("a"), hash.queryRect(0f, 0f, 3f, 3f, mutableListOf()))
    }

    @Test
    fun test_put_moves_and_remove() {
        val hash = SpatialHash<String>(1f)
        hash.putPoint("a", 0.5f, 0.5f)
        hash.putPoint("a", 20.5f, 20.5f)
        assertEquals(1, hash.size)
        assertTrue(hash.queryRect(0f, 0f, 1f, 1f, mutableListOf()).isEmpty())
        assertEquals(listOf("a"), hash.queryRect(20f, 20f, 1f, 1f, mutableListOf()))
        assertTrue(hash.remove("a"))
        assertFalse("a" in hash)
        assertTrue(hash.queryRect(20f, 20f, 1f, 1f, mutableListOf()).isEmpty())
        assertFalse(hash.remove("a"))
    }

    @Test
    fun test_query_circle_uses_distance_to_bounds() {
        val hash = SpatialHash<String>(2f)
        hash.putRect("near", 3f, 0f, 1f, 1f)
        hash.putRect("corner", 2.5f, 2.5f, 1f, 1f)
        val result = hash.queryCircle(0f, 0f, 3f, mutableListOf())
        assertEquals(listOf("near"), result)
    }

    @Test
    fun test_raycast_finds_closest() {
        val hash = SpatialHash<String>(2f)
        hash.box("far", 10f, 0f, 0f)
        hash.box("near", 4f, 0f, 0f)
        hash.box("off", 6f, 5f, 0f)
        assertEquals("near", hash.raycast(ray(0f, 0f, 0f, 1f, 0f, 0f)))
        assertTrue(abs(hash.lastHitDistance - 3.5f) < 1e-4f)
        assertEquals("far", hash.raycast(ray(20f, 0f, 0f, -1f, 0f, 0f)))
        assertNull(hash.raycast(ray(0f, 0f, 0f, 0f, 0f, 1f)))
        assertEquals(Ray.NO_HIT, hash.lastHitDistance)
    }

    @Test
    fun test_raycast_respects_max_distance_and_test() {
        val hash = SpatialHash<String>(2f)
        hash.box("near", 4f, 0f, 0f)
        hash.box("far", 10f, 0f, 0f)
        val r = ray(0f, 0f, 0f, 1f, 0f, 0f)
        assertNull(hash.raycast(r, maxDistance = 2f))
        assertEquals("far", hash.raycast(r) { item, _, d -> if (item == "near") Ray.NO_HIT else d })
    }

    @Test
    fun test_raycast_from_outside_and_diagonal() {
        val hash = SpatialHash<String>(1f)
        for (i in 0 until 10) hash.box("b$i", i * 3f, i * 3f, i * 3f, 0.4f)
        assertEquals("b9", hash.raycast(ray(100f, 100f, 100f, -1f, -1f, -1f)))
        assertEquals("b0", hash.raycast(ray(-50f, -50f, -50f, 1f, 1f, 1f)))
    }

    @Test
    fun test_query_ray_collects_hits() {
        val hash = SpatialHash<String>(2f)
        hash.box("a", 4f, 0f, 0f)
        hash.box("b", 10f, 0f, 0f)
        hash.box("c", 10f, 5f, 0f)
        val result = hash.queryRay(ray(0f, 0f, 0f, 1f, 0f, 0f), 100f, mutableListOf())
        assertEquals(setOf("a", "b"), result.toSet())
    }

    @Test
    fun test_clear_and_reuse() {
        val hash = SpatialHash<String>(1f)
        repeat(50) { hash.putPoint("p$it", it.toFloat(), 0f) }
        hash.clear()
        assertEquals(0, hash.size)
        assertTrue(hash.queryRect(-100f, -100f, 200f, 200f, mutableListOf()).isEmpty())
        hash.putPoint("again", 3f, 3f)
        assertEquals(listOf("again"), hash.queryCircle(3f, 3f, 0.5f, mutableListOf()))
    }

    @Test
    fun test_negative_coordinates_and_vertical_cell_size() {
        val hash = SpatialHash<String>(4f, cellSizeY = 1000f)
        hash.box("a", -10f, 3f, -10f)
        hash.box("b", -10f, -3f, 10f)
        assertEquals(listOf("a"), hash.queryRadius(-10f, 3f, -10f, 1f, mutableListOf()))
        assertEquals("b", hash.raycast(ray(-10f, 50f, 10f, 0f, -1f, 0f)))
    }
}
