@file:Suppress("NOTHING_TO_INLINE", "EXTENSION_SHADOWED_BY_MEMBER")

package com.lehaine.littlekt.math.geom

import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.util.internal.umod
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.math.atan2


@PublishedApi
internal const val MAX_DEGREES = 360f

@PublishedApi
internal const val MAX_RADIANS = PI2_F

@PublishedApi
internal const val HALF_DEGREES = MAX_DEGREES / 2f

@PublishedApi
internal const val HALF_RADIANS = MAX_RADIANS / 2f

@PublishedApi
internal fun Angle_shortDistanceTo(from: Angle, to: Angle): Angle {
    val r0 = from.radians umod MAX_RADIANS
    val r1 = to.radians umod MAX_RADIANS
    val diff = (r1 - r0 + HALF_RADIANS) % MAX_RADIANS - HALF_RADIANS
    return if (diff < -HALF_RADIANS) Angle(diff + MAX_RADIANS) else Angle(diff)
}

@PublishedApi
internal fun Angle_longDistanceTo(from: Angle, to: Angle): Angle {
    val short = Angle_shortDistanceTo(from, to)
    return when {
        short == Angle.ZERO -> Angle.ZERO
        short < Angle.ZERO -> 360.degrees + short
        else -> (-360).degrees + short
    }
}

@PublishedApi
internal fun Angle_between(x0: Float, y0: Float, x1: Float, y1: Float): Angle {
    val angle = atan2(y1 - y0, x1 - x0)
    return if (angle < 0) Angle(angle + PI2_F) else Angle(angle)
}

// https://github.com/korlibs/korge-next/blob/master/korma/src/commonMain/kotlin/com/soywiz/korma/geom/Angle.kt
@JvmInline
value class Angle(val radians: Float) : Comparable<Angle> {
    override fun toString(): String = "$degrees.degrees"

    operator fun times(scale: Float): Angle = Angle(this.radians * scale)
    operator fun div(scale: Float): Angle = Angle(this.radians / scale)
    operator fun times(scale: Double): Angle = this * scale.toFloat()
    operator fun div(scale: Double): Angle = this / scale.toFloat()
    operator fun times(scale: Int): Angle = this * scale.toFloat()
    operator fun div(scale: Int): Angle = this / scale.toFloat()

    operator fun div(other: Angle): Float = this.radians / other.radians // Ratio
    operator fun plus(other: Angle): Angle = Angle(this.radians + other.radians)
    operator fun minus(other: Angle): Angle = Angle(this.radians - other.radians)
    operator fun unaryMinus(): Angle = Angle(-radians)
    operator fun unaryPlus(): Angle = Angle(+radians)

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        inline val ZERO get() = Angle(0f)

        inline fun fromRadians(radians: Float) = Angle(radians)
        inline fun fromDegrees(degrees: Float) = Angle(degreesToRadians(degrees))

        inline fun fromRadians(radians: Double) = fromRadians(radians.toFloat())
        inline fun fromDegrees(degrees: Double) = fromDegrees(degrees.toFloat())

        inline fun fromRadians(radians: Int) = fromRadians(radians.toDouble())
        inline fun fromDegrees(degrees: Int) = fromDegrees(degrees.toDouble())

        inline fun cos01(ratio: Double) = kotlin.math.cos(PI2 * ratio)
        inline fun sin01(ratio: Double) = kotlin.math.sin(PI2 * ratio)
        inline fun tan01(ratio: Double) = kotlin.math.tan(PI2 * ratio)

        inline fun degreesToRadians(degrees: Float): Float = (degrees.toRad())
        inline fun radiansToDegrees(radians: Float): Float = (radians.toDeg())

        inline fun shortDistanceTo(from: Angle, to: Angle): Angle = Angle_shortDistanceTo(from, to)
        inline fun longDistanceTo(from: Angle, to: Angle): Angle = Angle_longDistanceTo(from, to)
        inline fun between(x0: Float, y0: Float, x1: Float, y1: Float): Angle = Angle_between(x0, y0, x1, y1)

        inline fun between(x0: Int, y0: Int, x1: Int, y1: Int): Angle =
            between(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())

        inline fun between(x0: Double, y0: Double, x1: Double, y1: Double): Angle =
            between(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
    }

    override fun compareTo(other: Angle): Int {
        val left = this.radians
        val right = other.radians
        if (left < right) return -1
        if (left > right) return +1
        return 0
    }
}

inline fun cos(angle: Angle): Float = kotlin.math.cos(angle.radians)
inline fun sin(angle: Angle): Float = kotlin.math.sin(angle.radians)
inline fun tan(angle: Angle): Float = kotlin.math.tan(angle.radians)
inline fun abs(angle: Angle): Angle = Angle.fromRadians(angle.radians.absoluteValue)

val Angle.cosine get() = cos(this)
val Angle.sine get() = sin(this)
val Angle.tangent get() = tan(this)
val Angle.degrees get() = Angle.radiansToDegrees(radians)

val Angle.absoluteValue: Angle get() = Angle.fromRadians(radians.absoluteValue)
fun Angle.shortDistanceTo(other: Angle): Angle = Angle.shortDistanceTo(this, other)
fun Angle.longDistanceTo(other: Angle): Angle = Angle.longDistanceTo(this, other)

infix fun Angle.until(other: Angle) = OpenRange(this, other)

fun Angle.inBetweenInclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = true)
fun Angle.inBetweenExclusive(min: Angle, max: Angle): Boolean = inBetween(min, max, inclusive = false)

infix fun Angle.inBetween(range: ClosedRange<Angle>): Boolean =
    inBetween(range.start, range.endInclusive, inclusive = true)

infix fun Angle.inBetween(range: OpenRange<Angle>): Boolean =
    inBetween(range.start, range.endExclusive, inclusive = false)

fun Angle.inBetween(min: Angle, max: Angle, inclusive: Boolean): Boolean {
    val nthis = this.normalized
    val nmin = min.normalized
    val nmax = max.normalized
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    return when {
        nmin > nmax -> nthis >= nmin || (if (inclusive) nthis <= nmax else nthis < nmax)
        else -> nthis >= nmin && (if (inclusive) nthis <= nmax else nthis < nmax)
    }
}

operator fun Float.times(angle: Angle): Angle = Angle(this * angle.radians)
operator fun Float.div(angle: Angle): Angle = Angle(this / angle.radians)
operator fun Double.times(angle: Angle): Angle = this.toFloat() * angle
operator fun Double.div(angle: Angle): Angle = this.toFloat() / angle
operator fun Int.times(angle: Angle): Angle = this.toFloat() * angle
operator fun Int.div(angle: Angle): Angle = this.toFloat() / angle

val Double.degrees get() = Angle.fromDegrees(this)
val Double.radians get() = Angle.fromRadians(this)
val Int.degrees get() = Angle.fromDegrees(this)
val Int.radians get() = Angle.fromRadians(this)
val Float.degrees get() = Angle.fromDegrees(this)
val Float.radians get() = Angle.fromRadians(this)

val Angle.normalized get() = Angle(radians umod MAX_RADIANS)

fun Double.interpolate(l: Angle, r: Angle): Angle = this.interpolate(l.radians, r.radians).radians