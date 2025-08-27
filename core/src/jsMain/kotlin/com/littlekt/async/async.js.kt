package com.littlekt.async

import kotlinx.coroutines.await
import kotlin.js.Promise

actual suspend fun <T> Promise<*>.await(): T  = await() as T