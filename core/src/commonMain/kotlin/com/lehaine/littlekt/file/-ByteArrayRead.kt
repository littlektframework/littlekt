package com.lehaine.littlekt.file

private fun ByteArray.u8(o: Int): Int = this[o].toInt() and 0xFF

private inline fun ByteArray.read16LE(o: Int): Int = (u8(o + 0) shl 0) or (u8(o + 1) shl 8)
private inline fun ByteArray.read24LE(o: Int): Int = (u8(o + 0) shl 0) or (u8(o + 1) shl 8) or (u8(o + 2) shl 16)
private inline fun ByteArray.read32LE(o: Int): Int =
    (u8(o + 0) shl 0) or (u8(o + 1) shl 8) or (u8(o + 2) shl 16) or (u8(o + 3) shl 24)

private inline fun ByteArray.read64LE(o: Int): Long =
    (read32LE(o + 0).unsigned shl 0) or (read32LE(o + 4).unsigned shl 32)

private inline fun ByteArray.read16BE(o: Int): Int = (u8(o + 1) shl 0) or (u8(o + 0) shl 8)
private inline fun ByteArray.read24BE(o: Int): Int = (u8(o + 2) shl 0) or (u8(o + 1) shl 8) or (u8(o + 0) shl 16)
private inline fun ByteArray.read32BE(o: Int): Int =
    (u8(o + 3) shl 0) or (u8(o + 2) shl 8) or (u8(o + 1) shl 16) or (u8(o + 0) shl 24)

private inline fun ByteArray.read64BE(o: Int): Long =
    (read32BE(o + 4).unsigned shl 0) or (read32BE(o + 0).unsigned shl 32)

// Unsigned
internal fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
internal fun ByteArray.readU16LE(o: Int): Int = read16LE(o)
internal fun ByteArray.readU24LE(o: Int): Int = read24LE(o)
internal fun ByteArray.readU32LE(o: Int): Long = read32LE(o).unsigned
internal fun ByteArray.readU16BE(o: Int): Int = read16BE(o)
internal fun ByteArray.readU24BE(o: Int): Int = read24BE(o)
internal fun ByteArray.readU32BE(o: Int): Long = read32BE(o).unsigned

// Signed
internal fun ByteArray.readS8(o: Int): Int = this[o].toInt()
internal fun ByteArray.readS16LE(o: Int): Int = read16LE(o).signExtend(16)
internal fun ByteArray.readS24LE(o: Int): Int = read24LE(o).signExtend(24)
internal fun ByteArray.readS32LE(o: Int): Int = read32LE(o)
internal fun ByteArray.readS64LE(o: Int): Long = read64LE(o)
internal fun ByteArray.readF32LE(o: Int): Float = Float.fromBits(read32LE(o))
internal fun ByteArray.readF64LE(o: Int): Double = Double.fromBits(read64LE(o))
internal fun ByteArray.readS16BE(o: Int): Int = read16BE(o).signExtend(16)
internal fun ByteArray.readS24BE(o: Int): Int = read24BE(o).signExtend(24)
internal fun ByteArray.readS32BE(o: Int): Int = read32BE(o)
internal fun ByteArray.readS64BE(o: Int): Long = read64BE(o)
internal fun ByteArray.readF32BE(o: Int): Float = Float.fromBits(read32BE(o))
internal fun ByteArray.readF64BE(o: Int): Double = Double.fromBits(read64BE(o))

// Custom Endian
internal fun ByteArray.readU16(o: Int, little: Boolean): Int = if (little) readU16LE(o) else readU16BE(o)
internal fun ByteArray.readU24(o: Int, little: Boolean): Int = if (little) readU24LE(o) else readU24BE(o)
internal fun ByteArray.readU32(o: Int, little: Boolean): Long = if (little) readU32LE(o) else readU32BE(o)
internal fun ByteArray.readS16(o: Int, little: Boolean): Int = if (little) readS16LE(o) else readS16BE(o)
internal fun ByteArray.readS24(o: Int, little: Boolean): Int = if (little) readS24LE(o) else readS24BE(o)
internal fun ByteArray.readS32(o: Int, little: Boolean): Int = if (little) readS32LE(o) else readS32BE(o)
internal fun ByteArray.readS64(o: Int, little: Boolean): Long = if (little) readS64LE(o) else readS64BE(o)
internal fun ByteArray.readF32(o: Int, little: Boolean): Float = if (little) readF32LE(o) else readF32BE(o)
internal fun ByteArray.readF64(o: Int, little: Boolean): Double = if (little) readF64LE(o) else readF64BE(o)

