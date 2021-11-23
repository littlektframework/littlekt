package com.lehaine.littlekt.log

import java.text.SimpleDateFormat
import java.util.logging.Level

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class JvmLogger(name: String) : Logger {
    private val log = java.util.logging.Logger.getLogger(name)
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")

    private fun LambdaMessage.withTag(tag: String): LambdaMessage {
        if (tag.isNotBlank()) {
            return { "[$tag] ${this.invoke()}" }
        } else {
            return { this.invoke() }
        }
    }

    private fun LambdaMessage.withTimestamp(): LambdaMessage {
        synchronized(dateFormat) {
            return { "${dateFormat.format(System.currentTimeMillis())}| ${this.invoke()}" }

        }
    }

    override fun debug(tag: String, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.DEBUG.ordinal) {
            log.log(Level.FINEST, message.withTimestamp().withTag(tag))
        }
    }

    override fun debug(tag: String, exception: Throwable, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.DEBUG.ordinal) {
            log.log(Level.FINEST, exception, message.withTimestamp().withTag(tag))
        }
    }

    override fun info(tag: String, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.INFO.ordinal) {
            log.log(Level.INFO, message.withTimestamp().withTag(tag))
        }
    }

    override fun info(tag: String, exception: Throwable, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.INFO.ordinal) {
            log.log(Level.INFO, exception, message.withTimestamp().withTag(tag))
        }
    }

    override fun warn(tag: String, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.WARN.ordinal) {
            log.log(Level.WARNING, message.withTimestamp().withTag(tag))
        }
    }

    override fun warn(tag: String, exception: Throwable, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.WARN.ordinal) {
            log.log(Level.WARNING, exception, message.withTimestamp().withTag(tag))
        }
    }

    override fun error(tag: String, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.ERROR.ordinal) {
            log.log(Level.SEVERE, message.withTimestamp().withTag(tag))
        }
    }

    override fun error(tag: String, exception: Throwable, message: () -> String) {
        if (rootLevel.ordinal <= Logger.LogLevel.ERROR.ordinal) {
            log.log(Level.SEVERE, exception, message.withTimestamp().withTag(tag))
        }
    }

    override var rootLevel: Logger.LogLevel = Logger.LogLevel.INFO
        set(value) {
            field = value
            val javaUtilsLog = when (value) {
                Logger.LogLevel.DEBUG -> Level.FINEST
                Logger.LogLevel.INFO -> Level.INFO
                Logger.LogLevel.WARN -> Level.WARNING
                Logger.LogLevel.ERROR -> Level.SEVERE
            }
            log.level = javaUtilsLog
        }
}


typealias LambdaMessage = () -> String