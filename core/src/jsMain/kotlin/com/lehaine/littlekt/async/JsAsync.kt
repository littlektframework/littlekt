package com.lehaine.littlekt.async

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.ContinuationInterceptor

actual fun CoroutineScope.isOnRenderingThread() =
    coroutineContext[ContinuationInterceptor.Key] is RenderingThreadDispatcher