package com.littlekt.util.viewport

import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * A base [Viewport] that handles scaling to a virtual width and height.
 *
 * @author Colton Daily
 * @date 12/21/2021
 */
open class ScalingViewport(
    val scaler: Scaler,
    virtualWidth: Int,
    virtualHeight: Int,
    camera: Camera = OrthographicCamera(),
) : Viewport(0, 0, virtualWidth, virtualHeight, camera) {

    override fun update(width: Int, height: Int, centerCamera: Boolean) {
        val scaled = scaler.apply(virtualWidth, virtualHeight, width.toFloat(), height.toFloat())
        val viewportWidth = scaled.x.roundToInt()
        val viewportHeight = scaled.y.roundToInt()

        x = (width - viewportWidth) / 2
        y = (height - viewportHeight) / 2
        this.width = viewportWidth
        this.height = viewportHeight

        apply(centerCamera)
    }
}
