package com.lehaine.littlekt.math

import kotlin.math.*
import kotlin.random.Random

/**
 * @author Colton Daily
 * @date 11/23/2021
 */

const val DEG_2_RAD = PI / 180.0
const val RAD_2_DEG = 180.0 / PI

const val FUZZY_EQ_F = 1e-5f
const val FUZZY_EQ_D = 1e-10

/**
 * The difference between 1 and the smallest floating point number of type float that is greater than 1.
 */
const val FLT_EPSILON = 1.19209290e-7f

/**
 * Square-root of 0.5f
 */
const val SQRT_1_2 = 0.707106781f

inline fun Float.toDeg() = this * RAD_2_DEG.toFloat()
inline fun Float.toRad() = this * DEG_2_RAD.toFloat()
inline fun Double.toDeg() = this * RAD_2_DEG
inline fun Double.toRad() = this * DEG_2_RAD

inline fun isFuzzyEqual(a: Float, b: Float, eps: Float = FUZZY_EQ_F) = (a - b).isFuzzyZero(eps)
inline fun isFuzzyEqual(a: Double, b: Double, eps: Double = FUZZY_EQ_D) = (a - b).isFuzzyZero(eps)

inline fun Float.isFuzzyZero(eps: Float = FUZZY_EQ_F) = abs(this) <= eps
inline fun Double.isFuzzyZero(eps: Double = FUZZY_EQ_D) = abs(this) <= eps

inline fun Int.clamp(min: Int, max: Int): Int = when {
    this < min -> min
    this > max -> max
    else -> this
}

inline fun Float.clamp(min: Float = 0f, max: Float = 1f): Float = when {
    this < min -> min
    this > max -> max
    else -> this
}

inline fun Double.clamp(min: Double = 0.0, max: Double = 1.0): Double = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun Int.wrap(low: Int, high: Int): Int {
    val r = high - low
    var t = (this - low) % r
    if (t < 0) {
        t += r
    }
    return t + low
}

fun Float.wrap(low: Float, high: Float): Float {
    val r = high - low
    var t = (this - low) % r
    if (t < 0) {
        t += r
    }
    return t + low
}

fun Double.wrap(low: Double, high: Double): Double {
    val r = high - low
    var t = (this - low) % r
    if (t < 0) {
        t += r
    }
    return t + low
}

fun getNumMipLevels(texWidth: Int, texHeight: Int): Int {
    return floor(log2(max(texWidth, texHeight).toDouble())).toInt() + 1
}

fun smoothStep(low: Float, high: Float, x: Float): Float {
    val nx = ((x - low) / (high - low)).clamp()
    return nx * nx * (3 - 2 * nx)
}

fun triArea(va: Vec3f, vb: Vec3f, vc: Vec3f): Float {
    val xAB = vb.x - va.x
    val yAB = vb.y - va.y
    val zAB = vb.z - va.z
    val xAC = vc.x - va.x
    val yAC = vc.y - va.y
    val zAC = vc.z - va.z
    val abSqr = xAB * xAB + yAB * yAB + zAB * zAB
    val acSqr = xAC * xAC + yAC * yAC + zAC * zAC
    val abcSqr = xAB * xAC + yAB * yAC + zAB * zAC
    return 0.5f * sqrt(abSqr * acSqr - abcSqr * abcSqr)
}

fun triAspectRatio(va: Vec3f, vb: Vec3f, vc: Vec3f): Float {
    val a = va.distance(vb)
    val b = vb.distance(vc)
    val c = vc.distance(va)
    val s = (a + b + c) / 2f
    return abs(a * b * c / (8f * (s - a) * (s - b) * (s - c)))
}

fun barycentricWeights(pt: Vec3f, va: Vec3f, vb: Vec3f, vc: Vec3f, result: MutableVec3f): MutableVec3f {
    val e1 = MutableVec3f(vb).subtract(va)
    val e2 = MutableVec3f(vc).subtract(va)
    val n = e1.cross(e2, MutableVec3f())

    val a = n.length()
    val aa = a * a
    val m = MutableVec3f()

    e1.set(vc).subtract(vb)
    e2.set(pt).subtract(vb)
    result.x = (n * e1.cross(e2, m)) / aa

    e1.set(va).subtract(vc)
    e2.set(pt).subtract(vc)
    result.y = (n * e1.cross(e2, m)) / aa

    e1.set(vb).subtract(va)
    e2.set(pt).subtract(va)
    result.z = (n * e1.cross(e2, m)) / aa

    return result
}


