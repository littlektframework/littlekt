package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.resource.Drawable
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graph.node.resource.Theme
import com.lehaine.littlekt.graph.node.resource.VAlign
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.MutableColor
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.font.BitmapFont
import com.lehaine.littlekt.graphics.g2d.font.BitmapFontCache
import com.lehaine.littlekt.graphics.g2d.font.GlyphLayout
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

/**
 * Adds a [Button] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}): Button {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Button().also(callback).addTo(this)
}

/**
 * Adds a [Button] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}): Button {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.button(callback)
}

/**
 * @author Colton Daily
 * @date 1/17/2022
 */
open class Button : BaseButton() {

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    private var _fontScale = MutableVec2f(1f)

    var padding = 10f
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

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

    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    var verticalAlign: VAlign = VAlign.CENTER
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }
    var horizontalAlign: HAlign = HAlign.CENTER
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    var ellipsis: String? = null

    var wrap: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

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
        if (flag == SIZE_DIRTY && parent is Container) {
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
                drawable = if (hasThemeDrawable(themeVars.hoverPressed)) {
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
            color = drawable.modulate
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
                color = focusDrawable.modulate
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

        if(horizontalAlign != HAlign.LEFT) {
            tx += if(horizontalAlign == HAlign.RIGHT) {
                width - textWidth
            } else {
                (width - textWidth) / 2
            }
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty += font.metrics.descent
                ty += padding
            }
            VAlign.BOTTOM -> {
                ty += height
                ty -= textHeight
                ty -= padding
                ty += font.metrics.descent
            }
            else -> {
                ty += height / 2
                ty -= textHeight
            }
        }

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

        /**
         * [Theme] related variable names when setting theme values for a [Button]
         */
        val themeVars = ThemeVars()
    }

}