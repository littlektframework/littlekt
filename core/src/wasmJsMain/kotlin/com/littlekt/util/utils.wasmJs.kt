package com.littlekt.util

@JsName("Date")
external class JsDate {
    companion object {
        fun now(): Double
    }
}
actual fun epochMillis(): Long = JsDate.now().toLong()

@JsFun("() => performance.now()")
external fun getPerformanceNow(): Double

actual fun now(): Double = getPerformanceNow()