/** Vector extensions **/

fun add(a: Vec2f, b: Vec2f): MutableVec2f = a.add(b, MutableVec2f())
fun add(a: Vec3f, b: Vec3f): MutableVec3f = a.add(b, MutableVec3f())
fun add(a: Vec2d, b: Vec2d): MutableVec2d = a.add(b, MutableVec2d())
fun add(a: Vec3d, b: Vec3d): MutableVec3d = a.add(b, MutableVec3d())

fun subtract(a: Vec2f, b: Vec2f): MutableVec2f = a.subtract(b, MutableVec2f())
fun subtract(a: Vec3f, b: Vec3f): MutableVec3f = a.subtract(b, MutableVec3f())
fun subtract(a: Vec2d, b: Vec2d): MutableVec2d = a.subtract(b, MutableVec2d())
fun subtract(a: Vec3d, b: Vec3d): MutableVec3d = a.subtract(b, MutableVec3d())

fun scale(a: Vec2f, fac: Float): MutableVec2f = a.scale(fac, MutableVec2f())
fun scale(a: Vec3f, fac: Float): MutableVec3f = a.scale(fac, MutableVec3f())
fun scale(a: Vec2d, fac: Double): MutableVec2d = a.scale(fac, MutableVec2d())
fun scale(a: Vec3d, fac: Double): MutableVec3d = a.scale(fac, MutableVec3d())

fun norm(a: Vec2f): MutableVec2f = a.norm(MutableVec2f())
fun norm(a: Vec3f): MutableVec3f = a.norm(MutableVec3f())
fun norm(a: Vec2d): MutableVec2d = a.norm(MutableVec2d())
fun norm(a: Vec3d): MutableVec3d = a.norm(MutableVec3d())

fun cross(a: Vec3f, b: Vec3f): MutableVec3f = a.cross(b, MutableVec3f())
fun cross(a: Vec3d, b: Vec3d): MutableVec3d = a.cross(b, MutableVec3d())

fun Vec3f.xy(): Vec2f = Vec2f(x, y)
fun MutableVec3f.xy(): MutableVec2f = MutableVec2f(x, y)

fun Vec3f.distanceToLine(lineA: Vec3f, lineB: Vec3f) = sqrt(sqrDistanceToLine(lineA, lineB))

fun Vec3f.sqrDistanceToLine(lineA: Vec3f, lineB: Vec3f) =
    sqrDistancePointToLine(x, y, z, lineA, lineB)

fun sqrDistancePointToLine(x: Float, y: Float, z: Float, lineA: Vec3f, lineB: Vec3f): Float {
    // vec math would be nice here, but we don't want to create a temporary MutableVec3f
    val rx = lineB.x - lineA.x
    val ry = lineB.y - lineA.y
    val rz = lineB.z - lineA.z

    val dotPt = x * rx + y * ry + z * rz
    val dotLineA = lineA.x * rx + lineA.y * ry + lineA.z * rz
    val dotR = rx * rx + ry * ry + rz * rz

    val l = (dotPt - dotLineA) / dotR
    val nx = rx * l + lineA.x
    val ny = ry * l + lineA.y
    val nz = rz * l + lineA.z

    val dx = nx - x
    val dy = ny - y
    val dz = nz - z
    return dx * dx + dy * dy + dz * dz
}

fun Vec3f.nearestPointOnLine(lineA: Vec3f, lineB: Vec3f, result: MutableVec3f): MutableVec3f {
    lineB.subtract(lineA, result)
    val l = (dot(result) - lineA * result) / (result * result)
    return result.scale(l).add(lineA)
}

//
// point to ray functions (one end, infinite length)
//

