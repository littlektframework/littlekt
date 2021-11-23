package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var title: String = "LitteKt"
    var vSync: Boolean = true
}

actual fun createLittleKtApp(action: LittleKtProps.() -> ((Application) -> LittleKt)): LittleKtContext {
    val props = LittleKtProps()
    val gameBuilder = props.action()
    return LittleKtContext(
        ApplicationConfiguration(
            props.title,
            props.width,
            props.height,
            props.vSync
        ), gameBuilder
    )
}