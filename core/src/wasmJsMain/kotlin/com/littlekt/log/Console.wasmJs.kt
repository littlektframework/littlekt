package com.littlekt.log

private external object console {
    fun log(vararg msg: JsAny?)
    fun warn(vararg msg: JsAny?)
    fun error(vararg msg: JsAny?)
    fun info(vararg msg: JsAny?)
}

actual object Console : BaseConsole() {
    override fun log(kind: Kind, vararg msg: Any?) {
        val errors = msg.map { it?.toString()?.toJsString() }.toJsArray()
        println(errors)
        when (kind) {
            Kind.ERROR -> console.error(errors)
            Kind.WARN -> console.warn(errors)
            Kind.INFO -> console.info(errors)
            Kind.DEBUG -> console.log(errors)
            Kind.TRACE -> console.log(errors)
            Kind.LOG -> console.log(errors)
        }
    }
}