package com.lehaine.littlekt.view

import android.opengl.GLSurfaceView
import android.util.Log
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
        private val EGL_CONTEXT_CLIENT_VERSION = 0x3098
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
    }
}