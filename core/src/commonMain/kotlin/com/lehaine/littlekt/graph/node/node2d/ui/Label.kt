package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.MutableColor
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle

/**
 * Adds a [Label] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.label(callback: @SceneGraphDslMarker Label.() -> Unit = {}) =
    Label().also(callback).addTo(this)

/**
 * Adds a [Label] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.label(callback: @SceneGraphDslMarker Label.() -> Unit = {}) =
    root.label(callback)

/**
 * @author Colton Daily
 * @date 1/8/2022
 */
open class Label : Control() {

    companion object {
        private val tempColor = MutableColor()
        private val minSizeLayout = GlyphLayout()

        /**
         * [Theme] related variable names when setting theme values for a [Button]
         */
        object ThemeVars {
            const val FONT_COLOR = "fontColor"
            const val FONT = "font"
        }
    }

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    private var _fontScale = MutableVec2f(1f)
    private var textDirty = false

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
            textDirty = true
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
            textDirty = true
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
            textDirty = true
            onMinimumSizeChanged()
        }

    var fontColor: Color
        get() = getThemeColor(ThemeVars.FONT_COLOR)
        set(value) {
            colorsOverride[ThemeVars.FONT_COLOR] = value
        }

    var font: BitmapFont
        get() = getThemeFont(ThemeVars.FONT)
        set(value) {
            fontsOverride[ThemeVars.FONT] = value
            cache = BitmapFontCache(value)
        }

    var verticalAlign: VAlign = VAlign.TOP
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }
    var horizontalAlign: HAlign = HAlign.LEFT
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var ellipsis: String? = null

    var wrap: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var uppercase: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    override fun render(batch: SpriteBatch, camera: Camera) {
        cache?.let {
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
        val font = font ?: return

        if (textDirty) {
            layout()
        }

        val text = if (uppercase) text.uppercase() else text
        minSizeLayout.setText(font, text, scaleX = fontScaleX, scaleY = fontScaleY, wrap = wrap)
        _internalMinWidth = minSizeLayout.width
        _internalMinHeight = minSizeLayout.height

        minSizeInvalid = false
    }

    private fun layout() {
        val font = font ?: return
        val cache = cache ?: return
        val text = if (uppercase) text.uppercase() else text

        var ty = 0f
        val textWidth: Float

        if (wrap || text.contains("\n")) {
            layout.setText(
                font,
                text,
                Color.WHITE,
                width,
                scaleX = fontScaleX,
                scaleY = fontScaleY,
                horizontalAlign,
                wrap,
                ellipsis
            )
            textWidth = layout.width
        } else {
            textWidth = width
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty += font.metrics.descent
            }
            VAlign.BOTTOM -> {
                ty += height
                ty -= font.metrics.descent
            }
            else -> {
                ty += (height) / 2
            }
        }

        layout.setText(
            font,
            text,
            Color.WHITE,
            textWidth,
            scaleX = fontScaleX,
            scaleY = fontScaleY,
            horizontalAlign,
            wrap,
            ellipsis
        )
        cache.setText(layout, 0f, ty, fontScaleX, fontScaleY)
    }
}