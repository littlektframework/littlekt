package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.Cursor
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.SystemCursor

/**
 * @author Colton Daily
 * @date 2/12/2022
 */
class AndroidGraphics(engineStats: EngineStats) : Graphics {
    override val gl: AndroidGL = AndroidGL(engineStats)

    internal var _width: Int = 0
    internal var _height: Int = 0

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
}