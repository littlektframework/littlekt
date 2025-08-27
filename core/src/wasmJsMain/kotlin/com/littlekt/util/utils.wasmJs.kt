package com.littlekt.util

@JsName("Date")
external class JsDate {
    companion object {
        fun now(): Double
    }
}
actual fun epochMillis(): Long = JsDate.now().toLong()

@JsFun("() => performance.now()")
external fun getPerformanceNow(): Double

actual fun now(): Double = getPerformanceNow()
actual fun nativeIndexOf(array: IntArray, element: Int): Int = array.indexOf(element)
actual fun <T> nativeGet(array: Array<T>, index: Int): T = array[index]
actual fun nativeGet(array: ShortArray, index: Int): Short = array[index]
actual fun nativeGet(array: IntArray, index: Int): Int = array[index]
actual fun nativeGet(array: FloatArray, index: Int): Float = array[index]
actual fun nativeGetOrNull(array: FloatArray, index: Int): Float? = array.getOrNull(index)
actual fun nativeGet(array: ByteArray, index: Int): Byte = array[index]
actual fun <T> nativeSet(array: Array<T>, index: Int, element: T) {
    array[index] = element
}
actual fun nativeSet(array: ByteArray, index: Int, element: Byte) {
    array[index] = element
}
actual fun nativeSet(array: IntArray, index: Int, element: Int) {
    array[index] = element
}