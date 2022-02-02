package com.lehaine.littlekt.util.internal

import kotlin.math.*

/**
 * @author Colton Daily
 * @date 9/29/2021
 */

internal const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60 // 60_000
internal const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60 // 3600_000
internal const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24 // 86400_000
internal const val MILLIS_PER_WEEK = MILLIS_PER_DAY * 7 // 604800_000

internal val Float.niceStr: String get() = if (floor(this) == this) "${this.toInt()}" else "$this"

internal fun Int.padded(count: Int): String {
    // @TODO: Handle edge case Int.MIN_VALUE that could not be represented as abs
    val res = this.absoluteValue.toString().padStart(count, '0')
    return if (this < 0) return "-$res" else res
}

internal fun Float.padded(intCount: Int, decCount: Int): String {
    val intPart = floor(this).toInt()
    val decPart = round((this - intPart) * 10.0.pow(decCount)).toInt()
    return "${intPart.padded(intCount).substr(-intCount, intCount)}.${
        decPart.toString().padEnd(decCount, '0').substr(0, decCount)
    }"
}

internal fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
internal fun Int.cycle(min: Int, max: Int): Int = ((this - min) umod (max - min + 1)) + min
internal fun Int.cycleSteps(min: Int, max: Int): Int = (this - min) / (max - min + 1)
internal fun Int.mask(): Int = (1 shl this) - 1
internal fun Int.insert(value: Int, offset: Int, count: Int): Int {
    val mask = count.mask()
    val clearValue = this and (mask shl offset).inv()
    return clearValue or ((value and mask) shl offset)
}

internal infix fun Int.umod(that: Int): Int {
    val remainder = this % that
    return when {
        remainder < 0 -> remainder + that
        else -> remainder
    }
}

internal infix fun Float.umod(that: Float): Float {
    val remainder = this % that
    return when {
        remainder < 0 -> remainder + that
        else -> remainder
    }
}

internal infix fun Double.umod(that: Double): Double {
    val remainder = this % that
    return when {
        remainder < 0 -> remainder + that
        else -> remainder
    }
}


internal fun String.substr(start: Int, length: Int): String {
    val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
    val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
    return if (high < low) "" else this.substring(low, high)
}

internal fun String.unescape(): String {
    val out = StringBuilder()
    var n = 0
    while (n < this.length) {
        val c = this[n++]
        when (c) {
            '\\' -> {
                val c2 = this[n++]
                when (c2) {
                    '\\' -> out.append('\\')
                    '"' -> out.append('\"')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> {
                        val chars = this.substring(n, n + 4)
                        n += 4
                        out.append(chars.toInt(16).toChar())
                    }
                    else -> {
                        out.append("\\$c2")
                    }
                }
            }
            else -> out.append(c)
        }
    }
    return out.toString()
}

internal fun String.isQuoted(): Boolean = this.startsWith('"') && this.endsWith('"')
internal fun String.unquote(): String = if (isQuoted()) this.substring(1, this.length - 1).unescape() else this

internal expect fun epochMillis(): Long

internal expect fun now(): Double

internal expect inline fun <R> lock(lock: Any, block: () -> R): R

private val NUMBERS = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex()

internal fun String.compareName(s2: String): Int {
    val s1 = this
    val split1 = NUMBERS.split(s1)
    val split2 = NUMBERS.split(s2)
    for (i in 0 until min(split1.size, split2.size)) {
        val c1 = split1[i][0]
        val c2 = split2[i][0]
        var cmp = 0
        // If both segments start with a digit, sort them numerically using
        // BigInteger to stay safe
        if (c1 in '0'..'9' && c2 in '0'..'9')
            cmp = split1[i].toInt().compareTo(split2[i].toInt())

        // If we haven't sorted numerically before, or if numeric sorting yielded
        // equality (e.g 007 and 7) then sort lexicographically
        if (cmp == 0)
            cmp = split1[i].compareTo(split2[i])

        // Abort once some prefix has unequal ordering
        if (cmp != 0)
            return cmp
    }

    // If we reach this, then both strings have equally ordered prefixes, but
    // maybe one string is longer than the other (i.e. has more segments)
    return split1.size - split2.size
}