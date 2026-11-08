package com.littlekt.math

import kotlin.test.Test
import kotlin.test.assertEquals

class QuaternionTests {

    @Test
    fun test_times_scalar() {
        val result = MutableQuaternion(1f, 2f, 3f, 4f) * 2f
        assertEquals(2f, result.x)
        assertEquals(4f, result.y)
        assertEquals(6f, result.z)
        assertEquals(8f, result.w)
    }
}
