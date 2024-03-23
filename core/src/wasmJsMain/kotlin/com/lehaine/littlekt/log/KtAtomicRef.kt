package com.lehaine.littlekt.log

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
internal actual class KtAtomicRef<T> actual constructor(initial: T) {

    actual var value: T = initial

    actual inline fun update(block: (T) -> T) {
        value = block(value)
    }
}