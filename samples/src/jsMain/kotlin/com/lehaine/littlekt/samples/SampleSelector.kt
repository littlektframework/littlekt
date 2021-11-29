package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.LittleKt
import com.lehaine.littlekt.LittleKtContext
import com.lehaine.littlekt.createLittleKtApp
import kotlinx.browser.document
import kotlinx.html.DIV
import kotlinx.html.button
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.canvas
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.style

/**
 * @author Colton Daily
 * @date 11/22/2021
 */

var lastContext: LittleKtContext? = null
fun main() {
    document.body!!.append {
        div {
            addSample("Display Test") { DisplayTest(it) }
            addSample("Frame Buffer Test") { FrameBufferTest(it) }
        }
    }
}

fun DIV.addSample(title: String, gameBuilder: (app: Application) -> LittleKt) {
    button {
        +title
        onClickFunction = {
            lastContext?.close()
            document.getElementById("canvas")?.remove()
            document.getElementById("canvas-container")!!.append {
                canvas {
                    id = "canvas"
                    width = "1024"
                    height = "576"
                    style = "border:1px solid #000000;"
                }
            }

            lastContext = createLittleKtApp {
                this.title = title
            }.start(gameBuilder)
        }

    }

}