package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 10/4/2021
 */
actual class PlatformApplication actual constructor(configuration: ApplicationConfiguration) : Application {
    actual override val configuration: ApplicationConfiguration
        get() = TODO("Not yet implemented")
    actual override val graphics: Graphics
        get() = TODO("Not yet implemented")
    actual override val input: Input
        get() = TODO("Not yet implemented")
    actual override val logger: Logger
        get() = TODO("Not yet implemented")
    actual override val assetManager: AssetManager
        get() = TODO("Not yet implemented")
    actual override val fileHandler: FileHandler
        get() = TODO("Not yet implemented")

    actual override fun start(gameBuilder: (app: Application) -> LittleKt) {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    actual override fun destroy() {
        TODO("Not yet implemented")
    }

}