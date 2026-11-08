package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Ray
import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BoundingBoxTests {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f) {
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
    }

    private fun assertNear(expected: Vec3f, actual: Vec3f, eps: Float = 1e-4f) {
        assertTrue(actual.isFuzzyEqual(expected, eps), "expected $expected but was $actual")
    }

    private fun box(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) =
        BoundingBox().set(minX, minY, minZ, maxX, maxY, maxZ)

    private fun ray(origin: Vec3f, direction: Vec3f) =
        Ray().apply {
            this.origin.set(origin)
            this.direction.set(direction)
        }

    @Test
    fun test_new_box_is_empty() {
        val aabb = BoundingBox()
        assertTrue(aabb.isEmpty)
        assertEquals("[empty]", aabb.toString())
    }

    @Test
    fun test_constructor_sets_min_max_size_center() {
        val aabb = BoundingBox(Vec3f(-1f, 0f, 2f), Vec3f(3f, 4f, 6f))
        assertFalse(aabb.isEmpty)
        assertNear(Vec3f(-1f, 0f, 2f), aabb.min)
        assertNear(Vec3f(3f, 4f, 6f), aabb.max)
        assertNear(Vec3f(4f, 4f, 4f), aabb.size)
        assertNear(Vec3f(1f, 2f, 4f), aabb.center)
    }

    @Test
    fun test_add_first_point_makes_degenerate_box() {
        val aabb = BoundingBox().add(Vec3f(1f, 2f, 3f))
        assertFalse(aabb.isEmpty)
        assertNear(Vec3f(1f, 2f, 3f), aabb.min)
        assertNear(Vec3f(1f, 2f, 3f), aabb.max)
        assertNear(Vec3f.ZERO, aabb.size)
        assertNear(Vec3f(1f, 2f, 3f), aabb.center)
    }

    @Test
    fun test_add_points_grows_box() {
        val aabb = BoundingBox()
        aabb.add(Vec3f(1f, -2f, 3f))
        aabb.add(Vec3f(-1f, 4f, 0f))
        aabb.add(Vec3f(0f, 0f, 5f))
        assertNear(Vec3f(-1f, -2f, 0f), aabb.min)
        assertNear(Vec3f(1f, 4f, 5f), aabb.max)
        assertNear(Vec3f(2f, 6f, 5f), aabb.size)
        assertNear(Vec3f(0f, 1f, 2.5f), aabb.center)
    }

    @Test
    fun test_add_point_list_and_range() {
        val points = listOf(Vec3f(0f, 0f, 0f), Vec3f(1f, 1f, 1f), Vec3f(5f, 5f, 5f), Vec3f(-5f, -5f, -5f))
        val all = BoundingBox().add(points)
        assertNear(Vec3f(-5f, -5f, -5f), all.min)
        assertNear(Vec3f(5f, 5f, 5f), all.max)

        val partial = BoundingBox().add(points, 0..1)
        assertNear(Vec3f(0f, 0f, 0f), partial.min)
        assertNear(Vec3f(1f, 1f, 1f), partial.max)
        assertNear(Vec3f(1f, 1f, 1f), partial.size)
    }

    @Test
    fun test_add_box() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f)
        aabb.add(box(2f, -1f, 0.5f, 3f, 0.5f, 0.75f))
        assertNear(Vec3f(0f, -1f, 0f), aabb.min)
        assertNear(Vec3f(3f, 1f, 1f), aabb.max)
    }

    @Test
    fun test_add_empty_box_is_noop() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f)
        aabb.add(BoundingBox())
        assertEquals(box(0f, 0f, 0f, 1f, 1f, 1f), aabb)
    }

    @Test
    fun test_add_box_to_empty_box() {
        val aabb = BoundingBox().add(box(1f, 2f, 3f, 4f, 5f, 6f))
        assertEquals(box(1f, 2f, 3f, 4f, 5f, 6f), aabb)
    }

    @Test
    fun test_clear_resets_to_empty() {
        val aabb = box(1f, 2f, 3f, 4f, 5f, 6f)
        aabb.clear()
        assertTrue(aabb.isEmpty)
        assertNear(Vec3f.ZERO, aabb.min)
        assertNear(Vec3f.ZERO, aabb.max)
        assertNear(Vec3f.ZERO, aabb.size)
        assertNear(Vec3f.ZERO, aabb.center)
        aabb.add(Vec3f(5f, 5f, 5f))
        assertNear(Vec3f(5f, 5f, 5f), aabb.min)
    }

    @Test
    fun test_expand() {
        val aabb = box(0f, 0f, 0f, 2f, 2f, 2f).expand(Vec3f(1f, 2f, 3f))
        assertNear(Vec3f(-1f, -2f, -3f), aabb.min)
        assertNear(Vec3f(3f, 4f, 5f), aabb.max)
        assertNear(Vec3f(4f, 6f, 8f), aabb.size)
        assertNear(Vec3f(1f, 1f, 1f), aabb.center)
    }

    @Test
    fun test_expand_empty_throws() {
        assertFailsWith<IllegalStateException> { BoundingBox().expand(Vec3f(1f, 1f, 1f)) }
        assertFailsWith<IllegalStateException> { BoundingBox().signedExpand(Vec3f(1f, 1f, 1f)) }
        assertFailsWith<IllegalStateException> { BoundingBox().translate(1f, 1f, 1f) }
    }

    @Test
    fun test_signed_expand() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f).signedExpand(Vec3f(2f, -3f, 0f))
        assertNear(Vec3f(0f, -3f, 0f), aabb.min)
        assertNear(Vec3f(3f, 1f, 1f), aabb.max)
        assertNear(Vec3f(3f, 4f, 1f), aabb.size)
    }

    @Test
    fun test_translate_moves_min_max_center() {
        val aabb = box(0f, 0f, 0f, 2f, 4f, 6f).translate(Vec3f(1f, -1f, 2f))
        assertNear(Vec3f(1f, -1f, 2f), aabb.min)
        assertNear(Vec3f(3f, 3f, 8f), aabb.max)
        assertNear(Vec3f(2f, 1f, 5f), aabb.center)
        assertNear(Vec3f(2f, 4f, 6f), aabb.size)
    }

    @Test
    fun test_set_copies_other() {
        val other = box(1f, 2f, 3f, 4f, 5f, 6f)
        val aabb = BoundingBox().set(other)
        assertEquals(other, aabb)
        assertNear(other.size, aabb.size)
        assertNear(other.center, aabb.center)
        assertTrue(BoundingBox().set(BoundingBox()).isEmpty)
    }

    @Test
    fun test_set_merged() {
        val a = box(0f, 0f, 0f, 1f, 1f, 1f)
        val b = box(-2f, 0.5f, 0.5f, 0.5f, 3f, 0.75f)
        val merged = BoundingBox().setMerged(a, b)
        assertNear(Vec3f(-2f, 0f, 0f), merged.min)
        assertNear(Vec3f(1f, 3f, 1f), merged.max)
        assertNear(Vec3f(-0.5f, 1.5f, 0.5f), merged.center)
    }

    @Test
    fun test_set_merged_into_self() {
        val a = box(0f, 0f, 0f, 1f, 1f, 1f)
        a.setMerged(a, box(2f, 2f, 2f, 3f, 3f, 3f))
        assertEquals(box(0f, 0f, 0f, 3f, 3f, 3f), a)
    }

    @Test
    fun test_contains_point() {
        val aabb = box(0f, 0f, 0f, 1f, 2f, 3f)
        assertTrue(Vec3f(0.5f, 1f, 1.5f) in aabb)
        assertTrue(Vec3f(0f, 0f, 0f) in aabb)
        assertTrue(Vec3f(1f, 2f, 3f) in aabb)
        assertFalse(Vec3f(1.1f, 1f, 1f) in aabb)
        assertFalse(Vec3f(0.5f, -0.1f, 1f) in aabb)
        assertFalse(Vec3f(0.5f, 1f, 3.1f) in aabb)
        assertTrue(aabb.contains(0.5f, 0.5f, 0.5f))
        assertFalse(aabb.contains(0.5f, 0.5f, -0.5f))
    }

    @Test
    fun test_contains_box() {
        val aabb = box(0f, 0f, 0f, 10f, 10f, 10f)
        assertTrue(box(1f, 1f, 1f, 2f, 2f, 2f) in aabb)
        assertTrue(box(0f, 0f, 0f, 10f, 10f, 10f) in aabb)
        assertFalse(box(5f, 5f, 5f, 11f, 6f, 6f) in aabb)
        assertFalse(box(-1f, -1f, -1f, 11f, 11f, 11f) in aabb)
    }

    @Test
    fun test_is_intersecting() {
        val aabb = box(0f, 0f, 0f, 2f, 2f, 2f)
        assertTrue(aabb.isIntersecting(box(1f, 1f, 1f, 3f, 3f, 3f)))
        assertTrue(aabb.isIntersecting(box(-1f, 0.5f, 0.5f, 3f, 1f, 1f)))
        assertTrue(aabb.isIntersecting(box(0.5f, 0.5f, 0.5f, 1f, 1f, 1f)))
        assertFalse(aabb.isIntersecting(box(3f, 0f, 0f, 4f, 2f, 2f)))
        assertFalse(aabb.isIntersecting(box(1f, 1f, 3f, 3f, 3f, 4f)))
        assertFalse(aabb.isIntersecting(box(1f, -3f, 1f, 3f, -1f, 3f)))
    }

    @Test
    fun test_is_intersecting_touching_faces_counts() {
        val aabb = box(0f, 0f, 0f, 2f, 2f, 2f)
        assertTrue(aabb.isIntersecting(box(2f, 0f, 0f, 3f, 2f, 2f)))
        assertTrue(box(2f, 0f, 0f, 3f, 2f, 2f).isIntersecting(aabb))
    }

    @Test
    fun test_clamp_to_bounds() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f)
        val point = MutableVec3f(2f, -1f, 0.5f)
        aabb.clampToBounds(point)
        assertNear(Vec3f(1f, 0f, 0.5f), point)
    }

    @Test
    fun test_point_distance() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertNear(0f, aabb.pointDistance(Vec3f(0.5f, 0.5f, 0.5f)))
        assertNear(2f, aabb.pointDistance(Vec3f(3f, 0.5f, 0.5f)))
        assertNear(2f, aabb.pointDistance(Vec3f(0.5f, -2f, 0.5f)))
        assertNear(25f, aabb.pointDistanceSqr(Vec3f(4f, 5f, 0.5f)))
        assertNear(3f, aabb.pointDistanceSqr(Vec3f(-1f, -1f, 2f)))
    }

    @Test
    fun test_hit_distance_from_outside() {
        val aabb = box(-1f, -1f, -1f, 1f, 1f, 1f)
        assertNear(16f, aabb.hitDistanceSqr(ray(Vec3f(0f, 0f, -5f), Vec3f(0f, 0f, 1f))))
        assertNear(16f, aabb.hitDistanceSqr(ray(Vec3f(5f, 0f, 0f), Vec3f(-1f, 0f, 0f))))
        assertNear(16f, aabb.hitDistanceSqr(ray(Vec3f(0f, 5f, 0f), Vec3f(0f, -3f, 0f))))
    }

    @Test
    fun test_hit_distance_diagonal() {
        val aabb = box(-1f, -1f, -1f, 1f, 1f, 1f)
        val r = ray(Vec3f(3f, 3f, 3f), Vec3f(-1f, -1f, -1f))
        assertNear(12f, aabb.hitDistanceSqr(r), 1e-3f)
    }

    @Test
    fun test_hit_distance_from_inside_is_zero() {
        val aabb = box(-1f, -1f, -1f, 1f, 1f, 1f)
        assertEquals(0f, aabb.hitDistanceSqr(ray(Vec3f(0.5f, 0f, 0f), Vec3f(1f, 0f, 0f))))
    }

    @Test
    fun test_hit_distance_miss() {
        val aabb = box(-1f, -1f, -1f, 1f, 1f, 1f)
        assertEquals(Float.MAX_VALUE, aabb.hitDistanceSqr(ray(Vec3f(0f, 3f, -5f), Vec3f(0f, 0f, 1f))))
        assertEquals(Float.MAX_VALUE, aabb.hitDistanceSqr(ray(Vec3f(3f, 0f, -5f), Vec3f(0f, 0f, 1f))))
        assertEquals(Float.MAX_VALUE, aabb.hitDistanceSqr(ray(Vec3f(5f, 5f, 0f), Vec3f(1f, -1f, 0f))))
    }

    @Test
    fun test_hit_distance_box_behind_ray() {
        val aabb = box(-1f, -1f, -1f, 1f, 1f, 1f)
        assertEquals(Float.MAX_VALUE, aabb.hitDistanceSqr(ray(Vec3f(0f, 0f, 5f), Vec3f(0f, 0f, 1f))))
    }

    @Test
    fun test_hit_distance_empty_box() {
        assertEquals(Float.MAX_VALUE, BoundingBox().hitDistanceSqr(ray(Vec3f.ZERO, Vec3f(1f, 0f, 0f))))
    }

    @Test
    fun test_batch_update_defers_size_and_center() {
        val aabb = box(0f, 0f, 0f, 1f, 1f, 1f)
        aabb.batchUpdate {
            add(Vec3f(4f, 4f, 4f))
            assertTrue(isBatchUpdate)
            assertNear(Vec3f(1f, 1f, 1f), size)
        }
        assertFalse(aabb.isBatchUpdate)
        assertNear(Vec3f(4f, 4f, 4f), aabb.size)
        assertNear(Vec3f(2f, 2f, 2f), aabb.center)
    }

    @Test
    fun test_equals_and_fuzzy_equal() {
        val a = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertEquals(a, box(0f, 0f, 0f, 1f, 1f, 1f))
        assertEquals(a.hashCode(), box(0f, 0f, 0f, 1f, 1f, 1f).hashCode())
        assertNotEquals(a, box(0f, 0f, 0f, 1f, 1f, 2f))
        assertNotEquals(BoundingBox(), box(0f, 0f, 0f, 0f, 0f, 0f))
        assertTrue(a.isFuzzyEqual(box(0f, 0f, 1e-7f, 1f, 1f, 1f)))
        assertFalse(a.isFuzzyEqual(box(0f, 0f, 0.1f, 1f, 1f, 1f)))
    }
}
