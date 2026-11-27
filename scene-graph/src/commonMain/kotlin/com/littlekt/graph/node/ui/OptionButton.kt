package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.util.SingleSignal
import com.littlekt.graphics.Camera
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal fun createOptionButton(items: List<String>, selected: Int): OptionButton =
    OptionButton().also {
        it.setItems(items)
        it.selected = selected
    }

/**
 * Adds an [OptionButton] to the current [Node] as a child and then triggers the [callback].
 *
 * @param items the choices
 * @param selected the index of the chosen item, or `-1` for none
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.optionButton(
    items: List<String> = emptyList(),
    selected: Int = if (items.isEmpty()) -1 else 0,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker OptionButton.() -> Unit = {},
): OptionButton {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createOptionButton(items, selected).addTo(this, modifier, callback)
}

/**
 * Adds an [OptionButton] to the current [SceneGraph.root] as a child and then triggers the
 * [callback].
 *
 * @param items the choices
 * @param selected the index of the chosen item, or `-1` for none
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.optionButton(
    items: List<String> = emptyList(),
    selected: Int = if (items.isEmpty()) -1 else 0,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker OptionButton.() -> Unit = {},
): OptionButton {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.optionButton(items, selected, modifier = modifier, callback = callback)
}

/**
 * A [Button] that shows the chosen item of a list and opens a [PopupList] of every item when
 * pressed. Listen to [onItemSelected] for choices made in the list.
 *
 * ```
 * optionButton(listOf("Grass", "Sand", "Stone")) {
 *     onItemSelected += { index -> println(items[index]) }
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
open class OptionButton : Button() {
    private val _items = ArrayList<String>()

    /** The choices. */
    val items: List<String>
        get() = _items

    /** Triggered with the index of an item chosen in the list. Setting [selected] does not trigger it. */
    val onItemSelected: SingleSignal<Int> = SingleSignal()

    /** The most items the list shows before it scrolls. */
    var maxVisibleItems = 12

    /** The text shown while nothing is selected. */
    var placeholder: String = ""
        set(value) {
            field = value
            if (selected < 0) text = value
        }

    /** The index of the chosen item, or `-1` for none. */
    var selected: Int = -1
        set(value) {
            field = if (value in _items.indices) value else -1
            text = if (field >= 0) _items[field] else placeholder
        }

    /** The chosen item, or `null` for none. */
    val selectedItem: String?
        get() = if (selected >= 0) _items[selected] else null

    /** The space, in pixels, between the text and the arrow. */
    var separation: Int
        get() = getThemeConstant(themeVars.separation)
        set(value) {
            constantOverrides[themeVars.separation] = value
            onMinimumSizeChanged()
        }

    /** The width, in pixels, of the arrow. */
    var arrowSize: Int
        get() = getThemeConstant(themeVars.arrowSize)
        set(value) {
            constantOverrides[themeVars.arrowSize] = value
            onMinimumSizeChanged()
        }

    /** The list while it is open. */
    var popup: PopupList? = null
        private set

    init {
        horizontalAlign = HAlign.LEFT
    }

    /** Adds an item to the end of the list. */
    fun addItem(text: String) {
        _items += text
        if (selected < 0 && placeholder.isEmpty() && _items.size == 1) selected = 0 else onMinimumSizeChanged()
    }

    /** Replaces every item, keeping the selection when its index still exists. */
    fun setItems(items: List<String>) {
        _items.clear()
        _items.addAll(items)
        selected = selected
    }

    /** Removes every item and clears the selection. */
    fun clearItems() {
        _items.clear()
        selected = -1
    }

    /** Opens the list of items, if there are any. */
    fun openList() {
        if (_items.isEmpty() || scene == null || popup?.isOpen == true) return
        val list = PopupList()
        list.maxVisibleItems = maxVisibleItems
        list.onItemSelected += {
            selected = it
            onItemSelected.emit(it)
        }
        list.onClosed += { popup = null }
        popup = list
        list.open(this, _items, selected)
    }

    override fun pressed() {
        super.pressed()
        openList()
    }

    override val contentRight: Float
        get() = arrowSize.toFloat() + separation

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val s = arrowSize.toFloat()
        val cx = globalX - originX + width - padding - s * 0.5f
        val cy = globalY - originY + height * 0.5f
        tempColor.set(color).mul(if (disabled && hasThemeColor(Button.themeVars.fontColorDisabled)) fontColorDisabled else fontColor)
        shapeRenderer.filledTriangle(cx - s * 0.5f, cy + s * 0.25f, cx + s * 0.5f, cy + s * 0.25f, cx, cy - s * 0.25f, color = tempColor)
    }

    override fun onDestroy() {
        super.onDestroy()
        popup?.close()
        onItemSelected.clear()
    }

    /** [Theme] related variable names of an [OptionButton], besides those of [Button.themeVars]. */
    class ThemeVars {
        val separation = "separation"
        val arrowSize = "arrowSize"
    }

    companion object {
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for an [OptionButton]. */
        val themeVars = ThemeVars()
    }
}
