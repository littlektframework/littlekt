package com.littlekt

actual class LittleKtProps {
    var surfaceView: LittleKtSurfaceView? = null
}

actual suspend fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    requireNotNull(props.surfaceView)
    return LittleKtApp(
        AndroidContext(
            AndroidConfiguration(surfaceView = props.surfaceView ?: error("SurfaceView is required"))
        )
    )
}

class AndroidConfiguration(
    override val title: String = "LittleKt", val surfaceView: LittleKtSurfaceView, val enableWGPULogging: Boolean = true
) : ContextConfiguration()