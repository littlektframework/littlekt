package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.font.TextureFont
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.input.Key
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

@PublishedApi
internal fun createSpinBox(value: Float, min: Float, max: Float, step: Float): SpinBox =
    SpinBox().also {
        it.min = min
        it.max = max
        it.step = step
        it.value = value
    }

/**
 * Adds a [SpinBox] to the current [Node] as a child and then triggers the [callback].
 *
 * @param value the starting value
 * @param min the lowest value
 * @param max the highest value
 * @param step the amount one arrow press, wheel notch or scrub step changes the value by
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.spinBox(
    value: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 1f,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker SpinBox.() -> Unit = {},
): SpinBox {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createSpinBox(value, min, max, step).addTo(this, modifier, callback)
}

/**
 * Adds a [SpinBox] to the current [SceneGraph.root] as a child and then triggers the [callback].
 *
 * @param value the starting value
 * @param min the lowest value
 * @param max the highest value
 * @param step the amount one arrow press, wheel notch or scrub step changes the value by
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.spinBox(
    value: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 1f,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker SpinBox.() -> Unit = {},
): SpinBox {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.spinBox(value, min, max, step, modifier = modifier, callback = callback)
}

/**
 * A number field: drag across it to scrub the value, press the arrows at its sides or scroll to
 * step it, or click it to type a value (Enter keeps it, Escape cancels). Listen to
 * [onValueChanged] for changes.
 *
 * ```
 * spinBox(value = 45f, min = 1f, max = 500f) {
 *     suffix = " hp"
 *     onValueChanged += { hp = it.toInt() }
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
open class SpinBox : Range() {
    private val scaledText = ScaledText()
    private val layout = GlyphLayout()
    private val editor = Editor()
    private var textDirty = true
    private var laidOutWidth = -1f
    private var shown = ""
    private var pressX = 0f
    private var startValue = 0f
    private var pressArrow = 0
    private var scrubbing = false
    private var pointerDown = false

    /** Text drawn before the value, such as `"x "`. */
    var prefix: String = ""
        set(value) {
            if (field == value) return
            field = value
            refreshText()
        }

    /** Text drawn after the value, such as `" ms"`. */
    var suffix: String = ""
        set(value) {
            if (field == value) return
            field = value
            refreshText()
        }

    /** Digits shown after the decimal point, or `-1` to use as many as [step] has. */
    var decimals: Int = -1
        set(value) {
            if (field == value) return
            field = value
            refreshText()
        }

    /** Pixels the pointer moves for each [step] while scrubbing. */
    var scrubPixels: Float = 6f

    /** Pixels the pointer must move before a press becomes a scrub. */
    var scrubThreshold: Float = 3f

    /** Whether clicking the field lets the value be typed. */
    var editable: Boolean = true

    /** Whether the field ignores input and draws dimmed. */
    var disabled: Boolean = false

    /** Whether the value is being typed. */
    val isEditing: Boolean
        get() = editor.visible

    /** The background of the field. */
    var normal: Drawable
        get() = getThemeDrawable(themeVars.normal)
        set(value) {
            drawableOverrides[themeVars.normal] = value
            onMinimumSizeChanged()
        }

    /** The background while the pointer is over the field. */
    var hover: Drawable
        get() = getThemeDrawable(themeVars.hover)
        set(value) {
            drawableOverrides[themeVars.hover] = value
        }

    /** The color of the text. */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /** The font of the text. */
    var font: TextureFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            refreshText()
        }

    /** The width, in pixels, of the arrow areas at each side. */
    var arrowWidth: Int
        get() = getThemeConstant(themeVars.arrowWidth)
        set(value) {
            constantOverrides[themeVars.arrowWidth] = value
            onMinimumSizeChanged()
        }

    private var hovering = false

    init {
        focusMode = FocusMode.ALL
        editor.visible = false
        addChild(editor)
        onValueChanged += { refreshText() }
    }

    /** The value as the field shows it, without [prefix] or [suffix]. */
    fun formatValue(): String {
        val d = if (decimals >= 0) decimals else decimalsOf(step)
        if (d == 0) return value.roundToLong().toString()
        val p = 10.0.pow(d)
        val scaled = (value.toDouble() * p).roundToLong()
        val negative = scaled < 0
        val digits = abs(scaled).toString().padStart(d + 1, '0')
        val whole = digits.substring(0, digits.length - d)
        val frac = digits.substring(digits.length - d)
        return (if (negative) "-" else "") + whole + "." + frac
    }

    private fun decimalsOf(s: Float): Int {
        if (s <= 0f) return 2
        var d = 0
        var v = s.toDouble()
        while (d < 6 && abs(v - v.roundToLong()) > 1e-6) {
            v *= 10.0
            d++
        }
        return d
    }

    private fun refreshText() {
        textDirty = true
        onMinimumSizeChanged()
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return
        layoutText()
        val bg = normal
        val arrows = arrowWidth * 2f
        _internalMinWidth = max(layout.width + arrows + bg.marginLeft + bg.marginRight, bg.minWidth)
        _internalMinHeight = max(font.metrics.lineHeight / scaledText.ratio + bg.marginTop + bg.marginBottom, bg.minHeight)
        minSizeInvalid = false
    }

    private fun layoutText() {
        val font = scaledText.update(font, pixelScale)
        val scale = 1f / scaledText.ratio
        shown = prefix + formatValue() + suffix
        layout.setText(font, shown, Color.WHITE, 0f, scaleX = scale, scaleY = scale, HAlign.LEFT, false, null)
        val lineHeight = font.metrics.lineHeight * scale
        val tx = (width - layout.width) * 0.5f
        val ty = (height - lineHeight) * 0.5f
        scaledText.cache.setText(layout, scaledText.snap(tx) - originX, scaledText.snap(ty) - originY, scale, scale)
        laidOutWidth = width
        textDirty = false
    }

    override fun onHierarchyChanged(flag: Int) {
        super.onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY) {
            textDirty = true
            editor.x = 0f
            editor.y = 0f
            editor.width = width
            editor.height = height
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        val bg = if (hovering && !disabled) hover else normal
        tempColor.set(bg.tint)
        if (disabled) tempColor.a *= DISABLED_ALPHA
        bg.draw(batch, globalX - originX, globalY - originY, 0f, 0f, width, height, color = tempColor)
        if (isEditing) return
        if (scaledText.font == null || pixelScale != scaledText.pixelScale) {
            textDirty = true
            onMinimumSizeChanged()
        }
        if (textDirty || laidOutWidth != width) layoutText()
        tempColor.set(color).mul(fontColor)
        if (disabled) tempColor.a *= DISABLED_ALPHA
        scaledText.cache.tint(tempColor)
        scaledText.cache.setPosition(scaledText.snap(globalX), scaledText.snap(globalY))
        scaledText.draw(batch)
        val a = arrowWidth.toFloat()
        val s = a * ARROW_SHARE
        val cy = globalY - originY + height * 0.5f
        val lx = globalX - originX + a * 0.5f
        val rx = globalX - originX + width - a * 0.5f
        shapeRenderer.filledTriangle(lx + s * 0.35f, cy + s * 0.5f, lx + s * 0.35f, cy - s * 0.5f, lx - s * 0.35f, cy, color = tempColor)
        shapeRenderer.filledTriangle(rx - s * 0.35f, cy + s * 0.5f, rx - s * 0.35f, cy - s * 0.5f, rx + s * 0.35f, cy, color = tempColor)
    }

    private fun arrowAt(localX: Float): Int {
        val a = arrowWidth.toFloat()
        return when {
            localX < a -> -1
            localX >= width - a -> 1
            else -> 0
        }
    }

    /** Changes the value by [steps] steps. */
    fun stepBy(steps: Int) {
        value += steps * (if (step > 0f) step else (max - min) / 100f)
    }

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)
        if (disabled || isEditing) return
        when (event.type) {
            InputEvent.Type.MOUSE_ENTER -> hovering = true
            InputEvent.Type.MOUSE_EXIT -> hovering = false
            InputEvent.Type.TOUCH_DOWN -> {
                pointerDown = true
                pressX = event.localX
                startValue = value
                pressArrow = arrowAt(event.localX)
                scrubbing = false
                event.handle()
            }
            InputEvent.Type.TOUCH_DRAGGED -> {
                if (!pointerDown) return
                val dx = event.localX - pressX
                if (!scrubbing && pressArrow == 0 && abs(dx) >= scrubThreshold) scrubbing = true
                if (scrubbing) {
                    val steps = (dx / scrubPixels).toInt()
                    value = startValue + steps * (if (step > 0f) step else (max - min) / 100f)
                }
                event.handle()
            }
            InputEvent.Type.TOUCH_UP -> {
                if (!pointerDown) return
                pointerDown = false
                when {
                    scrubbing -> scrubbing = false
                    pressArrow != 0 -> stepBy(pressArrow)
                    editable -> beginEdit()
                }
                event.handle()
            }
            InputEvent.Type.SCROLLED -> {
                if (event.scrollAmountY != 0f) {
                    stepBy(if (event.scrollAmountY > 0f) -1 else 1)
                    event.handle()
                }
            }
            else -> {}
        }
    }

    /** Shows the typing field with the current value selected. */
    fun beginEdit() {
        editor.text = formatValue()
        editor.x = 0f
        editor.y = 0f
        editor.width = width
        editor.height = height
        editor.visible = true
        scene?.requestFocus(editor)
        editor.selectAll()
    }

    private fun endEdit(keep: Boolean) {
        if (!editor.visible) return
        if (keep) editor.text.trim().toFloatOrNull()?.let { value = it }
        editor.visible = false
        textDirty = true
    }

    override fun onDestroy() {
        super.onDestroy()
        onValueChanged.clear()
    }

    private inner class Editor : LineEdit() {
        init {
            onFocusLost += { endEdit(true) }
        }

        override fun uiInput(event: InputEvent<*>) {
            if (event.type == InputEvent.Type.KEY_DOWN && event.key == Key.ENTER) {
                endEdit(true)
                scene?.releaseFocus()
                event.handle()
                return
            }
            if (event.type == InputEvent.Type.KEY_DOWN && event.key == Key.ESCAPE) {
                endEdit(false)
                scene?.releaseFocus()
                event.handle()
                return
            }
            super.uiInput(event)
        }
    }

    /** [Theme] related variable names of a [SpinBox]. */
    class ThemeVars {
        val normal = "normal"
        val hover = "hover"
        val fontColor = "fontColor"
        val font = "font"
        val arrowWidth = "arrowWidth"
    }

    companion object {
        private const val DISABLED_ALPHA = 0.45f
        private const val ARROW_SHARE = 0.55f
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for a [SpinBox]. */
        val themeVars = ThemeVars()
    }
}
