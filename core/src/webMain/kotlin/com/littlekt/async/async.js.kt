package com.littlekt.async

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

/** Returns true if the coroutine was launched from the rendering thread dispatcher. */
actual fun CoroutineScope.isOnRenderingThread() =
    coroutineContext[ContinuationInterceptor.Key] is RenderingThreadDispatcher

expect suspend fun <T> Promise<*>.await() : T