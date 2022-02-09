package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.MutableColor
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.toString
import kotlin.math.max

/**
 * Adds a [ProgressBar] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.progressBar(callback: @SceneGraphDslMarker ProgressBar.() -> Unit = {}) =
    ProgressBar().also(callback).addTo(this)

/**
 * Adds a [ProgressBar] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.progressBar(callback: @SceneGraphDslMarker ProgressBar.() -> Unit = {}) =
    root.progressBar(callback)


/**
 * General-purpose progress bar. Shows fill percentage from right to left.
 * @author Colton Daily
 * @date 2/6/2022
 */
open class ProgressBar : Range() {

    /**
     * If `true`, the fill percentage is displayed on the bar
     */
    var percentVisible: Boolean = true

    /**
     * Font used to draw the fill percentage if [percentVisible] is `true`.
     */
    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    /**
     * Color of the text.
     */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /**
     * The background drawable
     */
    var bg: Drawable
        get() = getThemeDrawable(themeVars.bg)
        set(value) {
            drawableOverrides[themeVars.bg] = value
        }

    /**
     * The drawable of the progress (the part that files the bar).
     */
    var fg: Drawable
        get() = getThemeDrawable(themeVars.fg)
        set(value) {
            drawableOverrides[themeVars.fg] = value
        }


    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    override fun render(batch: Batch, camera: Camera) {
        bg.draw(batch, globalX, globalY, width, height)
        val progress = ratio * (width - fg.minWidth)
        if (progress > 0) {
            fg.draw(batch, globalX, globalY, progress + fg.minWidth, height)
        }

        if (percentVisible) {
            val text = (ratio * 100.0).toString(1) + "%"
            layout.setText(font, text, fontColor)
            cache.setText(layout, 0f, 0f)

            tempColor.set(color).mul(fontColor)
            cache.tint(tempColor)

            if (globalRotation != Angle.ZERO || globalScaleX != 1f || globalScaleY != 1f) {
                applyTransform(batch)
                cache.setPosition(0f, 0f)
                cache.draw(batch)
                resetTransform(batch)
            } else {
                cache.setPosition(globalX + width / 2f - layout.width / 2f, globalY - height / 2f)
                cache.draw(batch)
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var minHeight = max(bg.minHeight, fg.minHeight)
        var minWidth = max(bg.minWidth, fg.minWidth)

        if (percentVisible) {
            minSizeLayout.setText(font, "100%")
            minHeight = max(minHeight, bg.minHeight + minSizeLayout.height)
        } else {
            // needed or else the progress bar will collapse
            minWidth = max(minWidth, 1f)
            minHeight = max(minHeight, 1f)
        }

        _internalMinWidth = minWidth
        _internalMinHeight = minHeight

        minSizeInvalid = false
    }

    class ThemeVars {
        val bg = "bg"
        val fg = "fg"
        val font = "font"
        val fontColor = "fontColor"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [PaddedContainer]
         */
        val themeVars = ThemeVars()

        private val tempColor = MutableColor()
        private val minSizeLayout = GlyphLayout()
    }

}