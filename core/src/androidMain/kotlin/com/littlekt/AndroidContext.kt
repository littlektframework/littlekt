package com.littlekt

import com.littlekt.async.KT
import com.littlekt.async.KtScope
import com.littlekt.file.AndroidKeyValueStorage
import com.littlekt.file.AndroidAssetsVfs
import com.littlekt.file.AndroidApplicationVfs
import com.littlekt.file.AndroidUrlVfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.input.AndroidInput
import com.littlekt.log.Logger
import com.littlekt.log.setLevel
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.now
import ffi.globalMemory
import io.ygdrasil.wgpu.WGPULogCallback
import io.ygdrasil.wgpu.WGPULogLevel_Debug
import io.ygdrasil.wgpu.WGPULogLevel_Error
import io.ygdrasil.wgpu.WGPULogLevel_Info
import io.ygdrasil.wgpu.WGPULogLevel_Trace
import io.ygdrasil.wgpu.WGPULogLevel_Warn
import io.ygdrasil.wgpu.wgpuSetLogCallback
import io.ygdrasil.wgpu.wgpuSetLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.lazy
import kotlin.time.Duration.Companion.milliseconds

internal class AndroidContext(
    override val configuration: AndroidConfiguration
) : Context() {
    override val platform = Platform.ANDROID
    override val stats by lazy { AppStats() }
    override val graphics by lazy { AndroidGraphics(this) }
    override val input by lazy { AndroidInput() }
    override val logger = Logger(configuration.title).apply {
        setLevel(Logger.Level.TRACE)
    }
    override val vfsResources by lazy { AndroidAssetsVfs(this, logger) }
    override val resourcesVfs: VfsFile = vfsResources.root
    override val vfsUrl by lazy { AndroidUrlVfs(this, androidContext, logger) }
    override val urlVfs: VfsFile = vfsUrl.root
    override val vfsApplication by lazy { AndroidApplicationVfs(this, logger) }
    override val applicationVfs: VfsFile = vfsApplication.root
    override val kvStorage by lazy { AndroidKeyValueStorage(androidContext) }
    override val clipboard by lazy { AndroidClipboard(androidContext) }
    private var closed = false
    val androidContext get() = configuration.surfaceView.context

    init {
        if (configuration.enableWGPULogging) {
            initLogging()
        }
    }

    override fun start(build: (app: Context) -> ContextListener) {
        check(configuration.surfaceView.holder.surface.isValid) { "SurfaceView surface must be valid" }
        KtScope.launch {
            graphics.handleSurfaceCreated(configuration.surfaceView.holder.surface)
            build(this@AndroidContext).run {
                start()
                resizeCalls.fastForEach { it.invoke(graphics.width, graphics.height) }
            }
        }
    }

    fun update(): Boolean {
        EngineStats.resetPerFrameCounts()

        invokeAnyRunnable()

        calcFrameTimes(now().milliseconds)
        Dispatchers.KT.executePending(available)

        input.update()
        stats.update(dt)

        updateCalls.fastForEach { update -> update(dt) }
        postUpdateCalls.fastForEach { postUpdate -> postUpdate(dt) }

        input.reset()

        return closed.not().also { if (closed) destroy() }
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable -> postRunnable.invoke() }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        closed = true
    }

    override fun destroy() {
        KtScope.launch { releaseCalls.fastForEach { release -> release() } }
    }

    private fun initLogging() {
        val callback = WGPULogCallback.allocate(globalMemory) { level, cMessage, userdata ->
            val message = cMessage?.data?.toKString(cMessage.length) ?: "empty message"
            when (level) {
                WGPULogLevel_Error -> logger.error { message }
                WGPULogLevel_Warn -> logger.warn { message }
                WGPULogLevel_Info -> logger.info { message }
                WGPULogLevel_Debug -> logger.debug { message }
                WGPULogLevel_Trace -> logger.trace { message }
                else -> logger.warn { "Unknown log level $level with message $message" }
            }
        }
        wgpuSetLogLevel(WGPULogLevel_Trace)
        wgpuSetLogCallback(callback, globalMemory.bufferOfAddress(callback.handler).handler)
    }

    fun dispatchResize() {
        resizeCalls.fastForEach { it.invoke(graphics.width, graphics.height) }
    }
}