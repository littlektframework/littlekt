package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LittleKtAppBuilder(
    val configBuilder: () -> ApplicationConfiguration,
    val gameBuilder: (app: Application) -> LittleKt
) {

    fun start() {
        val app = PlatformApplication(configBuilder())
        app.start(gameBuilder)
    }
}