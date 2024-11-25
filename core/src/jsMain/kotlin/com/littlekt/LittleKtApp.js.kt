package com.littlekt

import io.ygdrasil.wgpu.PowerPreference

/** Properties related to creating a [LittleKtApp] */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var resourcesDir: String = "./"
    var applicationDir: String = "./"
    var powerPreference = PowerPreference.highPerformance
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
                title = props.title,
                canvasId = props.canvasId,
                resourcesPath = props.resourcesDir,
                applicationPath = props.applicationDir,
                powerPreference = props.powerPreference
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
    val resourcesPath: String = "./",
    val applicationPath: String = "./",
    val powerPreference: PowerPreference = PowerPreference.highPerformance
) : ContextConfiguration()

