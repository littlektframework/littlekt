package com.littlekt

import com.littlekt.graphics.Color

/** Properties related to creating a [LittleKtApp] */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var assetsDir: String = "./"
    var backgroundColor: Color = Color.CLEAR
    var powerPreference = PowerPreference.HIGH_POWER
}

/**
 * Creates a new [LittleKtApp] containing [LittleKtProps] as the [ContextConfiguration] for building
 * a [Context].
 */
actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    props.action()
    return LittleKtApp(
        WebGPUContext(
            JsConfiguration(
                props.title,
                props.canvasId,
                props.assetsDir,
                props.backgroundColor,
                props.powerPreference
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
    val backgroundColor: Color = Color.CLEAR,
    val powerPreference: PowerPreference = PowerPreference.HIGH_POWER
) : ContextConfiguration()

val PowerPreference.nativeFlag: String
    get() =
        when (this) {
            PowerPreference.LOW_POWER -> "low-power"
            PowerPreference.HIGH_POWER -> "high-performance"
        }
