package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.math.geom.Angle

/**
 * A [Drawable] for a [TextureSlice].
 * @author Colton Daily
 * @date 1/19/2022
 */
class TextureSliceDrawable(val slice: TextureSlice) : Drawable {
    constructor(texture: Texture) : this(texture.slice())

    override var minWidth: Float = slice.width.toFloat()
    override var minHeight: Float = slice.height.toFloat()
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
        batch.draw(
            slice,
            x,
            y,
            width = width,
            height = height,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            colorBits = color.toFloatBits()
        )
    }


}