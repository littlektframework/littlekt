package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.font.GlyphLayout
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.clamp
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import com.lehaine.littlekt.util.internal.now
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adds a [LineEdit] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.lineEdit(callback: @SceneGraphDslMarker LineEdit.() -> Unit = {}): LineEdit {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return LineEdit().also(callback).addTo(this)
}

/**
 * Adds a [LineEdit] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.lineEdit(callback: @SceneGraphDslMarker LineEdit.() -> Unit = {}): LineEdit {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.lineEdit(callback)
}

/**
 * A [Control] that renders and single line of editable text.
 * @author Colton Daily
 * @date 2/18/2022
 */
open class LineEdit : Control() {

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()
    private val glyphPositions = FloatArrayList()

    private var visibleStart = 0
    private var visibleEnd = 0
    private val availableWidth get() = width - bg.marginLeft - bg.marginRight
    private var renderOffset = 0f
    private var fontOffset = 0f
    private var textOffset = 0f
    private var selectionStart = 0
    private var hasSelection = false
    private var selectionX = 0f
    private var selectionWidth = 0f
    private var _caretPosition: Int = 0

    private var lastChanged: Duration = Duration.ZERO
    private val undoStack by lazy { ArrayDeque<String>() }
    private val redoStack by lazy { ArrayDeque<String>() }

    private val secretBuffer by lazy { StringBuilder() }

    private var lastPointer: Pointer? = null
    private var lastTap: Duration = Duration.ZERO
    private var taps = 0
    private var pressed = false
    private var displayTest: String = ""

    var editable: Boolean = true
    var text: String = ""
        set(value) {
            field = value
            updateText()
            _caretPosition = _caretPosition.clamp(0, text.length)
        }
    var placeholderText: String = ""
    var secretCharacter: Char = '*'
    var secret: Boolean = false
    var caretPosition: Int
        get() = _caretPosition
        set(value) {
            unselect()
            _caretPosition = value.clamp(0, text.length)
        }

    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    var fontColorDisabled: Color
        get() = getThemeColor(themeVars.fontColorDisabled)
        set(value) {
            colorOverrides[themeVars.fontColorDisabled] = value
        }

    var fontColorPlaceholder: Color
        get() = getThemeColor(themeVars.fontColorPlaceholder)
        set(value) {
            colorOverrides[themeVars.fontColorPlaceholder] = value
        }

    var bg: Drawable
        get() = getThemeDrawable(themeVars.bg)
        set(value) {
            drawableOverrides[themeVars.bg] = value
        }

    var bgDisabled: Drawable
        get() = getThemeDrawable(themeVars.disabled)
        set(value) {
            drawableOverrides[themeVars.disabled] = value
        }

    var selection: Drawable
        get() = getThemeDrawable(themeVars.selection)
        set(value) {
            drawableOverrides[themeVars.selection] = value
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

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)

        if (event.type == InputEvent.Type.TOUCH_DOWN) {
            if (pressed) return
            moveCaretToPosition(event.localX)
            pressed = true
            lastPointer = event.pointer
            selectionStart = _caretPosition
            hasSelection = true
            scene?.context?.input?.showSoftKeyboard()
        }

        if (event.type == InputEvent.Type.TOUCH_DRAGGED) {
            if (lastPointer != event.pointer) return
            pressed = hasPoint(event.sceneX, event.sceneY)
            moveCaretToPosition(event.localX)
        }

        if (event.type == InputEvent.Type.TOUCH_UP) {
            if (event.pointer == lastPointer) {
                if (selectionStart == _caretPosition) {
                    hasSelection = false
                }
                val time = now().milliseconds
                if (time - lastTap > 500.milliseconds) {
                    taps = 0
                }
                taps++
                lastTap = time
                pressed = false
                lastPointer = null
                onTapped(event)
            }
        }

