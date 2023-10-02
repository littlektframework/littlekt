package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Color

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var assetsDir: String = "./"
    var backgroundColor: Color = Color.CLEAR
}

actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    props.action()
    return LittleKtApp(
        WebGLContext(
            JsConfiguration(
                props.title,
                props.canvasId,
                props.assetsDir,
                props.backgroundColor
            )
        )
    )
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class JsConfiguration(
    override val title: String = "LittleKt - JS",
    val canvasId: String = "canvas",
    val rootPath: String = "./",
    val backgroundColor: Color = Color.CLEAR
) : ContextConfiguration()