package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
open class LittleKt(val application: Application) {

    val fileHandler get() = application.fileHandler

    open fun create() {}

    open fun render(dt: Float) {}

    open fun resize(width: Int, height: Int) {}

    open fun resume() {}

    open fun pause() {}

    open fun dispose() {}
}