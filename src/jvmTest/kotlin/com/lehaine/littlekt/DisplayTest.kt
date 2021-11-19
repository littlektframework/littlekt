package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.io.get

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application), InputProcessor {

    val batch = SpriteBatch(application)
    val input get() = application.input

    val texture by application.fileHandler.get<Texture>("person.png")

    override fun create() {
        println("create")
        input.inputProcessor = this
    }

    override fun render(dt: Float) {
        batch.begin()
        batch.draw(texture, 5f, 5f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        println("resize to $width,$height")
    }

    override fun keyDown(key: Key): Boolean {
        println("Key down: $key")
        return false
    }

    override fun keyUp(key: Key): Boolean {
        println("key up $key")
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        println("Key typed $character")
        return false
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button $pointer pressed $screenX,$screenY")
        return false
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button $pointer released $screenX,$screenY")
        return false
    }

    override fun touchDragged(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        println("Mouse button dragged to $screenX,$screenY")
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        println("Scrolled $amountX,$amountY")
        return false
    }
}

fun main(args: Array<String>) {
    LittleKtAppBuilder(
        configBuilder = { ApplicationConfiguration("Display Test", 960, 540, true) },
        gameBuilder = { DisplayTest(it) })
        .start()
}