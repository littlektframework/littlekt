package com.lehaine.littlekt.util.internal

import java.util.*

/**
 * @author Colton Daily
 * @date 11/17/2021
 */

internal actual fun epochMillis(): Long = System.currentTimeMillis()

internal actual fun now():Double = System.nanoTime() / 1e6

actual inline fun <R> lock(lock: Any, block: () -> R): R = synchronized(lock, block)