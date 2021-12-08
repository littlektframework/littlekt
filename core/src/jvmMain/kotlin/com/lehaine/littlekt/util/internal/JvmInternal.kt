package com.lehaine.littlekt.util.internal

import java.util.*

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
internal actual fun epochMillis(): Long = System.currentTimeMillis()

actual inline fun <R> lock(lock: Any, block: () -> R): R = synchronized(lock, block)