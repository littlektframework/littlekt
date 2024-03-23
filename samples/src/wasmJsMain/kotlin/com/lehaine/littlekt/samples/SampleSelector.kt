package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.LittleKtApp
import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color
import kotlinx.browser.document
import kotlinx.dom.appendElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement

/**
 * @author Colton Daily
 * @date 10/02/2023
 */

var lastApp: LittleKtApp? = null

fun main() {
    document.body?.appendElement("div") {
        addSample("Display Test") { DisplayTest(it) }
        addSample("Mutable Atlas Test") { MutableAtlasTest(it) }
        addSample("Texture Array Sprite Batch Test") { TextureArraySpriteBatchTest(it) }
        addSample("Tiled Map Test") { TiledMapTest(it) }
        addSample("LDtk Map Test") { LDtkMapTest(it) }
        addSample("Pixel Smooth Camera Test") { PixelSmoothCameraTest(it) }
        addSample("Gesture Controller Test") { GestureControllerTest(it) }
    }
}

fun Element.addSample(title: String, gameBuilder: (app: Context) -> ContextListener) {
    appendElement("button") {
        this as HTMLButtonElement
        textContent = title
        onclick = {
            document.getElementById("canvas")?.remove()
            document.getElementById("canvas-container")!!.appendElement("canvas") {
                this as HTMLCanvasElement
                id = "canvas"
                width = 960
                height = 540
                setAttribute("style", "border:1px solid #000000;")
            }

            lastApp?.close()
            lastApp = createLittleKtApp {
                this.title = title
                backgroundColor = Color.DARK_GRAY
            }.also {
                it.start(gameBuilder)
                println("Start $title")
            }
            null
        }
    }
}