package com.littlekt.util.viewport

import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera

/**
 * A viewport that uses a virtual size that will always match the window size. No scaling happens.
 *
 * @author Colton Daily
 * @date 12/21/2021
 */
class ScreenViewport(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    camera: Camera = OrthographicCamera(),
) : Viewport(x, y, width, height, camera) {
    constructor(
        width: Int,
        height: Int,
        camera: Camera = OrthographicCamera()
    ) : this(0, 0, width, height, camera)

    /**
     * The number of pixels for each world unit. Eg: a scale of 2.5f means there are 2.5f world
     * units for every 1 screen pixel.
     */
    var unitsPerPixel = 1f

    override fun update(width: Int, height: Int, centerCamera: Boolean) {
        set(0, 0, width, height)
        virtualWidth = width * unitsPerPixel
        virtualHeight = height * unitsPerPixel
        apply(centerCamera)
    }
}