private inline fun <T> ByteArray.readTypedArray(
    o: Int,
    count: Int,
    elementSize: Int,
    array: T,
    crossinline read: ByteArray.(array: T, n: Int, pos: Int) -> Unit
): T = array.also {
    for (n in 0 until count) read(this, array, n, o + n * elementSize)
}

internal fun ByteArray.readByteArray(o: Int, count: Int): ByteArray = this.copyOfRange(o, o + count)
internal fun ByteArray.readShortArrayLE(o: Int, count: Int): ShortArray =
    this.readTypedArray(o, count, 2, ShortArray(count)) { array, n, pos -> array[n] = readS16LE(pos).toShort() }

internal fun ByteArray.readCharArrayLE(o: Int, count: Int): CharArray =
    this.readTypedArray(o, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = readS16LE(pos).toChar() }

internal fun ByteArray.readIntArrayLE(o: Int, count: Int): IntArray =
    this.readTypedArray(o, count, 4, IntArray(count)) { array, n, pos -> array[n] = readS32LE(pos) }

internal fun ByteArray.readLongArrayLE(o: Int, count: Int): LongArray =
    this.readTypedArray(o, count, 8, LongArray(count)) { array, n, pos -> array[n] = readS64LE(pos) }

internal fun ByteArray.readFloatArrayLE(o: Int, count: Int): FloatArray =
    this.readTypedArray(o, count, 4, FloatArray(count)) { array, n, pos -> array[n] = readF32LE(pos) }

internal fun ByteArray.readDoubleArrayLE(o: Int, count: Int): DoubleArray =
    this.readTypedArray(o, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = readF64LE(pos) }

internal fun ByteArray.readShortArrayBE(o: Int, count: Int): ShortArray =
    this.readTypedArray(o, count, 2, ShortArray(count)) { array, n, pos -> array[n] = readS16BE(pos).toShort() }

internal fun ByteArray.readCharArrayBE(o: Int, count: Int): CharArray =
    this.readTypedArray(o, count, 2, kotlin.CharArray(count)) { array, n, pos -> array[n] = readS16BE(pos).toChar() }

internal fun ByteArray.readIntArrayBE(o: Int, count: Int): IntArray =
    this.readTypedArray(o, count, 4, IntArray(count)) { array, n, pos -> array[n] = readS32BE(pos) }

internal fun ByteArray.readLongArrayBE(o: Int, count: Int): LongArray =
    this.readTypedArray(o, count, 8, LongArray(count)) { array, n, pos -> array[n] = readS64BE(pos) }

internal fun ByteArray.readFloatArrayBE(o: Int, count: Int): FloatArray =
    this.readTypedArray(o, count, 4, FloatArray(count)) { array, n, pos -> array[n] = readF32BE(pos) }

internal fun ByteArray.readDoubleArrayBE(o: Int, count: Int): DoubleArray =
    this.readTypedArray(o, count, 8, DoubleArray(count)) { array, n, pos -> array[n] = readF64BE(pos) }

internal fun ByteArray.readShortArray(o: Int, count: Int, little: Boolean): ShortArray =
    if (little) readShortArrayLE(o, count) else readShortArrayBE(o, count)

internal fun ByteArray.readCharArray(o: Int, count: Int, little: Boolean): CharArray =
    if (little) readCharArrayLE(o, count) else readCharArrayBE(o, count)

internal fun ByteArray.readIntArray(o: Int, count: Int, little: Boolean): IntArray =
    if (little) readIntArrayLE(o, count) else readIntArrayBE(o, count)

internal fun ByteArray.readLongArray(o: Int, count: Int, little: Boolean): LongArray =
    if (little) readLongArrayLE(o, count) else readLongArrayBE(o, count)

internal fun ByteArray.readFloatArray(o: Int, count: Int, little: Boolean): FloatArray =
    if (little) readFloatArrayLE(o, count) else readFloatArrayBE(o, count)

internal fun ByteArray.readDoubleArray(o: Int, count: Int, little: Boolean): DoubleArray =
    if (little) readDoubleArrayLE(o, count) else readDoubleArrayBE(o, count)