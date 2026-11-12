package com.littlekt

import com.littlekt.async.await
import com.littlekt.audio.WebAudio
import com.littlekt.graphics.webgpu.GPUAdapter
import com.littlekt.graphics.webgpu.GPUDevice
import com.littlekt.graphics.webgpu.navigator
import kotlinx.browser.window

/** Properties related to creating a [LittleKtApp] */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var canvasId: String = "canvas"
    var title: String = "LitteKt"
    var resourcesDir: String = "./"
    var applicationDir: String = "./"
    var powerPreference = PowerPreference.HIGH_POWER

    /**
     * The maximum number of times each audio clip can play at once. Once reached, the oldest play
     * of that clip restarts.
     */
    var maxAudioClipInstances = WebAudio.DEFAULT_MAX_INSTANCES_PER_CLIP
}

/**
 * Creates a new [LittleKtApp] containing [LittleKtProps] as the [ContextConfiguration] for building
 * a [Context].
 */
actual suspend fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    props.action()
    val gpuAdapter = navigator.gpu.requestAdapter().await<GPUAdapter>()
    val gpuDevice = gpuAdapter.requestDevice(gpuDeviceDescriptor = null).await<GPUDevice>()

    return LittleKtApp(

        WebGPUContext(
            JsConfiguration(
                title = props.title,
                canvasId = props.canvasId,
                resourcesPath = props.resourcesDir,
                applicationPath = props.applicationDir,
                powerPreference = props.powerPreference,
                maxAudioClipInstances = props.maxAudioClipInstances,
            ),
            gpuAdapter,
            gpuDevice
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
    val powerPreference: PowerPreference = PowerPreference.HIGH_POWER,
    val maxAudioClipInstances: Int = WebAudio.DEFAULT_MAX_INSTANCES_PER_CLIP,
) : ContextConfiguration()

val PowerPreference.nativeFlag: String
    get() =
        when (this) {
            PowerPreference.LOW_POWER -> "low-power"
            PowerPreference.HIGH_POWER -> "high-performance"
        }