        val oldText = text
        if (event.type == InputEvent.Type.KEY_DOWN || event.type == InputEvent.Type.KEY_REPEAT) {
            val shift =
                scene?.context?.input?.isKeyPressed(Key.SHIFT_LEFT) == true
                        || scene?.context?.input?.isKeyPressed(Key.SHIFT_RIGHT) == true
            val ctrl = scene?.context?.input?.isKeyPressed(Key.CTRL_LEFT) == true
                    || scene?.context?.input?.isKeyPressed(Key.CTRL_RIGHT) == true
            when (event.key) {
                Key.ARROW_LEFT -> {
                    if (text.isNotEmpty()) {
                        if (!hasSelection && shift) {
                            hasSelection = true
                            selectionStart = _caretPosition
                        } else if (!shift) {
                            unselect()
                        }
                        if (ctrl) {
                            while (--_caretPosition > 0) {
                                if (!text[_caretPosition - 1].isLetterOrDigit()) break
                            }
                        } else if (_caretPosition > 0) {
                            _caretPosition--
                        }
                    }
                    event.handle()
                }
                Key.ARROW_RIGHT -> {
                    if (text.isNotEmpty()) {
                        if (!hasSelection && shift) {
                            hasSelection = true
                            selectionStart = _caretPosition
                        } else if (!shift) {
                            unselect()
                        }
                        if (ctrl) {
                            while (++_caretPosition < text.length) {
                                if (!text[_caretPosition].isLetterOrDigit()) break
                            }
                        } else if (_caretPosition < text.length) {
                            _caretPosition++
                        }
                    }
                    event.handle()
                }
                Key.BACKSPACE -> {
                    if (!editable) return
                    removeAtCaret(false)
                    event.handle()
                }
                Key.DELETE -> {
                    if (!editable) return
                    removeAtCaret(scene?.context?.platform?.isMobile?.not() ?: true)
                    event.handle()
                }
                Key.HOME -> {
                    _caretPosition = 0
                    event.handle()
                }
                Key.END -> {
                    _caretPosition = text.length
                    event.handle()
                }
                Key.A -> {
                    if (ctrl) {
                        selectAll()
                        event.handle()
                    }
                }
                Key.C -> {
                    if (ctrl && hasSelection && !secret) {
                        val minIdx = min(_caretPosition, selectionStart)
                        val maxIdx = max(_caretPosition, selectionStart)
                        scene?.context?.clipboard?.contents = text.substring(minIdx, maxIdx)
                        event.handle()
                    }
                }
                Key.X -> {
                    if (ctrl && hasSelection && !secret && editable) {
                        val minIdx = min(_caretPosition, selectionStart)
                        val maxIdx = max(_caretPosition, selectionStart)
                        scene?.context?.clipboard?.contents = text.substring(minIdx, maxIdx)
                        removeAtCaret(true)
                        event.handle()
                    }
                }
                Key.V -> {
                    if (ctrl && editable) {
                        if (hasSelection) {
                            removeAtCaret(true)
                        }
                        scene?.context?.clipboard?.contents?.let {
                            insertAtCaret(it)
                        }
                        event.handle()
                    }
                }
                Key.Z -> {
                    if (ctrl && !shift && editable) {
                        undo()
                        event.handle()
                        return
                    } else if (ctrl && shift && editable) {
                        redo()
                        event.handle()
                        return
                    }
                }
                else -> Unit
            }
        }

        if (event.type == InputEvent.Type.CHAR_TYPED) {
            if (editable) {
                if (hasSelection) {
                    removeAtCaret(true)
                }
                insertAtCaret(event.char.toString())
                event.handle()
            }
        }

