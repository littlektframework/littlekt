package com.lehaine.littlekt.log

import java.text.SimpleDateFormat

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
actual object Console : BaseConsole() {
    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
    override fun log(kind: Kind, vararg msg: Any?) {
        val stream = if (kind == Kind.ERROR) System.err else System.out
        stream.println(logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String = buildString {
        val color = kind.color
        if (color != null) appendFgColor(color)
        synchronized(dateFormat) {
            append("[${dateFormat.format(System.currentTimeMillis())}] - ")
        }
        append("[Thread: ${Thread.currentThread().id}] - ")
        msg.joinTo(this, ": ")
        if (color != null) appendReset()
    }
}