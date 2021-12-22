package com.lehaine.littlekt.graphics.viewport

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 12/21/2021
 */
open class ScalingViewport(val scaler: Scaler, virtualWidth: Int, virtualHeight: Int) : Viewport() {
    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
    }

    override fun update(width: Int, height: Int, context: Context) {
        val scaled = scaler.apply(virtualWidth, virtualHeight, width, height)
        val viewportWidth = scaled.x.roundToInt()
        val viewportHeight = scaled.y.roundToInt()

        x = (width - viewportWidth) / 2
        y = (height - viewportHeight) / 2
        this.width = viewportWidth
        this.height = viewportHeight

        apply(context)
    }
}

class FitViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Fit(), virtualWidth,
    virtualHeight
)

class StretchViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Stretch(), virtualWidth,
    virtualHeight
)

class FillViewport(virtualWidth: Int, virtualHeight: Int) : ScalingViewport(
    Scaler.Fill(), virtualWidth,
    virtualHeight
)