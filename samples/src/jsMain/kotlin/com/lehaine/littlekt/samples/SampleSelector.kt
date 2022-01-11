package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.LittleKtApp
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

var lastApp: LittleKtApp? = null

fun main() {
    document.body!!.append {
        div {
            addSample("Display Test") { DisplayTest(it) }
        }
    }
}

fun DIV.addSample(title: String, gameBuilder: (app: Context) -> ContextListener) {
    button {
        +title
        onClickFunction = {
            document.getElementById("canvas")?.remove()
            document.getElementById("canvas-container")!!.append {
                canvas {
                    id = "canvas"
                    width = "1024"
                    height = "576"
                    style = "border:1px solid #000000;"
                }
            }

            lastApp?.close()
            lastApp = createLittleKtApp {
                this.title = title
            }.also {
                it.start(gameBuilder)
            }
        }
    }
}