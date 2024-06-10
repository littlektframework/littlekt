package com.littlekt.graph.node.resource

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.math.geom.Angle

/**
 * A [Drawable] for a [NinePatch]. Simply renders the ninepatch as is.
 *
 * @param ninePatch the ninepatch to draw
 * @author Colton Daily
 * @date 1/19/2022
 */
class NinePatchDrawable(val ninePatch: NinePatch) : Drawable {
    override var marginLeft: Float = ninePatch.left.toFloat()
    override var marginRight: Float = ninePatch.right.toFloat()
    override var marginTop: Float = ninePatch.top.toFloat()
    override var marginBottom: Float = ninePatch.bottom.toFloat()

    override var minWidth: Float = ninePatch.totalWidth
    override var minHeight: Float = ninePatch.totalHeight

    override var tint: Color = Color.WHITE

    override fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color
    ) {
        ninePatch.draw(batch, x, y, width, height, 0f, 0f, scaleX, scaleY, rotation, color)
    }
}
