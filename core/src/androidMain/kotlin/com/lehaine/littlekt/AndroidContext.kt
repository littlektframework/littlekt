package com.lehaine.littlekt

import android.graphics.Point
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.MainDispatcher
import com.lehaine.littlekt.file.AndroidVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.input.AndroidInput
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.internal.now
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AndroidContext(override val configuration: AndroidConfiguration) : Context() {
    override val stats: AppStats = AppStats()
    override val graphics: AndroidGraphics = AndroidGraphics(stats.engineStats)
    override val input: AndroidInput = AndroidInput()
    override val logger: Logger = Logger(configuration.title)
    override val vfs: AndroidVfs =
        AndroidVfs(
            configuration.activity,
            configuration.activity.getPreferences(android.content.Context.MODE_PRIVATE),
            this, logger, "./.storage", "."
        )
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")
    override val platform: Platform = Platform.ANDROID

    override fun start(build: (app: Context) -> ContextListener) {
        graphics.onCreate = {
            val size = Point()
            configuration.activity.windowManager.defaultDisplay.getSize(size)
            val listener = build(this@AndroidContext)
            listener.run {
                KtScope.launch {
                    start()
                    resizeCalls.forEach {
                        it.invoke(size.x, size.y)
                    }
                }
            }
        }
        graphics.onResize = { width, height ->
            KtScope.launch {
                resizeCalls.forEach {
                    it.invoke(width, height)
                }
            }
        }
        graphics.onDrawFrame = {
            KtScope.launch {
                calcFrameTimes(now().milliseconds)
                MainDispatcher.INSTANCE.executePending(available)
                update(dt)
            }
        }

        val surfaceView = LittleKtSurfaceView(configuration.activity).apply {
            setRenderer(graphics)
        }
        configuration.activity.setContentView(surfaceView)
    }

    private suspend fun update(dt: Duration) {
        //     audioContext.update()

        stats.engineStats.resetPerFrameCounts()

        //   invokeAnyRunnable()

        //input.update()
        stats.update(dt)
        renderCalls.fastForEach { render -> render(dt) }
        postRenderCalls.fastForEach { postRender -> postRender(dt) }

        //  input.reset()
    }


    override fun close() {

    }

    override fun destroy() {

    }

}