package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.MutableColor
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.math.MutableVec2f
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
    }

    private var cache: BitmapFontCache? = null
    private val layout = GlyphLayout()

    private var fontScaleDirty = false

    private var _fontScale = MutableVec2f()
    private var textDirty = false

    var fontColor = Color.WHITE

    var fontScale
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
            fontScaleDirty = true
            onMinimumSizeChanged()
        }

    var fontScaleX
        get() = _fontScale.x
        set(value) {
            if (value == _fontScale.x) return
            if (value == 0f) {
                _fontScale.x = value + 0.1f
            } else {
                _fontScale.x = value
            }
            textDirty = true
            fontScaleDirty = true
            onMinimumSizeChanged()
        }

    var fontScaleY
        get() = _fontScale.y
        set(value) {
            if (value == _fontScale.y) return
            if (value == 0f) {
                _fontScale.y = value + 0.1f
            } else {
                _fontScale.y = value
            }
            textDirty = true
            fontScaleDirty = true
            onMinimumSizeChanged()
        }

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

    var verticalAlign: VAlign = VAlign.BOTTOM
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
        if (wrap) {
            minSizeLayout.setText(font, text, Color.WHITE, width, 1f, HAlign.LEFT, true)
        } else {
            minSizeLayout.setText(font, text)
        }
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
        val textHeight: Float

        if (wrap || text.contains("\n")) {
            layout.setText(font, text, Color.WHITE, width, 1f, horizontalAlign, wrap, ellipsis)
            textWidth = layout.width
            textHeight = layout.height
        } else {
            textWidth = width
            textHeight = font.metrics.capHeight
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty += height
                ty += font.metrics.ascent
            }
            VAlign.BOTTOM -> {
                ty += textHeight
                ty -= font.metrics.ascent
            }
            else -> {
                ty += (height - textHeight) / 2
                ty += textHeight
            }
        }

        layout.setText(font, text, Color.WHITE, textWidth, 1f, horizontalAlign, wrap, ellipsis)
        cache.setText(layout, 0f, ty)
    }
}