        if (oldText != text) {
            val time = now().milliseconds
            if (time - 750.milliseconds > lastChanged) {
                undoStack.addFirst(oldText)
                redoStack.clear()
            }
            lastChanged = time
        }
    }

    private fun onTapped(event: InputEvent<*>) {
        val count = taps % 4
        if (count == 0) unselect()
        if (count == 2) {
            val indices = determineWordIndices(event.localX)
            select(indices[0], indices[1])
        }
        if (count == 3) {
            selectAll()
        }
    }

    override fun render(batch: Batch, camera: Camera) {
        super.render(batch, camera)

        val bgDrawable = if (editable) bg else bgDisabled
        bgDrawable.draw(
            batch,
            globalX,
            globalY,
            width = width,
            height = height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = rotation
        )

        if (displayTest.isNotEmpty()) {
            calculateVisibility()
        }

        if (hasFocus && hasSelection) {
            selection.draw(
                batch,
                globalX + textOffset + selectionX + fontOffset + bg.marginLeft,
                globalY + font.lineHeight / 4f,
                width = selectionWidth,
                height = font.capHeight,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = rotation,
            )
        }

        if (displayTest.isNotEmpty()) {
            val color = if (editable) fontColor else fontColorDisabled
            cache.setText(
                displayTest.substring(visibleStart, visibleEnd),
                globalX + bg.marginLeft + textOffset,
                globalY,
                scaleX,
                scaleY,
                rotation,
                color
            )
            cache.draw(batch)
        } else {
            if ((!hasFocus || !editable) && placeholderText.isNotEmpty()) {
                cache.setText(
                    placeholderText,
                    globalX + bg.marginLeft,
                    globalY,
                    scaleX,
                    scaleY,
                    rotation,
                    fontColorPlaceholder,
                    availableWidth,
                    truncate = "..."
                )
                cache.draw(batch)
            }
        }

        if (hasFocus) {
            if (editable) {
                caret.draw(
                    batch,
                    globalX + bg.marginLeft + textOffset + glyphPositions[_caretPosition] - glyphPositions[visibleStart] + fontOffset,
                    globalY + font.lineHeight / 4f,
                    width = caret.minWidth,
                    height = font.capHeight,
                    scaleX = globalScaleX,
                    scaleY = globalScaleY,
                    rotation = rotation,
                )
            }

            focusDrawable.draw(
                batch,
                globalX,
                globalY,
                width = width,
                height = height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = rotation
            )
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth = max(minWidth, bg.minWidth)
        _internalMinHeight = max(minHeight, bg.minHeight)

        minSizeInvalid = false
    }

    fun select(start: Int, end: Int) {
        check(start >= 0) { "'start' must be >= 0" }
        check(end >= 0) { "'end' must be >= 0" }
        var selectionStart = min(text.length, start)
        var selectionEnd = min(text.length, end)
        if (selectionEnd == selectionStart) {
            unselect()
            return
        }

        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart.also { selectionStart = selectionEnd }
        }
        hasSelection = true
        this.selectionStart = selectionStart
        _caretPosition = selectionEnd
    }

    fun selectAll() {
        select(0, text.length)
    }

    fun unselect() {
        hasSelection = false
        selectionStart = 0
        selectionWidth = 0f
    }

    fun undo() {
        if (undoStack.isEmpty()) return

        unselect()
        val oldText = text
        text = undoStack.removeFirst()
        redoStack.addFirst(oldText)
        _caretPosition = text.length
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        unselect()
        val oldText = text
        text = redoStack.removeFirst()
        undoStack.addFirst(oldText)
        _caretPosition = text.length
    }

    private fun updateText() {
        displayTest = text
        if (secret) {
            secretBuffer.clear()
            repeat(text.length) {
                secretBuffer.append(secretCharacter)
            }
            displayTest = secretBuffer.toString()
        }
        layout.setText(font, displayTest.replace('\r', ' ').replace('\n', ' '))
        glyphPositions.clear()
        var x = 0f
        fontOffset = 0f
        if (layout.runs.isNotEmpty()) {
            val run = layout.runs.first()
            fontOffset = run.advances.first()
            for (i in 1 until run.advances.size) {
                glyphPositions += x
                x += run.advances[i]
            }
        }

        glyphPositions += x

        visibleStart = min(visibleStart, glyphPositions.size - 1)
        visibleEnd = visibleEnd.clamp(visibleStart, glyphPositions.size - 1)

        selectionStart = min(selectionStart, displayTest.length)
    }

    private fun calculateVisibility() {
        _caretPosition = _caretPosition.clamp(0, glyphPositions.size - 1)
        val distance = glyphPositions[max(0, _caretPosition - 1)] + renderOffset
        if (distance <= 0f) {
            renderOffset -= distance
        } else {
            val index = min(glyphPositions.size - 1, _caretPosition + 1)
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
        var startX = 0f
        for (i in glyphPositions.indices) {
            if (glyphPositions[i] >= -renderOffset) {
                visibleStart = i
                startX = glyphPositions[i]
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

        textOffset = startX + renderOffset

        if (hasSelection) {
            val minIdx = min(_caretPosition, selectionStart)
            val maxIdx = max(_caretPosition, selectionStart)
            val minX = max(glyphPositions[minIdx] - glyphPositions[visibleStart], -textOffset)
            val maxX = min(glyphPositions[maxIdx] - glyphPositions[visibleStart], availableWidth - textOffset)
            selectionX = minX
            selectionWidth = maxX - minX
        }
    }

    private fun insertAtCaret(chars: CharSequence) {
        stringBuilder.clear()
        stringBuilder.append(text)
        stringBuilder.insert(_caretPosition, chars)
        _caretPosition += chars.length
        text = stringBuilder.toString()
    }

    private fun removeAtCaret(forward: Boolean) {
        if (!forward && caretPosition <= 0 && !hasSelection) return
        if (forward && caretPosition >= text.length && !hasSelection) return

        stringBuilder.clear()
        stringBuilder.append(text)
        if (hasSelection) {
            val start = selectionStart
            val end = _caretPosition
            val minIdx = min(start, end)
            val maxIdx = max(start, end)
            stringBuilder.deleteRange(minIdx, maxIdx)
            _caretPosition = minIdx
        } else {
            val index = if (forward) _caretPosition else --_caretPosition
            stringBuilder.deleteAt(index)
        }
        text = stringBuilder.toString()
        unselect()
    }

    private fun moveCaretToPosition(x: Float) {
        _caretPosition = determineGlyphPosition(x)
        _caretPosition = max(0, _caretPosition)
    }

    private fun determineWordIndices(tx: Float) = determineWordIndices(determineGlyphPosition(tx))

    private fun determineWordIndices(idx: Int): IntArray {
        var right = text.length
        var left = 0
        if (idx >= text.length) {
            left = text.length
            right = 0
        } else {
            for (i in idx until right) {
                if (!text[i].isLetterOrDigit()) {
                    right = i
                    break
                }
            }
            for (i in idx - 1 downTo 0) {
                if (!text[i].isLetterOrDigit()) {
                    left = i + 1
                    break
                }
            }
        }
        return intArrayOf(left, right)
    }

    private fun determineGlyphPosition(tx: Float): Int {
        val x = tx + fontOffset + glyphPositions[visibleStart] - bg.marginLeft
        for (i in 1 until glyphPositions.size) {
            if (glyphPositions[i] > x) {
                if (glyphPositions[i] - x <= x - glyphPositions[i - 1]) return i
                return i - 1
            }
        }
        val pos = glyphPositions.size - 1
        return max(pos, 0)
    }

    class ThemeVars {
        val fontColor = "fontColor"
        val fontColorDisabled = "fontColorDisabled"
        val fontColorPlaceholder = "fontColorPlaceholder"
        val font = "font"
        val bg = "bg"
        val pressed = "pressed"
        val disabled = "disabled"
        val focus = "focus"
        val caret = "caret"
        val selection = "selection"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [LineEdit]
         */
        val themeVars = ThemeVars()
        private val stringBuilder = StringBuilder()
    }
}