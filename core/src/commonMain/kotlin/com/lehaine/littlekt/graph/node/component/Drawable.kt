package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
interface Drawable {

    var marginLeft: Float
    var marginRight: Float
    var marginTop: Float
    var marginBottom: Float

    var minWidth: Float
    var minHeight: Float

    var modulate: Color


    fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = modulate
    )
}