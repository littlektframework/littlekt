package com.littlekt.file

/** @return an [Int] representing this [Byte] as if it was unsigned 0x00...0xFF */
internal inline val Byte.unsigned: Int
    get() = this.toInt() and 0xFF

/** @return a [Long] representing this [Int] as if it was unsigned 0x00000000L...0xFFFFFFFFL */
internal inline val Int.unsigned: Long
    get() = this.toLong() and 0xFFFFFFFFL

/**
 * Takes n[bits] of [this] [Int], and extends the last bit, creating a plain [Int] in one's
 * complement
 */
internal fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits)

/**
 * Takes n[bits] of [this] [Long], and extends the last bit, creating a plain [Long] in one's
 * complement
 */
internal fun Long.signExtend(bits: Int): Long = (this shl (64 - bits)) shr (64 - bits)
