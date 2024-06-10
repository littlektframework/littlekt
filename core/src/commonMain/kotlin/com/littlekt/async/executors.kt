package com.littlekt.async

import com.littlekt.Releasable

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
expect class AsyncExecutor(maxConcurrent: Int) : Releasable {
    val maxConcurrent: Int

    fun <T> submit(action: () -> T): AsyncResult<T>

    override fun release()
}

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
expect class AsyncResult<T> {
    val isDone: Boolean

    fun get(): T
}
