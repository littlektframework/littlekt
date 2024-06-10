package com.littlekt.async

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncExecutor actual constructor(maxConcurrent: Int) : Disposable {
    actual val maxConcurrent: Int
        get() = TODO("Not yet implemented")

    actual fun <T> submit(action: () -> T): AsyncResult<T> {
        TODO("Not yet implemented")
    }

    actual override fun release() {}
}

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncResult<T> {
    actual val isDone: Boolean
        get() = TODO("Not yet implemented")

    actual fun get(): T {
        TODO("Not yet implemented")
    }
}
