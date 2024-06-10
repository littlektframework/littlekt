package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.*
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.BitmapFontCache
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.roundToInt

/** Adds a [Button] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}): Button {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Button().also(callback).addTo(this)
}

/** Adds a [Button] to the current [SceneGraph.root] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}): Button {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.button(callback)
}

/**
 * A [Control] that is a [BaseButton] that renders a button with text.
 *
 * @author Colton Daily
 * @date 1/17/2022
 */
open class Button : BaseButton() {

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    private var _fontScale = MutableVec2f(1f)

    /**
     * The padding, in pixels, between the edge of the button background and the text, on a single
     * side.
     *
     * E.g. if `padding = 10f` then 10 pixels will be added to the left side and 10 pixels will be
     * added to the right for a total for 20 extra pixels.
     */
    var padding = 10f
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

    /** The scale of the font used in the button. */
    var fontScale: Vec2f
        get() = _fontScale
        set(value) {
            if (value == _fontScale) return
            if (value.x == 0f || value.y == 0f) {
                _fontScale.set(value)
                if (value.x == 0f) {
                    _fontScale.x = 0.1f
                }
                if (value.y == 0f) {
                    _fontScale.y = 0.1f
                }
            } else {
                _fontScale.set(value)
            }
            onMinimumSizeChanged()
        }

    /** The scale of the font on the x-axis. */
    var fontScaleX: Float
        get() = _fontScale.x
        set(value) {
            if (value == _fontScale.x) return
            if (value == 0f) {
                _fontScale.x = value + 0.1f
            } else {
                _fontScale.x = value
            }
            onMinimumSizeChanged()
        }

    /** The scale of the font on the y-axis. */
    var fontScaleY: Float
        get() = _fontScale.y
        set(value) {
            if (value == _fontScale.y) return
            if (value == 0f) {
                _fontScale.y = value + 0.1f
            } else {
                _fontScale.y = value
            }
            onMinimumSizeChanged()
        }

    /** The color of the font. */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /** The [BitmapFont] that this label should use to draw with. */
    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    /** The vertical alignment of the text. */
    var verticalAlign: VAlign = VAlign.CENTER
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The horizontal alignment of the text. */
    var horizontalAlign: HAlign = HAlign.CENTER
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The label's text. */
    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The overflow text to display if the text is cut off. E.g `...`. */
    var ellipsis: String? = null

    /** `true` to allow the text to wrap to the next line. */
    var wrap: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** Convert the text to all uppercase. */
    var uppercase: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    init {
        focusMode = FocusMode.ALL
    }

    override fun onHierarchyChanged(flag: Int) {
        super.onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY) {
            onMinimumSizeChanged()
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        val drawable: Drawable
        when (drawMode) {
            DrawMode.NORMAL -> {
                drawable = getThemeDrawable(themeVars.normal)
            }
            DrawMode.PRESSED -> {
                drawable = getThemeDrawable(themeVars.pressed)
            }
            DrawMode.HOVER -> {
                drawable = getThemeDrawable(themeVars.hover)
            }
            DrawMode.DISABLED -> {
                drawable = getThemeDrawable(themeVars.disabled)
            }
            DrawMode.HOVER_PRESSED -> {
                drawable =
                    if (hasThemeDrawable(themeVars.hoverPressed)) {
                        getThemeDrawable(themeVars.hoverPressed)
                    } else {
                        getThemeDrawable(themeVars.pressed)
                    }
            }
        }

        drawable.draw(
            batch,
            globalX,
            globalY,
            width = width,
            height = height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = rotation,
            color = drawable.tint
        )

        if (hasFocus) {
            val focusDrawable = getThemeDrawable(themeVars.focus)
            focusDrawable.draw(
                batch,
                globalX,
                globalY,
                width = width,
                height = height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = rotation,
                color = focusDrawable.tint
            )
        }
        cache.let {
            tempColor.set(color).mul(fontColor)
            it.tint(tempColor)
            if (globalRotation != Angle.ZERO || globalScaleX != 1f || globalScaleY != 1f) {
                applyTransform(batch)
                it.setPosition(0f, 0f)
                it.draw(batch)
                resetTransform(batch)
            } else {
                it.setPosition(globalX, globalY)
                it.draw(batch)
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        layout()

        val drawable = getThemeDrawable(themeVars.normal)
        _internalMinWidth = max(layout.width, drawable.minWidth) + padding * 2f
        _internalMinHeight = max(layout.height, drawable.minHeight) + padding * 2f

        minSizeInvalid = false
    }

    private fun layout() {
        val text = if (uppercase) text.uppercase() else text

        var tx = padding
        var ty = 0f

        val background = getThemeDrawable(themeVars.normal)

        layout.setText(
            font,
            text,
            Color.WHITE,
            if (wrap || text.contains("\n")) max(width, background.minWidth) else 0f,
            scaleX = fontScaleX,
            scaleY = fontScaleY,
            horizontalAlign,
            wrap,
            ellipsis
        )
        val textWidth: Float = max(layout.width, width)
        val textHeight: Float = if (wrap || text.contains("\n")) layout.height else font.capHeight

        if (horizontalAlign != HAlign.LEFT) {
            tx +=
                if (horizontalAlign == HAlign.RIGHT) {
                    width - textWidth
                } else {
                    (width - textWidth) * 0.5f
                }
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty -= padding
                ty += height
                ty -= textHeight
                ty -= font.metrics.ascent
            }
            VAlign.BOTTOM -> {
                ty += padding
                ty += font.metrics.descent
            }
            else -> {
                ty += height * 0.5f
                ty -= textHeight * 0.5f
                ty += font.metrics.descent * 0.5f
            }
        }
        ty = ty.roundToInt().toFloat()

        layout.setText(
            font,
            text,
            Color.WHITE,
            textWidth - padding * 2,
            scaleX = fontScaleX,
            scaleY = fontScaleY,
            horizontalAlign,
            wrap,
            ellipsis
        )
        cache.setText(layout, tx, ty, fontScaleX, fontScaleY)
    }

    class ThemeVars {
        val fontColor = "fontColor"
        val font = "font"
        val normal = "normal"
        val pressed = "pressed"
        val hover = "hover"
        val hoverPressed = "hoverPressed"
        val disabled = "disabled"
        val focus = "focus"
    }

    companion object {
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for a [Button] */
        val themeVars = ThemeVars()
    }
}
