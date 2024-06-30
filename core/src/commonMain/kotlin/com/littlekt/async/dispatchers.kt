package com.littlekt.async

import com.littlekt.Context
import com.littlekt.Releasable
import com.littlekt.util.internal.SingletonBase
import com.littlekt.util.internal.lock
import com.littlekt.util.now
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** The base interface of [CoroutineContext] for dispatchers. */
@OptIn(InternalCoroutinesApi::class)
interface KtDispatcher : CoroutineContext, Delay {

    /** Immediately executes the passed [block]. */
    fun execute(block: Runnable)

    /** Schedules the execution of the passed [block]. */
    fun queue(block: Runnable)
}

/**
 * A [CoroutineDispatcher] that wraps around an [AsyncExecutor] instance ([executor]) to execute
 * tasks asynchronously.
 *
 * Requires calling [executePending] in order to support [delay].
 */
class AsyncThreadDispatcher(
    val executor: AsyncExecutor,
    val threads: Int = -1,
) : CoroutineDispatcher(), KtDispatcher, Releasable {
    private val lock: Any = Any()
    private val tasks = mutableListOf<Runnable>()
    private val timedTasks: MutableList<TimedTask> = mutableListOf()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        execute(block)
    }

    override fun execute(block: Runnable) {
        executor.submit(block::run)
    }

    override fun queue(block: Runnable) {
        lock(lock) { tasks += block }
    }

    override fun release() {
        executor.release()
    }

    override fun invokeOnTimeout(
        timeMillis: Long,
        block: Runnable,
        context: CoroutineContext
    ): DisposableHandle {
        val task = TimedTask(now().milliseconds + timeMillis.milliseconds, null, block)
        lock(lock) { timedTasks += task }
        return DisposableHandle { lock(lock) { timedTasks -= task } }
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    /** Executes any pending timed ([delay]) or queued ([queue]) tasks. */
    fun executePending() {
        try {
            val startTime = now().milliseconds
            executeTimedTasks(startTime)
            executeQueuedTasks()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun scheduleResumeAfterDelay(
        time: Duration,
        continuation: CancellableContinuation<Unit>
    ) {
        val task = TimedTask(now().milliseconds + time, continuation, null)
        continuation.invokeOnCancellation { task.exception = it }
        lock(lock) { timedTasks += task }
    }

    private fun executeTimedTasks(startTime: Duration) {
        while (true) {
            val item =
                lock(lock) {
                    if (timedTasks.isNotEmpty() && startTime >= timedTasks.first().time) {
                        timedTasks.removeFirst()
                    } else {
                        null
                    }
                } ?: break
            item.exception?.let { exception ->
                item.continuation?.resumeWithException(exception)
                item.callback?.let { exception.printStackTrace() }
            }
                ?: run {
                    item.continuation?.resume(Unit)
                    item.callback?.run()
                }
        }
    }

    private fun executeQueuedTasks() {
        while (true) {
            val task =
                lock(lock) {
                    if (tasks.isNotEmpty()) {
                        tasks.removeFirst()
                    } else {
                        null
                    }
                } ?: break
            task.run()
        }
    }

    override fun toString(): String {
        return "AsyncThreadDispatcher(executor=$executor, threads=$threads)"
    }

    private class TimedTask(
        val time: Duration,
        val continuation: CancellableContinuation<Unit>?,
        val callback: Runnable?,
    ) {
        var exception: Throwable? = null
    }
}

/**
 * A [CoroutineDispatcher] that wraps around a [Context] to execute tasks on main rendering thread.
 *
 * Requires calling [executePending] with the amount of time available in order to support [delay].
 */
sealed class RenderingThreadDispatcher : MainCoroutineDispatcher(), KtDispatcher {
    private val lock: Any = Any()
    private val tasks = mutableListOf<Runnable>()
    private val timedTasks: MutableList<TimedTask> = mutableListOf()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queue(block)
    }

    override fun execute(block: Runnable) {
        queue(block)
    }

    override fun queue(block: Runnable) {
        lock(lock) { tasks += block }
    }

    override fun invokeOnTimeout(
        timeMillis: Long,
        block: Runnable,
        context: CoroutineContext
    ): DisposableHandle {
        val task = TimedTask(now().milliseconds + timeMillis.milliseconds, null, block)
        lock(lock) { timedTasks += task }
        return DisposableHandle { lock(lock) { timedTasks -= task } }
    }

    override fun scheduleResumeAfterDelay(
        timeMillis: Long,
        continuation: CancellableContinuation<Unit>
    ) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    /** Executes any pending timed ([delay]) or queued ([queue]) tasks. */
    internal fun executePending(availableTime: Duration) {
        try {
            val startTime = now().milliseconds
            executeTimedTasks(startTime, availableTime)
            executeQueuedTasks(startTime, availableTime)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun scheduleResumeAfterDelay(
        time: Duration,
        continuation: CancellableContinuation<Unit>
    ) {
        val task = TimedTask(now().milliseconds + time, continuation, null)
        continuation.invokeOnCancellation { task.exception = it }
        lock(lock) { timedTasks += task }
    }

    private fun executeTimedTasks(startTime: Duration, availableTime: Duration) {
        while (true) {
            val item =
                lock(lock) {
                    if (timedTasks.isNotEmpty() && startTime >= timedTasks.first().time) {
                        timedTasks.removeFirst()
                    } else {
                        null
                    }
                } ?: break
            item.exception?.let { exception ->
                if (item.continuation?.isActive == true) {
                    item.continuation.resumeWithException(exception)
                    item.callback?.let { exception.printStackTrace() }
                }
            }
                ?: run {
                    if (item.continuation?.isActive == true) {
                        item.continuation.resume(Unit)
                        item.callback?.run()
                    }
                }
            if (now().milliseconds - startTime >= availableTime) {
                break
            }
        }
    }

    private fun executeQueuedTasks(startTime: Duration, availableTime: Duration) {
        while (true) {
            val task =
                lock(lock) {
                    if (tasks.isNotEmpty()) {
                        tasks.removeFirst()
                    } else {
                        null
                    }
                } ?: break
            task.run()
            if (now().milliseconds - startTime >= availableTime) {
                break
            }
        }
    }

    override fun toString(): String = "KtRenderingThreadDispatcher"

    private class TimedTask(
        val time: Duration,
        val continuation: CancellableContinuation<Unit>?,
        val callback: Runnable?,
    ) {
        var exception: Throwable? = null
    }
}

/** Executes tasks on the main rendering threads. See [RenderingThreadDispatcher] */
class MainDispatcher private constructor(unit: Unit = Unit) : RenderingThreadDispatcher() {
    override val immediate: MainCoroutineDispatcher = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        !KtScope.isOnRenderingThread()

    internal companion object : SingletonBase<MainDispatcher, Unit>(::MainDispatcher)
}
