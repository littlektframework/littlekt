package com.lehaine.littlekt.log

import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
internal expect class KtAtomicRef<T>(initial: T) {
    var value: T
    inline fun update(block: (T) -> T)
}

internal operator fun <T> KtAtomicRef<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

internal operator fun <T> KtAtomicRef<T>.getValue(receiver: Any?, property: KProperty<*>): T {
    return this.value
}