fun Vec3f.distanceToRay(ray: Ray) = distanceToRay(ray.origin, ray.direction)

fun Vec3f.distanceToRay(origin: Vec3f, direction: Vec3f) = sqrt(sqrDistanceToRay(origin, direction))

fun Vec3f.sqrDistanceToRay(ray: Ray) = sqrDistanceToRay(ray.origin, ray.direction)

fun Vec3f.sqrDistanceToRay(origin: Vec3f, direction: Vec3f) =
    sqrDistancePointToRay(x, y, z, origin, direction)

fun sqrDistancePointToRay(x: Float, y: Float, z: Float, origin: Vec3f, direction: Vec3f): Float {
    val nx: Float
    val ny: Float
    val nz: Float
    val dot = x * direction.x + y * direction.y + z * direction.z
    val l = (dot - origin * direction) / (direction * direction)
    if (l <= 0) {
        nx = origin.x - x
        ny = origin.y - y
        nz = origin.z - z
    } else {
        nx = direction.x * l + origin.x - x
        ny = direction.y * l + origin.y - y
        nz = direction.z * l + origin.z - z
    }
    return nx * nx + ny * ny + nz * nz
}

fun Vec3f.nearestPointOnRay(origin: Vec3f, direction: Vec3f, result: MutableVec3f): MutableVec3f {
    val l = (dot(direction) - origin * direction) / (direction * direction)
    return if (l <= 0) {
        result.set(origin)
    } else {
        result.set(direction).scale(l).add(origin)
    }
}

//
// point to edge functions (two ends, finite length)
//

fun Vec2f.distanceToEdge(edgeA: Vec2f, edgeB: Vec2f) = sqrt(sqrDistanceToEdge(edgeA, edgeB))

fun Vec2f.sqrDistanceToEdge(edgeA: Vec2f, edgeB: Vec2f) = sqrDistancePointToEdge(x, y, edgeA, edgeB)

fun sqrDistancePointToEdge(x: Float, y: Float, edgeA: Vec2f, edgeB: Vec2f): Float {
    // vec math would be nice here, but we don't want to create a temporary MutableVec3f
    val rx = edgeB.x - edgeA.x
    val ry = edgeB.y - edgeA.y

    val dotPt = x * rx + y * ry
    val dotEdgeA = edgeA.x * rx + edgeA.y * ry
    val dotR = rx * rx + ry * ry

    val l = (dotPt - dotEdgeA) / dotR
    val nx: Float
    val ny: Float
    when {
        l <= 0 -> {
            nx = edgeA.x; ny = edgeA.y
        }
        l >= 1 -> {
            nx = edgeB.x; ny = edgeB.y
        }
        else -> {
            nx = rx * l + edgeA.x; ny = ry * l + edgeA.y
        }
    }

    val dx = nx - x
    val dy = ny - y
    return dx * dx + dy * dy
}

fun Vec2f.nearestPointOnEdge(edgeA: Vec2f, edgeB: Vec2f, result: MutableVec2f): MutableVec2f {
    edgeB.subtract(edgeA, result)
    val l = (dot(result) - edgeA * result) / (result * result)
    return when {
        l <= 0 -> result.set(edgeA)
        l >= 1 -> result.set(edgeB)
        else -> result.scale(l).add(edgeA)
    }
}

fun Vec3f.distanceToEdge(edgeA: Vec3f, edgeB: Vec3f) = sqrt(sqrDistanceToEdge(edgeA, edgeB))

fun Vec3f.sqrDistanceToEdge(edgeA: Vec3f, edgeB: Vec3f) = sqrDistancePointToEdge(x, y, z, edgeA, edgeB)

