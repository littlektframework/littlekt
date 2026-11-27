package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.util.Signal
import com.littlekt.graph.util.SingleSignal
import com.littlekt.graphics.HAlign
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

/**
 * A list of choices shown over the whole scene, such as the one an [OptionButton] opens. It covers
 * the canvas so a press outside the list closes it, and scrolls once there are more than
 * [maxVisibleItems] choices.
 *
 * ```
 * PopupList().apply {
 *     onItemSelected += { index -> println("picked $index") }
 *     open(anchor, listOf("Grass", "Sand", "Stone"), selected = 0)
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
open class PopupList : Control() {
    /** Triggered with the index of the choice that was pressed, right before the list closes. */
    val onItemSelected: SingleSignal<Int> = SingleSignal()

    /** Triggered when the list closes, picked or not. */
    val onClosed: Signal = Signal()

    /** The most choices shown before the list scrolls. */
    var maxVisibleItems = 12

    /** Whether the list is open. */
    val isOpen: Boolean
        get() = scene != null && !closing

    private val panelContainer = PanelContainer()
    private val scroll = ScrollContainer()
    private val list = VBoxContainer()
    private var closing = false

    /** The background of the list. */
    var panel: Drawable
        get() = getThemeDrawable(themeVars.panel)
        set(value) {
            drawableOverrides[themeVars.panel] = value
        }

    init {
        mouseFilter = MouseFilter.STOP
        scroll.addChild(list)
        panelContainer.addChild(scroll)
        addChild(panelContainer)
    }

    /**
     * Shows [items] under [anchor], or over it when there is no room below, and highlights the
     * choice at [selected]. The list is added to the root of [anchor]'s scene.
     */
    fun open(anchor: Control, items: List<String>, selected: Int = -1) {
        val scene = anchor.scene ?: return
        closing = false
        list.destroyAllChildren()
        val normal = getThemeDrawable(themeVars.item)
        val hover = getThemeDrawable(themeVars.itemHover)
        val current = getThemeDrawable(themeVars.itemSelected)
        for (i in items.indices) {
            val item = Button()
            item.text = items[i]
            item.horizontalAlign = HAlign.LEFT
            item.padding = ITEM_PADDING
            item.drawableOverrides[Button.themeVars.normal] = if (i == selected) current else normal
            item.drawableOverrides[Button.themeVars.hover] = hover
            item.drawableOverrides[Button.themeVars.pressed] = hover
            item.drawableOverrides[Button.themeVars.hoverPressed] = hover
            item.drawableOverrides[Button.themeVars.focus] = normal
            item.onPressed += { pick(i) }
            list.addChild(item)
        }
        panelContainer.panel = panel
        if (parent == null) scene.root.addChild(this)
        val canvasWidth = scene.sceneCanvas.virtualWidth
        val canvasHeight = scene.sceneCanvas.virtualHeight
        x = 0f
        y = 0f
        width = canvasWidth
        height = canvasHeight

        val bg = panelContainer.panel
        var itemHeight = 0f
        var itemWidth = 0f
        list.nodes.forEach {
            if (it is Control) {
                itemHeight = max(itemHeight, it.combinedMinHeight)
                itemWidth = max(itemWidth, it.combinedMinWidth)
            }
        }
        val visible = min(items.size, maxVisibleItems).coerceAtLeast(1)
        val gaps = (visible - 1) * list.separation
        val h = min(visible * itemHeight + gaps + bg.marginTop + bg.marginBottom, canvasHeight)
        val w = min(max(anchor.width, itemWidth + bg.marginLeft + bg.marginRight + if (items.size > visible) SCROLLBAR_ROOM else 0f), canvasWidth)
        list.minWidth = w - bg.marginLeft - bg.marginRight - if (items.size > visible) SCROLLBAR_ROOM else 0f
        panelContainer.minWidth = w
        panelContainer.minHeight = h
        panelContainer.width = w
        panelContainer.height = h
        val below = anchor.globalY - h
        val py = if (below >= 0f) below else min(anchor.globalY + anchor.height, canvasHeight - h)
        panelContainer.x = anchor.globalX.coerceIn(0f, max(0f, canvasWidth - w))
        panelContainer.y = py.coerceAtLeast(0f)
    }

    /** Closes the list without picking anything. */
    fun close() {
        if (closing || scene == null) return
        closing = true
        visible = false
        onClosed.emit()
    }

    private fun pick(index: Int) {
        if (closing) return
        onItemSelected.emit(index)
        close()
    }

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)
        if (event.type == InputEvent.Type.TOUCH_DOWN) {
            close()
            event.handle()
        }
    }

    override fun update(dt: Duration) {
        super.update(dt)
        if (closing) {
            closing = false
            destroy()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onItemSelected.clear()
        onClosed.clear()
    }

    /** [Theme] related variable names of a [PopupList]. */
    class ThemeVars {
        val panel = "panel"
        val item = "item"
        val itemHover = "itemHover"
        val itemSelected = "itemSelected"
    }

    companion object {
        private const val SCROLLBAR_ROOM = 14f
        private const val ITEM_PADDING = 6f

        /** [Theme] related variable names when setting theme values for a [PopupList]. */
        val themeVars = ThemeVars()
    }
}
