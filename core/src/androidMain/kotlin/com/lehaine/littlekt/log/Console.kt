package com.lehaine.littlekt.log

import android.util.Log

actual object Console : BaseConsole() {

    override fun log(kind: Kind, vararg msg: Any?) {
        val tag = "LittleKt"
        val str = logToString(kind, *msg)
        when (kind) {
            Kind.ERROR -> Log.e(tag, str)
            Kind.WARN -> Log.w(tag, str)
            Kind.INFO -> Log.i(tag, str)
            Kind.DEBUG -> Log.d(tag, str)
            Kind.TRACE -> Log.v(tag, str)
            Kind.LOG -> Log.i(tag, str)
        }
    }
}