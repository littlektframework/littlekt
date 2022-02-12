package com.lehaine.littlekt.log

import java.util.concurrent.atomic.AtomicReference

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
internal actual class KtAtomicRef<T> actual constructor(initial: T) {
    private val ref = AtomicReference(initial)

    actual var value: T
        get() = ref.get()
        set(value) {
            ref.set(value)
        }

    actual inline fun update(block: (T) -> T) {
        do {
            val old = ref.get()
            val new = block(old)
        } while (!ref.compareAndSet(old, new))
    }
}