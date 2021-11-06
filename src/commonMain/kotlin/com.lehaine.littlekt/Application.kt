package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
interface Application {

    val graphics: Graphics

    fun start(game: LittleKt)

    fun exit()
}