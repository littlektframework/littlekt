package com.lehaine.littlekt.util.internal

import kotlin.js.Date

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
internal actual fun epochMillis(): Long = Date.now().toLong()

internal actual fun now(): Double = js("performance.now()") as Double

actual inline fun <R> lock(lock: Any, block: () -> R): R = block()

internal inline fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}