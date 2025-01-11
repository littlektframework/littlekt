package com.littlekt.graph.node.resource

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.slice
import com.littlekt.math.geom.Angle

/**
 * A [Drawable] for a [TextureSlice]. Simply renders the [TextureSlice] as is.
 *
 * @param slice the texture slice to draw
 * @author Colton Daily
 * @date 1/19/2022
 */
class TextureSliceDrawable(val slice: TextureSlice) : Drawable {
    constructor(texture: Texture) : this(texture.slice())

    override var marginLeft: Float = 0f
    override var marginRight: Float = 0f
    override var marginTop: Float = 0f
    override var marginBottom: Float = 0f

    override var minWidth: Float = slice.width.toFloat()
    override var minHeight: Float = slice.height.toFloat()

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
    ) {
        batch.draw(
            slice = slice,
            x = x,
            y = y,
            originX = originX,
            originY = originY,
            width = width,
            height = height,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            color = color,
        )
    }
}
