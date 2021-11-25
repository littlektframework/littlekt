package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.io.FileHandler
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
interface Application {
    val configuration: ApplicationConfiguration

    val graphics: Graphics

    val gl: GL get() = graphics.gl

    val input: Input

    val logger: Logger

    val fileHandler: FileHandler

    val platform: Platform

    val engineStats: EngineStats

    fun start(gameBuilder: (app: Application) -> LittleKt)

    fun close()

    fun destroy()
}