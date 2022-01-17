package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle
import kotlin.math.max

/**
 * Adds a [Button] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}) =
    Button().also(callback).addTo(this)

/**
 * Adds a [Button] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.button(callback: @SceneGraphDslMarker Button.() -> Unit = {}) =
    root.button(callback)

/**
 * @author Colton Daily
 * @date 1/17/2022
 */
open class Button : BaseButton() {

    companion object {
        private val tempColor = MutableColor()
        private val minSizeLayout = GlyphLayout()
    }

    private var cache: BitmapFontCache? = null
    private val layout = GlyphLayout()

    private var _fontScale = MutableVec2f(1f)
    private var textDirty = false

    var background: TextureSlice = Textures.white
    var backgroundColor = Color.BLACK
    var backgroundMinWidth = 50f

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

    var fontColor = Color.WHITE

    var font: BitmapFont?
        get() = cache?.font
        set(value) {
            cache = if (value == null) {
                cache?.font?.dispose()
                null
            } else {
                BitmapFontCache(value)
            }
        }

    var verticalAlign: VAlign = VAlign.CENTER
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }
    var horizontalAlign: HAlign = HAlign.CENTER
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

        when (drawMode) {
            DrawMode.NORMAL -> tempColor.set(backgroundColor)
            DrawMode.PRESSED -> tempColor.set(backgroundColor).lighten(0.2f)
            DrawMode.HOVER -> tempColor.set(backgroundColor).lighten(0.5f)
            DrawMode.DISABLED -> tempColor.set(Color.DARK_GRAY)
            DrawMode.HOVER_PRESSED -> tempColor.set(backgroundColor).darken(0.5f)
        }

        batch.draw(
            background,
            globalX,
            globalY,
            width = width,
            height = height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            colorBits = tempColor.toFloatBits()
        )
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
            textDirty = false
        }

        val text = if (uppercase) text.uppercase() else text
        minSizeLayout.setText(font, text, scaleX = fontScaleX, scaleY = fontScaleY, wrap = wrap)
        _internalMinWidth = minSizeLayout.width + padding
        _internalMinHeight = minSizeLayout.height + padding

        minSizeInvalid = false
    }

    private fun layout() {
        val font = font ?: return
        val cache = cache ?: return
        val text = if (uppercase) text.uppercase() else text

        var ty = 0f
        val textWidth: Float
        val textHeight: Float

        if (wrap || text.contains("\n")) {
            layout.setText(
                font,
                text,
                Color.WHITE,
                max(width, backgroundMinWidth) + padding * 2,
                scaleX = fontScaleX,
                scaleY = fontScaleY,
                horizontalAlign,
                wrap,
                ellipsis
            )
            textWidth = layout.width
            textHeight = layout.height + padding * 2f
        } else {
            textWidth = max(width, backgroundMinWidth) + padding * 2f
            textHeight = font.metrics.capHeight + padding * 2f
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
                //ty += textHeight
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