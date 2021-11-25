package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
expect class PlatformContext(configuration: ApplicationConfiguration) : Application {
    override val platform: Platform
    override val configuration: ApplicationConfiguration
    override val graphics: Graphics
    override val input: Input
    override val logger: Logger
    override val fileHandler: FileHandler
    override val engineStats: EngineStats
    override fun start(gameBuilder: (app: Application) -> LittleKt)
    override fun close()
    override fun destroy()
}