package com.lehaine.littlekt.view

import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class LittleKtSurfaceView(context: android.content.Context) : GLSurfaceView(context) {

    init {
        setEGLContextFactory(ContextFactory())
    }

    class ContextFactory : EGLContextFactory {
        private var target = 3

        override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {
            val context = egl.eglCreateContext(
                display,
                eglConfig,
                EGL10.EGL_NO_CONTEXT,
                intArrayOf(EGL_CONTEXT_CLIENT_VERSION, target, EGL10.EGL_NONE)
            )
            val success = egl.checkError()
            if (!success || context == null) {
                Log.w("LittleKt", "ES $target is not available. Falling back to ES 2.")
                target = 2
                return createContext(egl, display, eglConfig)
            }
            Log.i("LittleKt", "Using ES $target.")
            return context
        }

        private fun EGL10.checkError(): Boolean {
            var error: Int = eglGetError()
            var result = true
            while (error != EGL10.EGL_SUCCESS) {
                result = false
                Log.e("LittleKt", "EGL error: 0x${error.toString(16)}")
                error = eglGetError()
            }
            return result
        }

        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            egl.eglDestroyContext(display, context)
        }

        companion object {
            private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        outAttrs?.let {
            it.imeOptions = it.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            it.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }

        return object : BaseInputConnection(this, false) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength == 1 && afterLength == 0) {
                    /*
                    * In Jelly Bean, they don't send key events for delete. Instead, they send beforeLength = 1, afterLength = 0. So,
                    * we'll just simulate what it used to do.
                    */
                    sendDownUpKeyEventForBackwardCompatibility(KeyEvent.KEYCODE_DEL)
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            private fun sendDownUpKeyEventForBackwardCompatibility(code: Int) {
                val eventTime = SystemClock.uptimeMillis()
                super.sendKeyEvent(
                    KeyEvent(
                        eventTime,
                        eventTime,
                        KeyEvent.ACTION_DOWN,
                        code,
                        0,
                        0,
                        KeyCharacterMap.VIRTUAL_KEYBOARD,
                        0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
                    )
                )
                super.sendKeyEvent(
                    KeyEvent(
                        SystemClock.uptimeMillis(),
                        eventTime,
                        KeyEvent.ACTION_UP,
                        code,
                        0,
                        0,
                        KeyCharacterMap.VIRTUAL_KEYBOARD,
                        0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
                    )
                )
            }
        }
    }
}