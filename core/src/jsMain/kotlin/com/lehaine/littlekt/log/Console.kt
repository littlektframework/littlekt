package com.lehaine.littlekt.log

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
actual object Console : BaseConsole() {
    override fun log(kind: Kind, vararg msg: Any?) {
        when (kind) {
            Kind.ERROR -> console.error(*msg)
            Kind.WARN -> console.warn(*msg)
            Kind.INFO -> console.info(*msg)
            Kind.DEBUG -> console.log(*msg)
            Kind.TRACE -> console.log(*msg)
            Kind.LOG -> console.log(*msg)
        }
    }
}