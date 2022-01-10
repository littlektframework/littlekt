package com.lehaine.littlekt.async

import com.lehaine.littlekt.Disposable

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
expect class AsyncExecutor(maxConcurrent: Int) : Disposable {
    val maxConcurrent: Int
    fun <T> submit(action: () -> T): AsyncResult<T>
    override fun dispose()
}

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
expect class AsyncResult<T> {
    val isDone: Boolean
    fun get(): T
}