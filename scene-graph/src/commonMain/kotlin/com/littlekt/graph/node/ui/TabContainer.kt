package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.util.SingleSignal
import com.littlekt.graph.util.signal1v
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.util.datastructure.fastForEach
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

/**
 * Adds a [TabContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.tabContainer(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker TabContainer.() -> Unit = {},
): TabContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return TabContainer().addTo(this, modifier, callback)
}

/**
 * Adds a [TabContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.tabContainer(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker TabContainer.() -> Unit = {},
): TabContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.tabContainer(modifier = modifier, callback = callback)
}

/**
 * A [Container] that shows one of its [Control] children at a time, as tabs. A row of tab buttons,
 * the [tabBar], sits above the shown child and switches the [currentTab]. A tab's title is the
 * child's [Node.name], or the name of the control inside its [Modifier.Wrapper]s, unless set with
 * [setTabTitle]. The container sets the [Control.visible] of its tabs.
 *
 * ```
 * tabContainer {
 *     column { name = "Stats"; label("Strength 10") }
 *     column { name = "Skills"; label("Fireball") }
 *     onTabChanged += { println("Showing tab $it") }
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/23/2026
 */
open class TabContainer : Container() {

    private val tabs = ArrayList<Control>()
    private val titles = HashMap<Control, String>()
    private val tabButtons = ArrayList<BaseButton>()
    private var _currentTab = 0

    /**
     * The [HBoxContainer] of tab buttons. Change its [HBoxContainer.separation] or
     * [HBoxContainer.align] to lay out the tabs.
     */
    val tabBar: HBoxContainer = HBoxContainer().apply { name = "Tab Bar" }

    /** Emitted with the index of the new [currentTab] when it changes. */
    val onTabChanged: SingleSignal<Int> = signal1v()

    /** The index of the shown tab. Clamped to the tabs that exist. */
    var currentTab: Int
        get() = _currentTab
        set(value) {
            val index = clampTab(value)
            if (index == _currentTab) return
            _currentTab = index
            updateTabs()
            onTabChanged.emit(index)
        }

    /** The shown tab, if any. */
    val currentTabControl: Control?
        get() = tabs.getOrNull(_currentTab)

    /** The number of tabs. */
    val tabCount: Int
        get() = tabs.size

    /**
     * If `true`, the minimum size fits every tab, so switching tabs doesn't resize the container.
     * If `false`, only the [currentTab] is used.
     */
    var useHiddenTabsForMinSize: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

    /** The [Drawable] behind the tabs. Defaults to the theme's [Panel] panel. */
    var panel: Drawable
        get() =
            if (hasThemeDrawable(themeVars.panel)) getThemeDrawable(themeVars.panel)
            else getThemeDrawable(themeVars.panel, "Panel")
        set(value) {
            drawableOverrides[themeVars.panel] = value
            onMinimumSizeChanged()
        }

    init {
        addChild(tabBar)
    }

    /** @return the tab at [index], if any */
    fun getTab(index: Int): Control? = tabs.getOrNull(index)

    /** @return the title of the tab at [index] */
    fun getTabTitle(index: Int): String {
        val tab = tabs[index]
        return titles[tab] ?: (tab.modifierWrapped ?: tab).name
    }

    /** Sets the [title] of the tab at [index], instead of its [Node.name]. */
    fun setTabTitle(index: Int, title: String) {
        titles[tabs[index]] = title
        rebuildTabButtons()
    }

    /** @return the tab button of the tab at [index], if any */
    fun getTabButton(index: Int): BaseButton? = tabButtons.getOrNull(index)

    /**
     * Creates the button of a tab. Override to use a custom tab button.
     *
     * @param title the title of the tab
     * @return a new button for the [tabBar]
     */
    protected open fun createTabButton(title: String): BaseButton = Button().apply { text = title }

    override fun onChildAdded(child: Node) {
        super.onChildAdded(child)
        if (child !== tabBar && child is Control) updateTabList()
    }

    override fun onChildRemoved(child: Node) {
        super.onChildRemoved(child)
        if (child !== tabBar && child is Control) updateTabList()
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val panel = panel
        panel.draw(
            batch = batch,
            x = globalX - originX,
            y = globalY - originY,
            originX = originX,
            originY = originY,
            width = width,
            height = height - tabBar.height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation,
            color = panel.tint,
        )
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var tabWidth = 0f
        var tabHeight = 0f
        for (i in tabs.indices) {
            val tab = tabs[i]
            if (!tab.enabled || tab.isDestroyed) continue
            if (!useHiddenTabsForMinSize && i != _currentTab) continue
            tabWidth = max(tabWidth, tab.combinedMinWidth)
            tabHeight = max(tabHeight, tab.combinedMinHeight)
        }

        val panel = panel
        _internalMinWidth = max(tabBar.combinedMinWidth, tabWidth + panel.minWidth)
        _internalMinHeight = tabBar.combinedMinHeight + tabHeight + panel.minHeight

        minSizeInvalid = false
    }

    override fun onSortChildren() {
        val barHeight = tabBar.combinedMinHeight
        fitChild(tabBar, 0f - originX, height - barHeight - originY, width, barHeight)

        val panel = panel
        val w = width - panel.marginLeft - panel.marginRight
        val h = height - barHeight - panel.marginTop - panel.marginBottom
        tabs.fastForEach {
            if (it.enabled && !it.isDestroyed) {
                fitChild(it, panel.marginLeft - originX, panel.marginBottom - originY, w, h)
            }
        }
    }

    private fun updateTabList() {
        if (isDestroyed) return
        tabs.clear()
        nodes.forEach { if (it is Control && it !== tabBar && !it.isDestroyed) tabs += it }
        titles.keys.retainAll(tabs.toSet())
        rebuildTabButtons()
        val index = clampTab(_currentTab)
        if (index != _currentTab) {
            _currentTab = index
            updateTabs()
            onTabChanged.emit(index)
        }
    }

    private fun rebuildTabButtons() {
        tabButtons.fastForEach { it.destroy() }
        tabButtons.clear()
        val group = ButtonGroup()
        for (i in tabs.indices) {
            val button = createTabButton(getTabTitle(i))
            button.toggleMode = true
            button.setButtonGroup(group)
            button.onPressed += { currentTab = i }
            tabButtons += button
            tabBar.addChild(button)
        }
        updateTabs()
    }

    private fun updateTabs() {
        for (i in tabs.indices) {
            tabs[i].visible = i == _currentTab
        }
        tabButtons.getOrNull(_currentTab)?.pressed = true
        onMinimumSizeChanged()
        queueSort()
    }

    private fun clampTab(index: Int): Int = if (tabs.isEmpty()) 0 else index.coerceIn(0, tabs.lastIndex)

    class ThemeVars {
        val panel = "panel"
    }

    companion object {
        /** [Theme] related variable names when setting theme values for a [TabContainer] */
        val themeVars = ThemeVars()
    }
}
