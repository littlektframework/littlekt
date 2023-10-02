package com.lehaine.littlekt.file

import org.khronos.webgl.*

fun ByteArray.toInt8Array(): Int8Array {
    val out = Int8Array(this.size)
    for (n in 0 until out.length) out[n] = this[n]
    return out
}

fun ByteArray.toUint8Array(): Uint8Array {
    return Uint8Array(toInt8Array().buffer)
}

fun FloatArray.toFloat32Array(): Float32Array {
    val out = Float32Array(this.size)
    for (n in 0 until out.length) out[n] = this[n]
    return out
}