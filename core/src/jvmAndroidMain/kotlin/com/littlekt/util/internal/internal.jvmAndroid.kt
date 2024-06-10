package com.littlekt.util.internal

internal actual inline fun <R> lock(lock: Any, block: () -> R): R = synchronized(lock, block)
