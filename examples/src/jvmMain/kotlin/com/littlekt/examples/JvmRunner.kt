package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.coroutines.runBlocking

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

    val example = availableExamples[arg ?: "-triangle"]
    if (example == null) {
        println("Unknown example: $arg")
        println("Available examples:")
        availableExamples.values.groupBy { it.category }.forEach { (category, examples) ->
            println(category)
            examples.forEach { println("  ${it.key} - ${it.title}") }
        }
        return
    }
    createApp("${example.title} Example", example.create)
}

private fun createApp(title: String, start: (Context) -> ContextListener) = runBlocking {
        createLittleKtApp {
            width = 960
            height = 540
            this.title = title
            traceWgpu = false
            enableWGPULogging = false
        }
        .start(start)
}
