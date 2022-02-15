package com.lehaine.littlekt.async

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.ContinuationInterceptor

internal var mainThread = Thread.currentThread()
actual fun CoroutineScope.isOnRenderingThread() =
    coroutineContext[ContinuationInterceptor.Key] is RenderingThreadDispatcher &&
            Thread.currentThread() === mainThread
