package com.littlekt.file

actual fun <T> nativeGet(array: Array<T>, index: Int): T = array[index]
actual fun <T> nativeSet(array: Array<T>, index: Int, element: T) {
    array[index] = element
}

actual fun nativeSet(array: ByteArray, index: Int, element: Byte) {
    array[index] = element
}

actual fun nativeSet(array: IntArray, index: Int, element: Int){
    array[index] = element
}
actual fun nativeIndexOf(array: IntArray, element: Int): Int = array.indexOf(element)
actual fun nativeGet(array: ShortArray, index: Int): Short = array[index]

actual fun nativeGet(array: IntArray, index: Int): Int = array[index]
actual fun nativeGet(array: FloatArray, index: Int): Float = array[index]
actual fun nativeGet(array: ByteArray, index: Int): Byte = array[index]