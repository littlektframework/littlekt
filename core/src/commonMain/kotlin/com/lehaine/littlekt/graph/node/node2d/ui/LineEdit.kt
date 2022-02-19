package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.clamp
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * Adds a [LineEdit] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.lineEdit(callback: @SceneGraphDslMarker LineEdit.() -> Unit = {}) =
    LineEdit().also(callback).addTo(this)

/**
 * Adds a [LineEdit] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.lineEdit(callback: @SceneGraphDslMarker LineEdit.() -> Unit = {}) =
    root.lineEdit(callback)

/**
 * @author Colton Daily
 * @date 2/18/2022
 */
class LineEdit : Control() {

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()
    private val widthPositions = FloatArrayList()

    private var visibleStart = 0
    private var visibleEnd = 0

    var editable: Boolean = true
    var text: String = ""
        set(value) {
            field = value
            updateText()
        }
    var placeholderText: String = ""
    var secretCharacter: Char = '*'
    var caretPosition: Int = 0

    var font: BitmapFont
        get() = getThemeFont(Label.themeVars.font)
        set(value) {
            fontOverrides[Label.themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    var bg: Drawable
        get() = getThemeDrawable(themeVars.bg)
        set(value) {
            drawableOverrides[themeVars.bg] = value
        }

    var caret: Drawable
        get() = getThemeDrawable(themeVars.caret)
        set(value) {
            drawableOverrides[themeVars.caret] = value
        }

    var focusDrawable: Drawable
        get() = getThemeDrawable(themeVars.focus)
        set(value) {
            drawableOverrides[themeVars.focus] = value
        }

    init {
        focusMode = FocusMode.ALL
    }

    override fun uiInput(event: InputEvent) {
        super.uiInput(event)

        if (event.type == InputEvent.Type.TOUCH_DOWN) {
            event.handle()
        }

        if (event.type == InputEvent.Type.KEY_DOWN) {
            when (event.key) {
                Key.ARROW_LEFT -> {
                    if (caretPosition > 0) caretPosition--
                    event.handle()
                }
                Key.ARROW_RIGHT -> {
                    if (caretPosition < text.length) caretPosition++
                    event.handle()
                }
                else -> Unit
            }
            println(caretPosition)
        }
    }

    override fun render(batch: Batch, camera: Camera) {
        super.render(batch, camera)

        bg.draw(
            batch,
            globalX,
            globalY,
            width = width,
            height = height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = rotation
        )


        if (text.isNotEmpty()) {
            calcOffsets()
            cache.setText(
                text.substring(visibleStart, visibleEnd),
                globalX + bg.marginLeft,
                globalY,
                scaleX,
                scaleY,
                rotation
            )
            cache.draw(batch)
        } else {

        }

        if(hasFocus) {
            val caretHeight = font.capHeight - font.metrics.descent
            caret.draw(
                batch,
                globalX + bg.marginLeft + widthPositions[caretPosition],
                globalY,
                width = 1f,
                height = caretHeight,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = rotation,
            )
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth = max(minWidth, bg.minWidth)
        _internalMinHeight = max(minHeight, bg.minHeight)

        minSizeInvalid = false
    }

    private fun updateText() {
        layout.setText(font, text.replace('\r', ' ').replace('\n', ' '))
        widthPositions.clear()
        var x = 0f
        if (layout.runs.isNotEmpty()) {
            val advances = layout.runs.first().advances
            for (i in 1 until advances.size) {
                widthPositions += x
                x += advances[i]
            }
        }
        widthPositions += x

        visibleStart = min(visibleStart, widthPositions.size - 1)
        visibleEnd = visibleEnd.clamp(visibleStart, widthPositions.size - 1)
    }

    private fun calcOffsets() {
        visibleEnd = 0
        val availableWidth = width - bg.marginLeft - bg.marginRight
        for (pos in widthPositions) {
            if (pos <= availableWidth) {
                visibleEnd++
            } else {
                break
            }
        }
        visibleEnd--
        visibleEnd = visibleEnd.clamp(visibleStart, widthPositions.size - 1)
    }

    class ThemeVars {
        val fontColor = "fontColor"
        val font = "font"
        val bg = "bg"
        val pressed = "pressed"
        val hover = "hover"
        val hoverPressed = "hoverPressed"
        val disabled = "disabled"
        val focus = "focus"
        val caret = "caret"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [LineEdit]
         */
        val themeVars = ThemeVars()
    }
}