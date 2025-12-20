package com.littlekt.log

import android.util.Log

actual object Console : BaseConsole() {
    const val TAG = "LittleKt"

    override fun log(kind: Kind, vararg msg: Any?) {
        val error = msg.joinToString { it.toString() }
        when (kind) {
            Kind.ERROR -> Log.e(TAG, error)
            Kind.WARN -> Log.w(TAG, error)
            Kind.INFO -> Log.i(TAG, error)
            Kind.DEBUG -> Log.d(TAG, error)
            Kind.TRACE -> Log.d(TAG, error)
            Kind.LOG -> Log.d(TAG, error)
        }
    }
}