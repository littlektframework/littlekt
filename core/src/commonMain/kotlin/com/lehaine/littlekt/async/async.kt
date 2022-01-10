package com.lehaine.littlekt.async

import com.lehaine.littlekt.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * @author Colton Daily
 * @date 1/9/2022
 */
object KtAsync : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = MainDispatcher.INSTANCE

    fun initiate(context: Context) {
        MainDispatcher.createInstance(context)
    }
}

val Dispatchers.KT get() = MainDispatcher.INSTANCE

suspend fun <T> onRenderingThread(block: suspend CoroutineScope.() -> T) =
    withContext(MainDispatcher.INSTANCE, block = block)

fun newAsyncContext() = AsyncKtDispatcher()

expect fun CoroutineScope.isOnRenderingThread(): Boolean