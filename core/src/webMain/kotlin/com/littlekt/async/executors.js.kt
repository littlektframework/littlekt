package com.littlekt.async

import com.littlekt.Releasable

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncExecutor actual constructor(actual val maxConcurrent: Int) : Releasable {

    actual fun <T> submit(action: () -> T): AsyncResult<T> {
        return AsyncResult(action.invoke())
    }

    actual override fun release() = Unit
}

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncResult<T>(private val result: T) {
    actual val isDone: Boolean = true

    actual fun get(): T = result
}