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

actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtContext {
    val props = LittleKtProps().apply(action)
    return LittleKtContext(
        ApplicationConfiguration(
            props.title,
            props.width,
            props.height,
            props.vSync
        )
    )
}