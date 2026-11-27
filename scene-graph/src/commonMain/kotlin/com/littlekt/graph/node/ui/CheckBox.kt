package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Camera
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

@PublishedApi
internal fun createCheckBox(text: String?, checked: Boolean): CheckBox =
    CheckBox().also {
        if (text != null) it.text = text
        it.checked = checked
    }

/**
 * Adds a [CheckBox] to the current [Node] as a child and then triggers the [callback].
 *
 * @param text the text next to the box
 * @param checked whether the box starts checked
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.checkBox(
    text: String? = null,
    checked: Boolean = false,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker CheckBox.() -> Unit = {},
): CheckBox {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createCheckBox(text, checked).addTo(this, modifier, callback)
}

/**
 * Adds a [CheckBox] to the current [SceneGraph.root] as a child and then triggers the [callback].
 *
 * @param text the text next to the box
 * @param checked whether the box starts checked
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.checkBox(
    text: String? = null,
    checked: Boolean = false,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker CheckBox.() -> Unit = {},
): CheckBox {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.checkBox(text, checked, modifier = modifier, callback = callback)
}

/**
 * A [Button] in toggle mode that draws a box, with a check mark while [checked], to the left of
 * its text. Listen to [onToggled] for changes.
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
open class CheckBox : Button() {

    init {
        toggleMode = true
        horizontalAlign = HAlign.LEFT
    }

    /** Whether the box is checked. Setting it emits [onToggled] when it changes. */
    var checked: Boolean
        get() = pressed
        set(value) {
            pressed = value
        }

    /** The box drawn behind the check mark. */
    var box: Drawable
        get() = getThemeDrawable(themeVars.box)
        set(value) {
            drawableOverrides[themeVars.box] = value
            onMinimumSizeChanged()
        }

    /** The check mark drawn over the [box] while [checked]. */
    var check: Drawable
        get() = getThemeDrawable(themeVars.check)
        set(value) {
            drawableOverrides[themeVars.check] = value
        }

    /** The space, in pixels, between the box and the text. */
    var separation: Int
        get() = getThemeConstant(themeVars.separation)
        set(value) {
            constantOverrides[themeVars.separation] = value
            onMinimumSizeChanged()
        }

    /** The space, in pixels, between the edge of the box and the check mark. */
    var checkInset: Int
        get() = getThemeConstant(themeVars.checkInset)
        set(value) {
            constantOverrides[themeVars.checkInset] = value
        }

    private val boxSize: Float
        get() = max(box.minWidth, box.minHeight)

    override val contentLeft: Float
        get() = boxSize + if (text.isEmpty()) 0f else separation.toFloat()

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val size = boxSize
        val x = globalX - originX + padding
        val y = globalY - originY + (height - size) * 0.5f
        tempColor.set(box.tint)
        if (disabled) tempColor.a *= DISABLED_ALPHA
        box.draw(batch, x, y, 0f, 0f, size, size, color = tempColor)
        if (checked) {
            tempColor.set(check.tint)
            if (disabled) tempColor.a *= DISABLED_ALPHA
            val inset = checkInset.toFloat()
            check.draw(batch, x + inset, y + inset, 0f, 0f, size - inset * 2f, size - inset * 2f, color = tempColor)
        }
    }

    /** [Theme] related variable names of a [CheckBox], besides those of [Button.themeVars]. */
    class ThemeVars {
        val box = "box"
        val check = "check"
        val separation = "separation"
        val checkInset = "checkInset"
    }

    companion object {
        private const val DISABLED_ALPHA = 0.45f
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for a [CheckBox]. */
        val themeVars = ThemeVars()
    }
}
