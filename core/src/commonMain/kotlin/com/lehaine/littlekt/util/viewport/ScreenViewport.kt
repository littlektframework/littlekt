package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context

/**
 * @author Colton Daily
 * @date 12/21/2021
 */
class ScreenViewport(x: Int, y: Int, width: Int, height: Int) : Viewport(x, y, width, height) {
    constructor(width: Int, height: Int) : this(0, 0, width, height)

    override fun update(width: Int, height: Int, context: Context) {
        set(0, 0, width, height)
        virtualWidth = width
        virtualHeight = height
        apply(context)
    }
}