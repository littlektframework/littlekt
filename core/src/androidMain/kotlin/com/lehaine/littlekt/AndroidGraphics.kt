package com.lehaine.littlekt

import android.opengl.GLSurfaceView.Renderer
import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.graphics.SystemCursor
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidGraphics(engineStats: EngineStats) : Graphics, Renderer {
    override val gl: AndroidGL = AndroidGL(engineStats)

    internal var _width: Int = 0
    internal var _height: Int = 0
    internal var onCreate: (() -> Unit)? = null
    internal var onResize: ((width: Int, height: Int) -> Unit)? = null
    internal var onDrawFrame: (() -> Unit)? = null

    override val width: Int
        get() = _width
    override val height: Int
        get() = _height

    private val extensions: String by lazy { gl.getString(GL.EXTENSIONS) ?: "" }

    override fun supportsExtension(extension: String): Boolean {
        return extensions.contains(extension)
    }

    override fun setCursor(cursor: Cursor) = Unit
    override fun setCursor(cursor: SystemCursor) = Unit

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        val versionString = gl.glGetString(GL10.GL_VERSION)
        val vendorString = gl.glGetString(GL10.GL_VENDOR)
        val rendererString = gl.glGetString(GL10.GL_RENDERER)
        this.gl.glVersion = GLVersion(Context.Platform.ANDROID, versionString, vendorString, rendererString)
        onCreate?.invoke()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        _width = width
        _height = height
        onResize?.invoke(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        onDrawFrame?.invoke()
    }
}