package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color
import com.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 4/5/2024
 */
fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        println("args: ${args.joinToString { it }}")
    }
    Logger.setLevels(Logger.Level.DEBUG)

    val arg = args.firstOrNull()

    val exampleInfo = if (arg == null) availableExamples["-triangle"]!! else availableExamples[arg]
    if (exampleInfo == null) {
        println("Unknown example: $arg")
        println("Available examples:")
        println(availableExamples.keys.joinToString("\n"))
        return
    }
    val (title, example) = exampleInfo
    createApp("$title Example", example)
}

private fun createApp(title: String, start: (Context) -> ContextListener) =
    createLittleKtApp {
            width = 960
            height = 540
            vSync = true
            this.title = title
            backgroundColor = Color.DARK_GRAY
            traceWgpu = false
            enableWGPULogging = false
        }
        .start(start)
