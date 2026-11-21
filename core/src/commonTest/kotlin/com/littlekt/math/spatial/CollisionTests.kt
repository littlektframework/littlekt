package com.littlekt.math.spatial

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionTests {

    private val hit = SweepHit()

    private fun assertNear(expected: Float, actual: Float, eps: Float = 1e-4f, message: String = "") {
        assertTrue(abs(expected - actual) <= eps, "$message expected $expected but was $actual")
    }

    private fun assertVec(x: Float, y: Float, z: Float, actual: Vec3f, eps: Float = 1e-4f) {
        assertNear(x, actual.x, eps, "x")
        assertNear(y, actual.y, eps, "y")
        assertNear(z, actual.z, eps, "z")
    }

    private fun box(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) =
        BoundingBox().set(minX, minY, minZ, maxX, maxY, maxZ)

    @Test
    fun test_sphere_overlaps() {
        val box = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.overlapSphereBox(Vec3f(1.5f, 0.5f, 0.5f), 0.6f, box))
        assertFalse(Collision.overlapSphereBox(Vec3f(1.5f, 1.5f, 1.5f), 0.8f, box))
        assertTrue(Collision.overlapSphereSphere(Vec3f(0f), 1f, Vec3f(1.9f, 0f, 0f), 1f))
        assertFalse(Collision.overlapSphereSphere(Vec3f(0f), 1f, Vec3f(2.1f, 0f, 0f), 1f))
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(2f, 0f, 0f)
        val c = Vec3f(0f, 0f, 2f)
        assertTrue(Collision.overlapSphereTriangle(Vec3f(0.5f, 0.4f, 0.5f), 0.5f, a, b, c))
        assertFalse(Collision.overlapSphereTriangle(Vec3f(2f, 0f, 2f), 0.5f, a, b, c))
    }

    @Test
    fun test_capsule_overlaps() {
        val capsule = Capsule(Vec3f(-2f, 0.5f, 0.5f), Vec3f(3f, 0.5f, 0.5f), 0.1f)
        assertTrue(Collision.overlapCapsuleBox(capsule, box(0f, 0f, 0f, 1f, 1f, 1f)))
        val other = Capsule(Vec3f(0f, 1.2f, -1f), Vec3f(0f, 1.2f, 1f), 0.55f)
        assertFalse(Collision.overlapCapsuleCapsule(capsule, other))
        other.radius = 0.65f
        assertTrue(Collision.overlapCapsuleCapsule(capsule, other))
        assertTrue(Collision.overlapSphereCapsule(Vec3f(1f, 1f, 0.5f), 0.45f, capsule))
    }

    @Test
    fun test_sphere_sweep_hits_box_face() {
        val box = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.sweepSphereBox(Vec3f(-2f, 0.5f, 0.5f), 0.5f, Vec3f(4f, 0f, 0f), box, hit))
        assertNear(0.375f, hit.time)
        assertVec(-1f, 0f, 0f, hit.normal)
        assertVec(0f, 0.5f, 0.5f, hit.point)
    }

    @Test
    fun test_sphere_sweep_hits_box_edge_and_misses_corner_gap() {
        val box = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.sweepSphereBox(Vec3f(-1f, 1.3f, 0.5f), 0.5f, Vec3f(2f, 0f, 0f), box, hit))
        val x = -sqrt(0.25f - 0.09f)
        assertNear((x + 1f) / 2f, hit.time)
        assertVec(0f, 1f, 0.5f, hit.point)
        assertFalse(Collision.sweepSphereBox(Vec3f(-1f, 1.45f, 1.45f), 0.6f, Vec3f(2f, 0f, 0f), box, hit))
    }

    @Test
    fun test_sphere_sweep_triangle_face_and_edge() {
        val a = Vec3f(0f, 0f, 0f)
        val b = Vec3f(2f, 0f, 0f)
        val c = Vec3f(0f, 0f, 2f)
        assertTrue(Collision.sweepSphereTriangle(Vec3f(0.5f, 2f, 0.5f), 0.5f, Vec3f(0f, -3f, 0f), a, b, c, hit))
        assertNear(0.5f, hit.time)
        assertVec(0f, 1f, 0f, hit.normal)
        assertTrue(Collision.sweepSphereTriangle(Vec3f(0.5f, -2f, 0.5f), 0.5f, Vec3f(0f, 3f, 0f), a, b, c, hit))
        assertVec(0f, -1f, 0f, hit.normal)
        assertTrue(Collision.sweepSphereTriangle(Vec3f(1f, 0f, -2f), 0.5f, Vec3f(0f, 0f, 4f), a, b, c, hit))
        assertNear(1.5f / 4f, hit.time)
        assertVec(1f, 0f, 0f, hit.point)
    }

    @Test
    fun test_sweeps_report_initial_overlap() {
        val box = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.sweepSphereBox(Vec3f(0.5f, 1.2f, 0.5f), 0.5f, Vec3f(5f, 0f, 0f), box, hit))
        assertEquals(0f, hit.time)
        assertVec(0f, 1f, 0f, hit.normal)
    }

    @Test
    fun test_capsule_sweep_lands_on_box_with_its_middle() {
        val capsule = Capsule(Vec3f(-3f, 2f, 0.5f), Vec3f(4f, 2f, 0.5f), 0.5f)
        val box = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.sweepCapsuleBox(capsule, Vec3f(0f, -2f, 0f), box, hit))
        assertNear(0.25f, hit.time)
        assertVec(0f, 1f, 0f, hit.normal)
        assertNear(1f, hit.point.y)
    }

    @Test
    fun test_box_sweep() {
        val moving = box(-3f, 0f, 0f, -2f, 1f, 1f)
        val target = box(0f, 0f, 0f, 1f, 1f, 1f)
        assertTrue(Collision.sweepBoxBox(moving, Vec3f(4f, 0f, 0f), target, hit))
        assertNear(0.5f, hit.time)
        assertVec(-1f, 0f, 0f, hit.normal)
        assertFalse(Collision.sweepBoxBox(moving, Vec3f(1f, 0f, 0f), target, hit))
    }

    @Test
    fun test_capsule_bounds() {
        val capsule = Capsule().setUpright(1f, 0f, 2f, 2f, 0.5f)
        assertVec(1f, 0.5f, 2f, capsule.a)
        assertVec(1f, 1.5f, 2f, capsule.b)
        val bounds = capsule.sweepBounds(Vec3f(2f, -1f, 0f), BoundingBox())
        assertVec(0.5f, -1f, 1.5f, bounds.min)
        assertVec(3.5f, 2f, 2.5f, bounds.max)
    }

    private fun boxDistance(p: Vec3f, box: BoundingBox): Float {
        val dx = max(max(box.min.x - p.x, 0f), p.x - box.max.x)
        val dy = max(max(box.min.y - p.y, 0f), p.y - box.max.y)
        val dz = max(max(box.min.z - p.z, 0f), p.z - box.max.z)
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun segmentDistance(a: Vec3f, b: Vec3f, distance: (Vec3f) -> Float): Float {
        val p = MutableVec3f()
        var best = Float.MAX_VALUE
        for (i in 0..SEGMENT_SAMPLES) {
            val s = i / SEGMENT_SAMPLES.toFloat()
            p.set(a.x + (b.x - a.x) * s, a.y + (b.y - a.y) * s, a.z + (b.z - a.z) * s)
            best = minOf(best, distance(p))
        }
        return best
    }

    private fun bruteTime(overlaps: (Float) -> Boolean): Float {
        for (i in 0..STEPS) {
            val t = i / STEPS.toFloat()
            if (overlaps(t)) return t
        }
        return -1f
    }

    private fun Random.vec(scale: Float) =
        Vec3f((nextFloat() * 2f - 1f) * scale, (nextFloat() * 2f - 1f) * scale, (nextFloat() * 2f - 1f) * scale)

    private fun moved(p: Vec3f, d: Vec3f, t: Float) = Vec3f(p.x + d.x * t, p.y + d.y * t, p.z + d.z * t)

    private fun checkAgainstBrute(name: String, analytic: Float, brute: Float, tolerance: Float) {
        if (brute < 0f) {
            assertTrue(analytic < 0f || analytic >= 1f - tolerance, "$name: analytic $analytic but brute missed")
        } else {
            assertTrue(analytic >= 0f, "$name: analytic missed but brute hit at $brute")
            assertTrue(abs(analytic - brute) <= tolerance, "$name: analytic $analytic brute $brute")
        }
    }

    @Test
    fun test_sphere_sweeps_match_brute_force() {
        val random = Random(7)
        val box = box(-1f, -0.5f, -1.5f, 1f, 0.5f, 1.5f)
        val a = Vec3f(-1.5f, -0.3f, -1f)
        val b = Vec3f(1.5f, 0.4f, -0.8f)
        val c = Vec3f(0.2f, 0.1f, 1.7f)
        val closest = MutableVec3f()
        repeat(400) { i ->
            val start = random.vec(4f)
            val delta = random.vec(6f)
            val r = 0.1f + random.nextFloat()
            val boxT = if (Collision.sweepSphereBox(start, r, delta, box, hit)) hit.time else -1f
            val boxBrute = bruteTime { boxDistance(moved(start, delta, it), box) <= r }
            checkAgainstBrute("box $i", boxT, boxBrute, 2f / STEPS)
            val triT = if (Collision.sweepSphereTriangle(start, r, delta, a, b, c, hit)) hit.time else -1f
            val triBrute = bruteTime {
                val p = moved(start, delta, it)
                Collision.closestPointOnTriangle(p, a, b, c, closest).distance(p) <= r
            }
            checkAgainstBrute("triangle $i", triT, triBrute, 2f / STEPS)
        }
    }

    @Test
    fun test_capsule_sweeps_match_brute_force() {
        val random = Random(11)
        val box = box(-1f, -0.5f, -1.5f, 1f, 0.5f, 1.5f)
        val a = Vec3f(-1.5f, -0.3f, -1f)
        val b = Vec3f(1.5f, 0.4f, -0.8f)
        val c = Vec3f(0.2f, 0.1f, 1.7f)
        val target = Capsule(Vec3f(-0.5f, 1f, 0.3f), Vec3f(0.8f, -0.6f, -0.4f), 0.3f)
        val closest = MutableVec3f()
        repeat(150) { i ->
            val start = random.vec(4f)
            val capsule = Capsule(start, Vec3f(start.x + random.nextFloat() * 2f - 1f, start.y + 1.5f, start.z), 0.1f + random.nextFloat() * 0.6f)
            val delta = random.vec(6f)
            val r = capsule.radius
            val tolerance = 2f / STEPS + 0.02f
            val boxT = if (Collision.sweepCapsuleBox(capsule, delta, box, hit)) hit.time else -1f
            val boxBrute = bruteTime { t ->
                segmentDistance(moved(capsule.a, delta, t), moved(capsule.b, delta, t)) { boxDistance(it, box) } <= r
            }
            checkAgainstBrute("capsule box $i", boxT, boxBrute, tolerance)
            val triT = if (Collision.sweepCapsuleTriangle(capsule, delta, a, b, c, hit)) hit.time else -1f
            val triBrute = bruteTime { t ->
                segmentDistance(moved(capsule.a, delta, t), moved(capsule.b, delta, t)) {
                    Collision.closestPointOnTriangle(it, a, b, c, closest).distance(it)
                } <= r
            }
            checkAgainstBrute("capsule triangle $i", triT, triBrute, tolerance)
            val capT = if (Collision.sweepCapsuleCapsule(capsule, delta, target, hit)) hit.time else -1f
            val rr = r + target.radius
            val capBrute = bruteTime { t ->
                Collision.segmentDistanceSqr(moved(capsule.a, delta, t), moved(capsule.b, delta, t), target.a, target.b) <= rr * rr
            }
            checkAgainstBrute("capsule capsule $i", capT, capBrute, 2f / STEPS)
        }
    }

    @Test
    fun test_sweep_normals_face_the_mover() {
        val random = Random(3)
        val box = box(-1f, -1f, -1f, 1f, 1f, 1f)
        repeat(200) {
            val start = random.vec(5f)
            val delta = random.vec(8f)
            if (Collision.sweepSphereBox(start, 0.4f, delta, box, hit) && hit.time > 0f) {
                val center = moved(start, delta, hit.time)
                assertNear(1f, hit.normal.length(), 1e-3f)
                assertNear(0.4f, center.distance(hit.point), 1e-3f)
                assertTrue(hit.normal.dot(delta) <= 1e-4f)
            }
        }
    }

    private companion object {
        const val STEPS = 2000
        const val SEGMENT_SAMPLES = 200
    }
}
