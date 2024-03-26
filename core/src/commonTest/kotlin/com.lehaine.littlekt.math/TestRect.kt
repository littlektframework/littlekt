package com.lehaine.littlekt.math

import kotlin.test.Test
import kotlin.test.expect

class TestRect {
    @Test
    fun TestIntersects() {
        expect(true) {
            Rect(0f, 0f, 10f, 10f).intersects(Rect(1f, 1f, 10f, 10f))
        }
    }

    @Test
    fun TestIntersectsListAll() {
        expect(true) {
            Rect(0f, 0f, 10f, 10f).intersectsListAll(listOf(
                Rect(1f, 1f, 10f, 10f),
                Rect(2f, 2f, 10f, 10f)
            ))
        }
    }

    @Test
    fun TestIntersectsListAny() {
        expect(true) {
            Rect(0f, 0f, 10f, 10f).intersectsListAny(listOf(
                Rect(1f, 1f, 10f, 10f),
                Rect(11f, 11f, 10f, 10f)
            ))
        }
    }

    @Test
    fun TestGetIntersectingRects() {
        expect(1) {
            Rect(0f, 0f, 10f, 10f).getIntersectingRects(listOf(
                Rect(1f, 1f, 10f, 10f),
                Rect(11f, 11f, 10f, 10f) // This does not intersect
            )).size
        }
        expect(2) {
            Rect(0f, 0f, 10f, 10f).getIntersectingRects(listOf(
                Rect(1f, 1f, 10f, 10f),
                Rect(2f, 2f, 10f, 10f)
            )).size
        }
    }
}