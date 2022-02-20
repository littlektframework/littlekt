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
    private val glyphPositions = FloatArrayList()

    private var visibleStart = 0
    private var visibleEnd = 0
    private val availableWidth get() = width - bg.marginLeft - bg.marginRight
    private var renderOffset = 0f
    private var fontOffset = 0f

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

        if (event.type == InputEvent.Type.TOUCH_DOWN || event.type == InputEvent.Type.TOUCH_DRAGGED) {
            println(event.type)
            moveCaretToPosition(event.localX)
            event.handle()
        }

        if (event.type == InputEvent.Type.KEY_DOWN || event.type == InputEvent.Type.KEY_REPEAT) {
            when (event.key) {
                Key.ARROW_LEFT -> {
                    if (caretPosition > 0) {
                        caretPosition--
                    }
                    event.handle()
                }
                Key.ARROW_RIGHT -> {
                    if (caretPosition < text.length) {
                        caretPosition++
                    }
                    event.handle()
                }
                Key.BACKSPACE -> {
                    removeAtCaret(false)
                    event.handle()
                }
                Key.DELETE -> {
                    removeAtCaret(true)
                    event.handle()
                }
                Key.HOME -> {
                    caretPosition = 0
                    event.handle()
                }
                Key.END -> {
                    caretPosition = text.length
                    event.handle()
                }
                else -> Unit
            }
        }

        if (event.type == InputEvent.Type.CHAR_TYPED) {
            insertAtCaret(event.char.toString())
            event.handle()
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
            calculateVisibility()
            cache.setText(
                text.substring(visibleStart, visibleEnd),
                globalX + bg.marginLeft,
                globalY,
                scaleX,
                scaleY,
                rotation
            )
            cache.draw(batch)
        }

        if (hasFocus) {
            val caretHeight = font.capHeight - font.metrics.descent
            caret.draw(
                batch,
                globalX + bg.marginLeft + glyphPositions[caretPosition] - glyphPositions[visibleStart] + fontOffset,
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
        glyphPositions.clear()
        var x = 0f
        if (layout.runs.isNotEmpty()) {
            val run = layout.runs.first()
            fontOffset = run.glyphs[0].left
            run.glyphs.forEach { glyph ->
                glyphPositions += x
                x += glyph.xAdvance
            }
        }

        glyphPositions += x

        visibleStart = min(visibleStart, glyphPositions.size - 1)
        visibleEnd = visibleEnd.clamp(visibleStart, glyphPositions.size - 1)
    }

    private fun calculateVisibility() {
        caretPosition = caretPosition.clamp(0, glyphPositions.size - 1)
        val distance = glyphPositions[max(0, caretPosition - 1)] + renderOffset
        if (distance <= 0f) {
            renderOffset -= distance
        } else {
            val index = min(glyphPositions.size - 1, caretPosition + 1)
            val minX = glyphPositions[index] - availableWidth
            if (-renderOffset < minX) renderOffset = -minX
        }

        var maxOffset = 0f
        val width = glyphPositions.last()
        for (i in glyphPositions.size - 2 downTo 0) {
            val x = glyphPositions[i]
            if (width - x > availableWidth) break
            maxOffset = x
        }
        if (-renderOffset > maxOffset) renderOffset = -maxOffset

        visibleStart = 0
        for (i in glyphPositions.indices) {
            if (glyphPositions[i] >= -renderOffset) {
                visibleStart = i
                break
            }
        }

        var end = visibleStart + 1
        val endX = availableWidth - renderOffset
        for (n in end..min(text.length, glyphPositions.size)) {
            if (glyphPositions[end] > endX) break
            end = n
        }
        visibleEnd = max(0, end)
    }

    private fun insertAtCaret(chars: CharSequence) {
        stringBuilder.clear()
        stringBuilder.append(text)
        stringBuilder.insert(caretPosition, chars)
        caretPosition += chars.length
        text = stringBuilder.toString()
    }

    private fun removeAtCaret(forward: Boolean) {
        if (!forward && caretPosition <= 0) return
        if (forward && caretPosition >= text.length) return

        stringBuilder.clear()
        stringBuilder.append(text)
        val index = if (forward) caretPosition else --caretPosition
        stringBuilder.deleteAt(index)
        text = stringBuilder.toString()
    }

    private fun moveCaretToPosition(x: Float) {
        caretPosition = determineGlyphPosition(x)
        caretPosition = max(0, caretPosition)
    }

    private fun determineGlyphPosition(tx: Float): Int {
        val x = tx + fontOffset - glyphPositions[visibleStart] - bg.marginLeft
        for (i in 1 until glyphPositions.size) {
            if (glyphPositions[i] > x) {
                if (glyphPositions[i] - x <= x - glyphPositions[i - 1]) return i
                return i - 1
            }
        }
        return glyphPositions.size - 1
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
        private val stringBuilder = StringBuilder()
    }
}