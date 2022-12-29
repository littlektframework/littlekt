package com.lehaine.littlekt.graph.node.resource

import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.geom.Angle

/**
 * A [Drawable] with no size or margin and draws nothing.
 *
 * **WARNING**: This drawable is immutable and attempting to change a value will throw an exception!
 * @author Colton Daily
 * @date 10/19/2022
 */
object EmptyDrawable : Drawable {
    override var marginLeft: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var marginRight: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var marginTop: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var marginBottom: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var minWidth: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var minHeight: Float
        get() = 0f
        set(_) {
            error("You must not change the margins or minimum size of the EmptyDrawable!")
        }
    override var modulate: Color
        get() = Color.WHITE
        set(_) {
            error("You must not change the color  of the EmptyDrawable!")
        }

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

/**
 * Returns the [EmptyDrawable] singleton object.
 *
 * **WARNING**: This drawable is immutable and attempting to change a value will throw an exception!
 */
fun emptyDrawable(): EmptyDrawable = EmptyDrawable