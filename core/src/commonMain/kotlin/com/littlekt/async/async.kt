package com.littlekt.async

import com.littlekt.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

/**
 * The main **LittleKt** coroutine scope. Executes tasks on the main rendering thread. See
 * [MainDispatcher]
 *
 * @author Colton Daily
 * @date 1/9/2022
 */
object KtScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = MainDispatcher.INSTANCE

    /** Needs to be invoked on the [Context] that needs wrapped. Handled internally. */
    internal fun initiate() {
        MainDispatcher.createInstance(Unit)
    }
}

/** Coroutine scope used by vfs to load files. */
object VfsScope : CoroutineScope {
    internal val job = Job()

    override val coroutineContext: CoroutineContext = job
}

/**
 * THe main **LittleKt** coroutine dispatcher. Executes tasks on the main rendering thread. See
 * [MainDispatcher]
 */
val Dispatchers.KT
    get() = MainDispatcher.INSTANCE

/**
 * Creates a new [AsyncThreadDispatcher] wrapping around an [AsyncExecutor] with a single thread to
 * execute tasks asynchronously outside of the main rendering thread.
 */
fun newSingleThreadAsyncContext() = newAsyncContext(1)

/**
 * Creates a new [AsyncThreadDispatcher] wrapping around an [AsyncExecutor] with the chosen amount
 * of [threads] to execute tasks asynchronously outside of the main rendering thread.
 */
fun newAsyncContext(threads: Int) = AsyncThreadDispatcher(AsyncExecutor(threads), threads)

/**
 * Suspends the coroutine to execute the defined [block] on the main rendering thread and return its
 * result.
 */
suspend fun <T> onRenderingThread(block: suspend CoroutineScope.() -> T) =
    withContext(MainDispatcher.INSTANCE, block = block)

/** Returns true if the coroutine was launched from the rendering thread dispatcher. */
expect fun CoroutineScope.isOnRenderingThread(): Boolean
