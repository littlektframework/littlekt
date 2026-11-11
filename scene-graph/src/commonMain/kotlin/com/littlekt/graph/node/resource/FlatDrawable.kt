package com.littlekt.graph.node.resource

import com.littlekt.graphics.Color
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.math.geom.Angle
import com.littlekt.resources.Textures
import kotlin.math.max

/**
 * A [Drawable] of a flat rounded rectangle with an optional border, similar to Godot's
 * `StyleBoxFlat`. Uses [Textures.roundedRect], so no art is needed. The margins default to the
 * larger of [cornerRadius] and [borderWidth].
 *
 * @param color the fill color
 * @param borderColor the border color
 * @param borderWidth the border width in pixels or `0` for no border
 * @param cornerRadius the corner radius in pixels
 * @author Colton Daily
 * @date 11/11/2026
 */
class FlatDrawable(
    var color: Color = Color.WHITE,
    var borderColor: Color = Color.CLEAR,
    borderWidth: Int = 0,
    cornerRadius: Int = 0,
    margin: Float = max(cornerRadius, borderWidth).toFloat(),
) : Drawable {
    /** The border width in pixels or `0` for no border. */
    var borderWidth: Int = borderWidth
        set(value) {
            if (field == value) return
            field = value
            updatePatches()
        }

    /** The corner radius in pixels. */
    var cornerRadius: Int = cornerRadius
        set(value) {
            if (field == value) return
            field = value
            updatePatches()
        }

    override var marginLeft: Float = margin
    override var marginRight: Float = margin
    override var marginTop: Float = margin
    override var marginBottom: Float = margin

    override var minWidth: Float = 0f
    override var minHeight: Float = 0f

    override var tint: Color = Color.WHITE

    private lateinit var fill: NinePatch
    private var border: NinePatch? = null
    private val tempColor = MutableColor()

    init {
        updatePatches()
    }

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
        if (this.color.a > 0f) {
            tempColor.set(this.color).mul(color)
            fill.draw(
                batch = batch,
                x = x,
                y = y,
                width = width,
                height = height,
                originX = originX,
                originY = originY,
                scaleX = scaleX,
                scaleY = scaleY,
                rotation = rotation,
                color = tempColor,
            )
        }
        val border = border ?: return
        if (borderColor.a <= 0f) return
        tempColor.set(borderColor).mul(color)
        border.draw(
            batch = batch,
            x = x,
            y = y,
            width = width,
            height = height,
            originX = originX,
            originY = originY,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            color = tempColor,
        )
    }

    private fun updatePatches() {
        val fillCorner = Textures.roundedRectCorner(cornerRadius)
        fill =
            NinePatch(
                Textures.roundedRect(cornerRadius),
                fillCorner,
                fillCorner,
                fillCorner,
                fillCorner,
            )
        border =
            if (borderWidth > 0) {
                val corner = Textures.roundedRectCorner(cornerRadius, borderWidth)
                NinePatch(
                    Textures.roundedRect(cornerRadius, borderWidth),
                    corner,
                    corner,
                    corner,
                    corner,
                )
            } else {
                null
            }
        val minSize = max(cornerRadius, borderWidth) * 2f
        minWidth = minSize
        minHeight = minSize
    }
}
