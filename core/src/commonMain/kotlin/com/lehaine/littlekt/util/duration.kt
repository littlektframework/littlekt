package com.lehaine.littlekt.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * @author Colton Daily
 * @date 12/26/2021
 */

inline val Float.nanoseconds get() = this.toDouble().nanoseconds
inline val Float.microseconds get() = this.toDouble().microseconds
inline val Float.milliseconds get() = this.toDouble().milliseconds
inline val Float.seconds get() = this.toDouble().seconds
inline val Float.minutes get() = this.toDouble().minutes
inline val Float.hours get() = this.toDouble().hours
inline val Float.days get() = this.toDouble().days

val Duration.days: Float get() = toDouble(DurationUnit.DAYS).toFloat()
val Duration.hours: Float get() = toDouble(DurationUnit.HOURS).toFloat()
val Duration.minutes: Float get() = toDouble(DurationUnit.MINUTES).toFloat()
val Duration.seconds: Float get() = toDouble(DurationUnit.SECONDS).toFloat()
val Duration.milliseconds: Float get() = toDouble(DurationUnit.MILLISECONDS).toFloat()
val Duration.microseconds: Float get() = toDouble(DurationUnit.MICROSECONDS).toFloat()
val Duration.nanoseconds: Float get() = toDouble(DurationUnit.NANOSECONDS).toFloat()