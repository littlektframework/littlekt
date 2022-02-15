package com.lehaine.littlekt.async

import com.lehaine.littlekt.Disposable
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncExecutor actual constructor(actual val maxConcurrent: Int) : Disposable {
    private val executor = Executors.newFixedThreadPool(maxConcurrent) {
        Thread(it, "AsyncExecutor-Thread").apply { isDaemon = true }
    }

    actual fun <T> submit(action: () -> T): AsyncResult<T> {
        if (executor.isShutdown) {
            throw IllegalStateException("Cannot run tasks on an executor that has been shutdown (disposed)")
        }
        return AsyncResult(executor.submit(Callable { action.invoke() }))
    }

    actual override fun dispose() {
        executor.shutdown()
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw IllegalStateException("Couldn't shutdown async executor thread", e)
        }

    }
}

/**
 * @author Colton Daily
 * @date 1/10/2022
 */
actual class AsyncResult<T>(private val future: Future<T>) {
    actual val isDone: Boolean get() = future.isDone

    actual fun get(): T = future.get()
}