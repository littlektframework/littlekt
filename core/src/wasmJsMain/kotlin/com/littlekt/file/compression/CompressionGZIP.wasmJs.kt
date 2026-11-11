package com.littlekt.file.compression

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.Int32Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.toFloat32Array
import org.khronos.webgl.toInt16Array
import org.khronos.webgl.toInt32Array
import org.khronos.webgl.toUint16Array
import org.khronos.webgl.toUint32Array
import org.khronos.webgl.toUint8Array

actual fun toUint8Array(input: ByteArray): Uint8Array = input.toUByteArray().toUint8Array()
actual fun nativeByteArray(output: Uint8Array): ByteArray = ByteArray(output.length) { output[it] }
actual fun toUint16Array(input: ShortArray): Uint16Array = input.toUShortArray().toUint16Array()
actual fun toUint32Array(input: IntArray): Uint32Array = input.toUIntArray().toUint32Array()
actual fun toFloat32Array(input: FloatArray): Float32Array = input.toFloat32Array()
internal actual fun toInt16Array(input: ShortArray): Int16Array = input.toInt16Array()
internal actual fun toInt32Array(input: IntArray): Int32Array = input.toInt32Array()
