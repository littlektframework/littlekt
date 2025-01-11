package com.littlekt.graph.node.resource

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.geom.Angle

/**
 * An interface to allow the drawing of anything with a [Batch], position, size, and rotation.
 *
 * @author Colton Daily
 * @date 1/19/2022
 */
interface Drawable {

    /** Extra margin / padding that can be added to the left side when drawing. */
    var marginLeft: Float

    /** Extra margin / padding that can be added to the right side when drawing. */
    var marginRight: Float
    /** Extra margin / padding that can be added to the top side when drawing. */
    var marginTop: Float
    /** Extra margin / padding that can be added to the bottom side when drawing. */
    var marginBottom: Float

    /** The minimum width required to draw this [Drawable]. */
    var minWidth: Float

    /** The minium height required to draw this [Drawable]. */
    var minHeight: Float

    /** The tint/color to use when drawing this [Drawable]. */
    var tint: Color

    /** Draw this [Drawable] with the given [Batch]. */
    fun draw(
        batch: Batch,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = tint,
    )
}
