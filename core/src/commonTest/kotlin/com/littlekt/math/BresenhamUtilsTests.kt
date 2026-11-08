package com.littlekt.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BresenhamUtilsTests {

    private fun ray(x0: Int, y0: Int, x1: Int, y1: Int): Set<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        assertTrue(
            castRay(x0, y0, x1, y1) { x, y ->
                visited += x to y
                true
            }
        )
        return visited
    }

    private fun thickRay(x0: Int, y0: Int, x1: Int, y1: Int): Set<Pair<Int, Int>> {
        val visited = mutableSetOf<Pair<Int, Int>>()
        assertTrue(
            castThickRay(x0, y0, x1, y1) { x, y ->
                visited += x to y
                true
            }
        )
        return visited
    }

    private fun assertConnectedLine(points: Set<Pair<Int, Int>>, x0: Int, y0: Int, x1: Int, y1: Int) {
        assertTrue((x0 to y0) in points)
        assertTrue((x1 to y1) in points)
        assertEquals(max(abs(x1 - x0), abs(y1 - y0)) + 1, points.size)
        val major = if (abs(y1 - y0) > abs(x1 - x0)) points.map { it.second } else points.map { it.first }
        assertEquals(points.size, major.toSet().size)
    }

    @Test
    fun test_horizontal_line() {
        assertEquals((0..4).map { it to 0 }.toSet(), ray(0, 0, 4, 0))
    }

    @Test
    fun test_vertical_line() {
        assertEquals((0..4).map { 2 to it }.toSet(), ray(2, 0, 2, 4))
    }

    @Test
    fun test_diagonal_line() {
        assertEquals((0..3).map { it to it }.toSet(), ray(0, 0, 3, 3))
        assertEquals((0..3).map { it to -it }.toSet(), ray(0, 0, 3, -3))
    }

    @Test
    fun test_shallow_line() {
        val points = ray(0, 0, 4, 1)
        assertEquals(setOf(0 to 0, 1 to 0, 2 to 0, 3 to 1, 4 to 1), points)
    }

    @Test
    fun test_steep_line() {
        val points = ray(0, 0, 1, 4)
        assertEquals(setOf(0 to 0, 0 to 1, 0 to 2, 1 to 3, 1 to 4), points)
    }

    @Test
    fun test_reversed_endpoints_cover_same_cells() {
        assertEquals(ray(0, 0, 7, 3), ray(7, 3, 0, 0))
        assertEquals(ray(1, -2, 4, 9), ray(4, 9, 1, -2))
        assertEquals(ray(0, 0, 4, 0), ray(4, 0, 0, 0))
    }

    @Test
    fun test_lines_are_connected_in_every_octant() {
        val ends = listOf(5 to 2, 2 to 5, -2 to 5, -5 to 2, -5 to -2, -2 to -5, 2 to -5, 5 to -2)
        for ((x, y) in ends) {
            assertConnectedLine(ray(0, 0, x, y), 0, 0, x, y)
        }
    }

    @Test
    fun test_single_point() {
        assertEquals(setOf(3 to 3), ray(3, 3, 3, 3))
    }

    @Test
    fun test_blocked_cell_stops_ray() {
        assertFalse(castRay(0, 0, 4, 0) { x, _ -> x != 2 })
        assertTrue(castRay(0, 0, 4, 0) { _, y -> y == 0 })
    }

    @Test
    fun test_blocked_endpoint_fails() {
        assertFalse(castRay(0, 0, 4, 0) { x, _ -> x != 4 })
        assertFalse(castRay(0, 0, 4, 0) { x, _ -> x != 0 })
    }

    @Test
    fun test_thick_ray_horizontal_only_visits_row() {
        assertEquals((0..4).map { it to 0 }.toSet(), thickRay(0, 0, 4, 0))
    }

    @Test
    fun test_thick_ray_vertical_only_visits_column() {
        assertEquals((0..4).map { 2 to it }.toSet(), thickRay(2, 0, 2, 4))
    }

    @Test
    fun test_thick_ray_diagonal_visits_neighbours() {
        assertEquals(setOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), thickRay(0, 0, 1, 1))
    }

    @Test
    fun test_thick_ray_contains_thin_ray() {
        val thin = ray(0, 0, 6, 2)
        val thick = thickRay(0, 0, 6, 2)
        assertTrue(thick.containsAll(thin))
        assertTrue(thickRay(0, 0, 2, 6).containsAll(ray(0, 0, 2, 6)))
    }

    @Test
    fun test_thick_ray_blocked_by_diagonal_corner() {
        assertTrue(castRay(0, 0, 1, 1) { x, y -> !(x == 1 && y == 0) })
        assertFalse(castThickRay(0, 0, 1, 1) { x, y -> !(x == 1 && y == 0) })
        assertFalse(castThickRay(0, 0, 1, 1) { x, y -> !(x == 0 && y == 1) })
    }

    @Test
    fun test_thick_ray_blocked_cell_on_shallow_row() {
        assertFalse(castThickRay(0, 0, 4, 0) { x, y -> !(x == 2 && y == 0) })
        assertTrue(castThickRay(0, 0, 4, 0) { _, y -> y == 0 })
    }
}
