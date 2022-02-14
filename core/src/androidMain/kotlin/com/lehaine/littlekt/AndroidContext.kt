package com.lehaine.littlekt

import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.MainDispatcher
import com.lehaine.littlekt.async.mainThread
import com.lehaine.littlekt.audio.AndroidAudioContext
import com.lehaine.littlekt.file.AndroidVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.input.AndroidInput
import com.lehaine.littlekt.view.LittleKtSurfaceView
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
            configuration.activity.assets,
            configuration.activity.getPreferences(android.content.Context.MODE_PRIVATE),
            this, logger, "./.storage", "."
        )
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")
    override val platform: Platform = Platform.ANDROID
    val audioContext: AndroidAudioContext = AndroidAudioContext(configuration.activity)

    init {
        KtScope.initiate()
        mainThread = Thread.currentThread()
        setupConfig()
    }

    private fun setupConfig() {
        val activity = configuration.activity
        val window = activity.window
        if (!configuration.showStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.decorView.systemUiVisibility = 0x1
            }
        }

        if (configuration.useWakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (configuration.useImmersiveMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.captionBar())
            } else {
                window.decorView.systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    fun resume() {
        setupConfig()
        audioContext.resume()
        graphics.resume()
    }

    fun pause() {
        audioContext.pause()
        graphics.pause()
    }

    override fun start(build: (app: Context) -> ContextListener) {
        graphics.onCreate = {
            KtScope.launch {
                InternalResources.createInstance(this@AndroidContext)
                InternalResources.INSTANCE.load()

                gl.clearColor(configuration.backgroundColor)

                val size = Point()
                configuration.activity.windowManager.defaultDisplay.getSize(size)

                val listener = build(this@AndroidContext)
                listener.run {
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
            calcFrameTimes(now().milliseconds)
            MainDispatcher.INSTANCE.executePending(available)
            KtScope.launch {
                update(dt)
            }
        }

        val surfaceView = LittleKtSurfaceView(configuration.activity).apply {
            setRenderer(graphics)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }.also { graphics.surfaceView }
        configuration.activity.setContentView(surfaceView)
    }

    private suspend fun update(dt: Duration) {

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