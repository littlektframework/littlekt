package com.littlekt.log

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
actual object DefaultLogOutput : Logger.Output {
    actual override fun output(logger: Logger, level: Logger.Level, msg: Any?) =
        Logger.ConsoleLogOutput.output(logger, level, msg)
}