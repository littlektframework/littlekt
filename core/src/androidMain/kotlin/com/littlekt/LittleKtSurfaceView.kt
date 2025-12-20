package com.littlekt

import android.annotation.SuppressLint
import android.content.Context as AndroidContext
import com.littlekt.Context as LittleKtContext
import com.littlekt.AndroidContext as LittleKtAndroidContext
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.littlekt.async.KtScope
import com.littlekt.input.AndroidInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LittleKtSurfaceView @JvmOverloads constructor(
    context: AndroidContext, attrs: AttributeSet? = null, defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback {
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val choreographer by lazy { Choreographer.getInstance() }
    private var app: LittleKtApp? = null
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val ctx = app?.context as? LittleKtAndroidContext
            if (ctx == null) {
                this@LittleKtSurfaceView.stopRendering()
                return
            }
            val nextFrameRequested = ctx.update()
            if (nextFrameRequested) {
                choreographer.postFrameCallback(this)
            } else {
                this@LittleKtSurfaceView.stopRendering()
            }
        }
    }
    private val androidGraphics get() = app?.context?.graphics as? AndroidGraphics

    init {
        KtScope.initiate()
        holder.setFormat(PixelFormat.RGBA_8888)
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    var game: ((app: LittleKtContext) -> ContextListener)? = null
        set(value) {
            check(value != null) { "game must not be null" }
            check(field == null) { "game can be set only once per view instance" }
            field = value
            if (holder.surface.isValid) {
                startApp(value)
            }
        }

    private fun startApp(game: ((app: LittleKtContext) -> ContextListener)) {
        viewScope.launch {
            app = createLittleKtApp { surfaceView = this@LittleKtSurfaceView }.apply {
                start(game)
                startRendering()
            }
        }
    }
    private fun LittleKtApp.startRendering() {
        choreographer.removeFrameCallback(frameCallback)
        choreographer.postFrameCallback(frameCallback)
    }

    private fun stopRendering() {
        choreographer.removeFrameCallback(frameCallback)
    }

    fun release() {
        stopRendering()
        app?.context?.close()
        androidGraphics?.release()
        app = null
        viewScope.cancel()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewScope.launch {
            androidGraphics?.handleSurfaceCreated(holder.surface)
            app?.startRendering() ?: game?.let(::startApp)
        }
        requestFocus()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        app?.context?.let { (it as? LittleKtAndroidContext)?.dispatchResize() }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopRendering()
        androidGraphics?.handleSurfaceDestroyed()
    }

    private val androidInput get() = app?.context?.input as? AndroidInput

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return androidInput?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return androidInput?.onGenericMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return androidInput?.onKeyDown(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return androidInput?.onKeyUp(event) ?: super.onKeyUp(keyCode, event)
    }
}