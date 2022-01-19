package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.NinePatch
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
class NinePatchDrawable(val ninePatch: NinePatch) : Drawable {
    override var marginLeft: Float = ninePatch.left.toFloat()
    override var marginRight: Float = ninePatch.right.toFloat()
    override var marginTop: Float = ninePatch.top.toFloat()
    override var marginBottom: Float = ninePatch.bottom.toFloat()

    override var minWidth: Float = (ninePatch.left - ninePatch.right).toFloat()
    override var minHeight: Float = (ninePatch.top - ninePatch.bottom).toFloat()

    override fun draw(
        batch: SpriteBatch,
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