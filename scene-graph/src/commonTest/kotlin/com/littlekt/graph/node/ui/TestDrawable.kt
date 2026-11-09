package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.geom.Angle

internal class TestDrawable(
    override var minWidth: Float = 0f,
    override var minHeight: Float = 0f,
    override var marginLeft: Float = 0f,
    override var marginRight: Float = 0f,
    override var marginTop: Float = 0f,
    override var marginBottom: Float = 0f,
) : Drawable {
    override var tint: Color = Color.WHITE

    override fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
    ) = Unit
}
