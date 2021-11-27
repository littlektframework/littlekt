package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LittleKtContext(
    val applicationConfiguration: ApplicationConfiguration
) {
    private val app = PlatformContext(applicationConfiguration)
    fun start(gameBuilder: (app: Application) -> LittleKt): LittleKtContext {
        app.start(gameBuilder)
        return this
    }

    fun close(): LittleKtContext {
        app.close()
        return this
    }
}

expect class LittleKtProps

expect fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtContext