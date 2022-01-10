package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LittleKtApp(
    val context: Context
) {
    suspend fun start(gameBuilder: (app: Context) -> ContextListener): LittleKtApp {
        context.start(gameBuilder)
        return this
    }

    suspend fun close(): LittleKtApp {
        context.close()
        return this
    }
}

expect class LittleKtProps

expect fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp