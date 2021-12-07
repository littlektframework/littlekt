package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.file.FileHandler
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
interface Context {

    enum class Platform {
        DESKTOP,
        JS,
        ANDROID,
        IOS
    }

    val stats: AppStats

    val configuration: ContextConfiguration

    val graphics: Graphics

    val gl: GL get() = graphics.gl

    val input: Input

    val logger: Logger

    val fileHandler: FileHandler

    val platform: Platform

    fun start(gameBuilder: (app: Context) -> ContextListener)

    fun close()

    fun destroy()
}