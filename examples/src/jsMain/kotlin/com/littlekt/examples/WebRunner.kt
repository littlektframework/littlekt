package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.UL
import kotlinx.html.a
import kotlinx.html.dom.append
import kotlinx.html.js.ul
import kotlinx.html.li
import org.w3c.dom.url.URL

/**
 * @author Colton Daily
 * @date 4/18/2024
 */
fun main() {
    document.getElementById("examples-list")?.append {
        ul { availableExamples.forEach { addExample(it.key, it.value.first) } }
    }

    val params = URL(window.location.href).searchParams

    val arg = params.get("example")
    val exampleInfo = if (arg == null) availableExamples["-triangle"]!! else availableExamples[arg]
    if (exampleInfo == null) {
        setStatus("Unknown example: $arg! Select an example from the list below.")
        return
    }
    val (title, example) = exampleInfo

    Logger.setLevels(Logger.Level.DEBUG)
    createApp("$title Example", example)
}

private fun setStatus(message: String) {
    document.getElementById("status")?.innerHTML = message
}

private fun UL.addExample(key: String, title: String) {
    li { a(href = "index.html?example=$key") { +title } }
}

private fun createApp(title: String, start: (Context) -> ContextListener) =
    createLittleKtApp {
            width = 960
            height = 540
            this.title = title
            canvasId = "canvas"
        }
        .start(start)
