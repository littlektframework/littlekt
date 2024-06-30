package com.littlekt.async

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineScope

internal var mainThread = Thread.currentThread()

/** Returns true if the coroutine was launched from the rendering thread dispatcher. */
actual fun CoroutineScope.isOnRenderingThread(): Boolean =
    coroutineContext[ContinuationInterceptor.Key] is RenderingThreadDispatcher &&
        Thread.currentThread() === mainThread
