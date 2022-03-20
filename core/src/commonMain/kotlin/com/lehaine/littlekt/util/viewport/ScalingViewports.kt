package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * A base [Viewport] that handles scaling to a virtual width and height.
 * @author Colton Daily
 * @date 12/21/2021
 */
open class ScalingViewport(val scaler: Scaler, virtualWidth: Int, virtualHeight: Int) :
    Viewport(0, 0, virtualWidth, virtualHeight) {

    override fun update(width: Int, height: Int, context: Context) {
        val scaled = scaler.apply(virtualWidth, virtualHeight, width.toFloat(), height.toFloat())
        val viewportWidth = scaled.x.roundToInt()
        val viewportHeight = scaled.y.roundToInt()

        x = (width - viewportWidth) / 2
        y = (height - viewportHeight) / 2
        this.width = viewportWidth
        this.height = viewportHeight

        apply(context)
    }
}

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport will maintain its aspect ratio while attempting to fit as much as possible onto the screen.
 * Black bars may appear.
 */
class FitViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Fit(), virtualWidth,
    virtualHeight
)

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport is stretched to fit the screen. There are no black bars and the aspect ratio can change after scaling.
 */
class StretchViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Stretch(), virtualWidth,
    virtualHeight
)

/**
 * A viewport that supports using a virtual size.
 * The virtual viewport will maintain its aspect ratio but in an attempt to fill
 * the screen parts of the viewport may be cut off.
 * No black bars may appear.
 */
class FillViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Fill(), virtualWidth,
    virtualHeight
)