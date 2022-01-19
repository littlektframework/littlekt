package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
interface Drawable {

    var minWidth: Float
    var minHeight: Float

    fun draw(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE
    )
}