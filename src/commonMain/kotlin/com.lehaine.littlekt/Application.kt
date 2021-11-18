package com.lehaine.littlekt

import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.io.AssetManager
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
interface Application {
    val configuration: ApplicationConfiguration

    val graphics: Graphics

    val input: Input

    val logger: Logger

    val assetManager: AssetManager

    val fileHandler: FileHandler

    val platform: Platform

    fun start(gameBuilder: (app: Application) -> LittleKt)

    fun close()

    fun destroy()
}