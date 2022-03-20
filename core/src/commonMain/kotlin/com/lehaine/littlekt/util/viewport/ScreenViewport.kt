package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context

/**
 * A viewport that uses a virtual size that will always match the window size.
 * No scaling happens along with no black bars appearing.
 * @author Colton Daily
 * @date 12/21/2021
 */
class ScreenViewport(x: Int, y: Int, width: Int, height: Int) : Viewport(x, y, width, height) {
    constructor(width: Int, height: Int) : this(0, 0, width, height)

    /**
     * The number of pixels for each world unit.
     * Eg: a scale of 2.5f means there are 2.5f world units for every 1 screen pixel.
     */
    var unitsPerPixel = 1f


    override fun update(width: Int, height: Int, context: Context) {
        set(0, 0, width, height)
        virtualWidth = width * unitsPerPixel
        virtualHeight = height * unitsPerPixel
        apply(context)
    }
}