fun sqrDistancePointToEdge(x: Float, y: Float, z: Float, edgeA: Vec3f, edgeB: Vec3f): Float {
    // vec math would be nice here, but we don't want to create a temporary MutableVec3f
    val rx = edgeB.x - edgeA.x
    val ry = edgeB.y - edgeA.y
    val rz = edgeB.z - edgeA.z

    val dotPt = x * rx + y * ry + z * rz
    val dotEdgeA = edgeA.x * rx + edgeA.y * ry + edgeA.z * rz
    val dotR = rx * rx + ry * ry + rz * rz

    val l = (dotPt - dotEdgeA) / dotR
    val nx: Float
    val ny: Float
    val nz: Float
    when {
        l <= 0 -> {
            nx = edgeA.x; ny = edgeA.y; nz = edgeA.z
        }
        l >= 1 -> {
            nx = edgeB.x; ny = edgeB.y; nz = edgeB.z
        }
        else -> {
            nx = rx * l + edgeA.x; ny = ry * l + edgeA.y; nz = rz * l + edgeA.z
        }
    }

    val dx = nx - x
    val dy = ny - y
    val dz = nz - z
    return dx * dx + dy * dy + dz * dz
}

fun Vec3f.nearestPointOnEdge(edgeA: Vec3f, edgeB: Vec3f, result: MutableVec3f): MutableVec3f {
    edgeB.subtract(edgeA, result)
    val l = (dot(result) - edgeA * result) / (result * result)
    return when {
        l <= 0 -> result.set(edgeA)
        l >= 1 -> result.set(edgeB)
        else -> result.scale(l).add(edgeA)
    }
}

fun map(inRangeStart: Float, inRangeEnd: Float, outRangeStart: Float, outRangeEnd: Float, value: Float) =
    outRangeStart + (value - inRangeStart) * (outRangeEnd - outRangeStart) / (inRangeEnd - inRangeStart)

val Int.nextPowerOfTwo: Int
    get() {
        var v = this
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return v
    }

/** Returns the previous power of two of [this] */
val Int.prevPowerOfTwo: Int get() = if (isPowerOfTwo) this else (nextPowerOfTwo ushr 1)

/** Checks if [this] value is power of two */
val Int.isPowerOfTwo: Boolean get() = this.nextPowerOfTwo == this

fun Float.interpolate(l: Float, r: Float): Float = (l + (r - l) * this)
fun Float.interpolate(l: Int, r: Int): Int = (l + (r - l) * this).toInt()
fun Float.interpolate(l: Long, r: Long): Long = (l + (r - l) * this).toLong()
fun Float.interpolate(l: Double, r: Double): Double = (l + (r - l) * this)

fun Double.interpolate(l: Float, r: Float): Float = (l + (r - l) * this).toFloat()
fun Double.interpolate(l: Double, r: Double): Double = (l + (r - l) * this)
fun Double.interpolate(l: Int, r: Int): Int = (l + (r - l) * this).toInt()
fun Double.interpolate(l: Long, r: Long): Long = (l + (r - l) * this).toLong()

fun ClosedFloatingPointRange<Double>.random() = Random.nextDouble(start, endInclusive)
fun ClosedFloatingPointRange<Float>.random() = Random.nextFloat() * (endInclusive - start) + start
fun IntRange.random() = Random.nextFloat() * (endInclusive - start) + start

fun sparseListOf(vararg ranges: IntRange): List<Int> = ranges.flatMap { it }

fun distSqr(ax: Double, ay: Double, bx: Double, by: Double) = (ax - bx) * (ax - bx) + (ay - by) * (ay - by)
fun distSqr(ax: Int, ay: Int, bx: Int, by: Int) = distSqr(ax.toDouble(), ay.toDouble(), bx.toDouble(), by.toDouble())
fun dist(ax: Double, ay: Double, bx: Double, by: Double) = sqrt(distSqr(ax, ay, bx, by))
fun dist(ax: Int, ay: Int, bx: Int, by: Int) = dist(ax.toDouble(), ay.toDouble(), bx.toDouble(), by.toDouble())

fun distRadians(a: Double, b: Double): Double = abs(subtractRadians(a, b))
fun distRadians(a: Int, b: Int): Double = distRadians(a.toDouble(), b.toDouble())

fun subtractRadians(a: Double, b: Double): Double = normalizeRadian(normalizeRadian(a) - normalizeRadian(b))

fun normalizeRadian(a: Double): Double {
    var result = a
    while (result < -PI) {
        result += PI * 2
    }
    while (result > PI) {
        result -= PI * 2
    }

    return result
}