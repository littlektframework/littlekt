package com.littlekt.util.internal

internal actual inline fun <R> lock(lock: Any, block: () -> R): R = block()

internal inline fun jsObject(init: dynamic.() -> Unit = {}): dynamic {
    val o = js("{}")
    init(o)
    return o
}
