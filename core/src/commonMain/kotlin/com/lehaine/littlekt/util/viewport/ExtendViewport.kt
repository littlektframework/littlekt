package com.lehaine.littlekt.util.viewport

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.util.Scaler
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class ExtendViewport(val minWidth: Int, val minHeight: Int) : Viewport() {
    init {
        this.virtualWidth = minWidth
        this.virtualHeight = minHeight
    }

    override fun update(width: Int, height: Int, context: Context) {
        var worldWidth = minWidth.toFloat()
        var worldHeight = minHeight.toFloat()

        val scaled = Scaler.Fit().apply(minWidth, minHeight, width, height)
        var viewportWidth = scaled.x.roundToInt()
        var viewportHeight = scaled.y.roundToInt()
        if (viewportWidth < width) {
            val toViewportSpace = viewportHeight / worldHeight
            val toWorldSpace = worldHeight / viewportHeight
            val lengthen = (width - viewportWidth) * toWorldSpace
            worldWidth += lengthen
            viewportWidth += (lengthen * toViewportSpace).roundToInt()
        } else if (viewportHeight < height) {
            val toViewportSpace = viewportWidth / worldWidth
            val toWorldSpace = worldWidth / viewportWidth
            val lengthen = (height - viewportHeight) * toWorldSpace
            worldHeight += lengthen
            viewportHeight += (lengthen * toViewportSpace).roundToInt()
        }

        virtualWidth = worldWidth.toInt()
        virtualHeight = worldHeight.toInt()
        set((width - viewportWidth) / 2, (height - viewportHeight) / 2, viewportWidth, viewportHeight)
        apply(context)
    }
}