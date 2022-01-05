package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.GlyphLayout
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GpuFontCache
import com.lehaine.littlekt.graphics.font.TtfFont

/**
 * Adds a [GpuLabel] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.gpuLabel(callback: @SceneGraphDslMarker GpuLabel.() -> Unit = {}) =
    GpuLabel().also(callback).addTo(this)

/**
 * Adds a [GpuLabel] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.gpuLabel(callback: @SceneGraphDslMarker GpuLabel.() -> Unit = {}) =
    root.gpuLabel(callback)

/**
 * A label that takes the beziers from a [TtfFont], writes them to a texture, and then read the texture in a shader
 * to render the font to prevent blurring. Allows for unlimited scaling.
 * @author Colton Daily
 * @date 1/4/2022
 */
open class GpuLabel : Control() {
    private val layout = GlyphLayout()
    private var textDirty = false
    private val cache: GpuFontCache = GpuFontCache()

    var pxSize: Int = 16
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var font: TtfFont? = null

    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
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


    override fun gpuFontRender(batch: SpriteBatch, camera: Camera) {
        cache.setPosition(globalX, globalY)
        cache.render(batch)
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return
        val font = font ?: return

        if (textDirty) {
            layout()
        }

        val text = if (uppercase) text.uppercase() else text
        val scale = font.pxScale(pxSize)

        minSizeLayout.setText(font, text, width, scale, HAlign.LEFT, wrap)

        _internalMinWidth = minSizeLayout.width
        _internalMinHeight = minSizeLayout.height

        minSizeInvalid = false
    }

    private fun layout() {
        val font = font ?: return

        val text = if (uppercase) text.uppercase() else text
        var ty = 0f
        val scale = font.pxScale(pxSize)
        val textWidth: Float
        val textHeight: Float

        if (wrap || text.contains("\n")) {
            layout.setText(font, text, width, scale, horizontalAlign, wrap)
            textWidth = layout.width
            textHeight = layout.height
        } else {
            textWidth = width
            textHeight = font.capHeight * scale
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty += height
                ty += font.ascender * scale
            }
            VAlign.BOTTOM -> {
                ty += textHeight
                ty -= font.ascender * scale
            }
            else -> {
                ty += (height - textHeight) / 2
                ty += textHeight
            }
        }


        layout.setText(font, text, textWidth, scale, horizontalAlign, wrap)
        cache.setText(font, layout, 0f, ty, scale, rotation, color)
    }

    companion object {
        private val minSizeLayout = GlyphLayout()
    }
}