package com.littlekt.util

import kotlin.js.Date

actual fun epochMillis(): Long = Date.now().toLong()

actual fun now(): Double = js("performance.now()") as Double

internal inline fun jsObject(init: dynamic.() -> Unit = {}): dynamic {
    val o = js("{}")
    init(o)
    return o
}