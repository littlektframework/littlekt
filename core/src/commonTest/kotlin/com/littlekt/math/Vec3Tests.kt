package com.littlekt.math

import kotlin.test.Test
import kotlin.test.assertEquals

class Vec3Tests {

    @Test
    fun test_cross() {
        assertEquals(Vec3f(0f, 0f, 1f), Vec3f.X_AXIS.cross(Vec3f.Y_AXIS, MutableVec3f()))
    }

    @Test
    fun test_cross_in_place() {
        assertEquals(Vec3f(0f, 0f, 1f), MutableVec3f(1f, 0f, 0f).cross(Vec3f.Y_AXIS))
        assertEquals(Vec3f(-3f, 6f, -3f), MutableVec3f(1f, 2f, 3f).cross(Vec3f(4f, 5f, 6f)))
    }

    @Test
    fun test_cross_into_other() {
        val other = MutableVec3f(4f, 5f, 6f)
        Vec3f(1f, 2f, 3f).cross(other, other)
        assertEquals(Vec3f(-3f, 6f, -3f), other)
    }
}
