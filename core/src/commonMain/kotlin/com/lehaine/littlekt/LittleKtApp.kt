package com.lehaine.littlekt

import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.MainDispatcher
import com.lehaine.littlekt.util.internal.now
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LittleKtApp(val context: Context) {

    fun start(gameBuilder: (app: Context) -> ContextListener): LittleKtApp {
        KtScope.initiate()
        fun getCounterTimePerFrame() = (1_000_000.0 / context.stats.fps).microseconds

        KtScope.launch {
            context.initialize(gameBuilder)
        }
        var lastFrame = now().milliseconds
        while (!context.shouldClose) {
            val time = now().milliseconds
            val dt = time - lastFrame
            val available = getCounterTimePerFrame() - dt
            MainDispatcher.INSTANCE.executePending(available)
            lastFrame = time
            KtScope.launch {
                context.update(dt)
            }
        }
        KtScope.launch {
            context.destroy()
        }

        return this
    }

    fun close(): LittleKtApp {
        context.close()
        return this
    }
}

expect class LittleKtProps

expect fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp