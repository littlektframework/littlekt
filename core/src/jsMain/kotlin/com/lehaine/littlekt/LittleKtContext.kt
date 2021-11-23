package com.lehaine.littlekt

import kotlinx.browser.window

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var rootPath: String = window.location.protocol
}

actual fun createLittleKtApp(action: LittleKtProps.() -> ((Application) -> LittleKt)): LittleKtContext {
    val props = LittleKtProps()
    val gameBuilder = props.action()
    return LittleKtContext(
        ApplicationConfiguration(
            props.title,
            props.canvasId,
            props.rootPath
        ), gameBuilder
    )
}