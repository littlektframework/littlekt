package com.lehaine.littlekt.log

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
interface Logger {

    enum class LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    fun debug(tag: String = "", message: () -> String)
    fun debug(tag: String = "", exception: Throwable, message: () -> String)

    fun d(tag: String = "", message: () -> String) = debug(tag, message)
    fun d(tag: String = "", exception: Throwable, message: () -> String) = debug(tag, exception, message)

    fun info(tag: String = "", message: () -> String)
    fun info(tag: String = "", exception: Throwable, message: () -> String)

    fun i(tag: String = "", message: () -> String) = info(tag, message)
    fun i(tag: String = "", exception: Throwable, message: () -> String) = info(tag, exception, message)

    fun warn(tag: String = "", message: () -> String)
    fun warn(tag: String = "", exception: Throwable, message: () -> String)

    fun w(tag: String = "", message: () -> String) = warn(tag, message)
    fun w(tag: String = "", exception: Throwable, message: () -> String) = warn(tag, exception, message)

    fun error(tag: String = "", message: () -> String)
    fun error(tag: String = "", exception: Throwable, message: () -> String)

    fun e(tag: String = "", message: () -> String) = error(tag, message)
    fun e(tag: String = "", exception: Throwable, message: () -> String) = error(tag, exception, message)

    var rootLevel: LogLevel
}