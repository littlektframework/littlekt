package com.lehaine.littlekt.async

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.util.internal.SingletonBase
import com.lehaine.littlekt.util.internal.lock
import com.lehaine.littlekt.util.internal.now
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 1/9/2022
 */

@OptIn(InternalCoroutinesApi::class)
interface KtDispatcher : CoroutineContext, Delay {
    val lock: Any
    val tasks: MutableList<Runnable>
    val timedTasks: MutableList<TimedTask>
    fun execute(block: Runnable)

    fun queue(block: Runnable) {
        lock(lock) {
            tasks += block
        }
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        scheduleResumeAfterDelay(timeMillis.toDouble().milliseconds, continuation)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val task = TimedTask(now().milliseconds + timeMillis.milliseconds, null, block)
        lock(lock) { timedTasks += task }
        return DisposableHandle { lock(lock) { timedTasks -= task } }
    }

    private fun scheduleResumeAfterDelay(time: Duration, continuation: CancellableContinuation<Unit>) {
        val task = TimedTask(now().milliseconds + time, continuation, null)
        continuation.invokeOnCancellation {
            task.exception = it
        }
        lock(lock) { timedTasks += task }
    }

    fun executeTimedTasks(startTime: Duration, availableTime: Duration) {
        while (true) {
            val item = lock(lock) {
                if (timedTasks.isNotEmpty() && startTime >= timedTasks.first().time) {
                    timedTasks.removeFirst()
                } else {
                    null
                }
            } ?: break
            item.exception?.let { exception ->
                item.continuation?.resumeWithException(exception)
                item.callback?.let {
                    exception.printStackTrace()
                }
            } ?: run {
                item.continuation?.resume(Unit)
                item.callback?.run()
            }
            if (now().milliseconds - startTime >= availableTime) {
                break
            }
        }
    }

    fun executeQueuedTasks(startTime: Duration, availableTime: Duration) {
        while (true) {
            val task = lock(lock) {
                if (tasks.isNotEmpty()) {
                    tasks.removeFirst()
                } else {
                    null
                }
            } ?: break
            if (now().milliseconds - startTime >= availableTime) {
                break
            }
        }
    }

    fun executePending(availableTime: Duration) {
        try {
            val startTime = now().milliseconds
            executeTimedTasks(startTime, availableTime)
            executeQueuedTasks(startTime, availableTime)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    class TimedTask(val time: Duration, val continuation: CancellableContinuation<Unit>?, val callback: Runnable?) {
        var exception: Throwable? = null
    }
}

class AsyncKtDispatcher : CoroutineDispatcher(), KtDispatcher {
    override val lock = Any()

    override val tasks = mutableListOf<Runnable>()
    override val timedTasks = mutableListOf<KtDispatcher.TimedTask>()

    override fun execute(block: Runnable) {
        lock(lock) {
            tasks += block
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        execute(block)
    }
}

sealed class RenderingThreadDispatcher(val context: Context) : MainCoroutineDispatcher(), KtDispatcher {
    override val lock: Any = Any()
    override val tasks = mutableListOf<Runnable>()
    override val timedTasks: MutableList<KtDispatcher.TimedTask> = mutableListOf()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        execute(block)
    }

    override fun execute(block: Runnable) {
        context.postRunnable { block.run() }
    }

    override fun toString(): String = "KtRenderingThreadDispatcher"
}

class MainDispatcher private constructor(context: Context) : RenderingThreadDispatcher(context) {
    override val immediate: MainCoroutineDispatcher = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = KtAsync.isOnRenderingThread()

    internal companion object : SingletonBase<MainDispatcher, Context>(::MainDispatcher)
}