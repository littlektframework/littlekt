package com.littlekt.graphics.particle

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticleSortTests {

    private fun sort(keys: FloatArray): IntArray {
        val size = keys.size
        var log = 0
        while ((1 shl log) < size) log++
        val sorted = keys.copyOf()
        val order = IntArray(size) { it }
        val steps = ParticleRenderer.sortSteps(log)
        for (step in 0 until log * (log + 1) / 2) {
            val k = steps[step * 2]
            val j = steps[step * 2 + 1]
            for (i in 0 until size) {
                val l = i xor j
                if (l <= i) continue
                val a = sorted[i]
                val b = sorted[l]
                val descending = (i and k) == 0
                if ((descending && a < b) || (!descending && a > b)) {
                    sorted[i] = b
                    sorted[l] = a
                    val swap = order[i]
                    order[i] = order[l]
                    order[l] = swap
                }
            }
        }
        return order
    }

    @Test
    fun test_steps_cover_every_bitonic_stage() {
        val steps = ParticleRenderer.sortSteps(3)
        assertEquals(listOf(2, 1, 4, 2, 4, 1, 8, 4, 8, 2, 8, 1), steps.toList())
    }

    @Test
    fun test_live_particles_sort_far_to_near_before_dead_ones() {
        val random = Random(7)
        val active = 1000
        val keys = FloatArray(1024) { if (it < active && it % 7 != 0) random.nextFloat() * 100f else -3.0e38f }
        val order = sort(keys)
        val live = (0 until active).count { it % 7 != 0 }
        for (i in 1 until live) {
            assertTrue(keys[order[i - 1]] >= keys[order[i]], "not far to near at $i")
        }
        assertEquals(keys.indices.toSet(), order.toSet())
        for (i in live until order.size) assertTrue(keys[order[i]] < -1e38f, "live particle after dead at $i")
    }
}
