package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.geom.Angle

/**
 * A [Drawable] with no size or margin and draws nothing.
 * @author Colton Daily
 * @date 10/19/2022
 */
class EmptyDrawable : Drawable {
    override var marginLeft: Float = 0f
    override var marginRight: Float = 0f
    override var marginTop: Float = 0f
    override var marginBottom: Float = 0f
    override var minWidth: Float = 0f
    override var minHeight: Float = 0f
    override var modulate: Color = Color.WHITE

    override fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
    ) {
        // Do nothing. nothing to draw
    }
}