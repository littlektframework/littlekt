package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [PanelContainer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [PanelContainer] context in order to initialize any values
 * @return the newly created [PanelContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.panelContainer(callback: @SceneGraphDslMarker PanelContainer.() -> Unit = {}): PanelContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return PanelContainer().also(callback).addTo(this)
}

/**
 * Adds a [PanelContainer] to the  [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [PanelContainer] context in order to initialize any values
 * @return the newly created [PanelContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.panelContainer(callback: @SceneGraphDslMarker PanelContainer.() -> Unit = {}): PanelContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.panelContainer(callback)
}

/**
 * A [Container] that fits controls inside the area of the [Drawable].
 * @author Colton Daily
 * @date 1/21/2022
 */
open class PanelContainer : Container() {

    var panel: Drawable
        get() = if (hasThemeDrawable(themeVars.panel)) getThemeDrawable(themeVars.panel) else getThemeDrawable(
            themeVars.panel,
            "Panel"
        )
        set(value) {
            drawableOverrides[themeVars.panel] = value
            onMinimumSizeChanged()
        }

    override fun render(batch: Batch, camera: Camera) {
        panel.let {
            it.draw(batch, globalX, globalY, width, height, scaleX, scaleY, rotation, it.modulate)
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var maxWidth = 0f
        var maxHeight = 0f

        nodes.forEach {
            if (it is Control && it.enabled) {
                if (it.combinedMinWidth > maxWidth) {
                    maxWidth = it.combinedMinWidth
                }
                if (it.combinedMinHeight > maxHeight) {
                    maxHeight = it.combinedMinHeight
                }
            }
        }

        maxWidth += panel.minWidth
        maxHeight += panel.minHeight

        _internalMinWidth = maxWidth
        _internalMinHeight = maxHeight

        minSizeInvalid = false
    }

    override fun onSortChildren() {
        var w = width
        var h = height
        w -= panel.minWidth
        h -= panel.minHeight

        nodes.forEach {
            if (it is Control && it.enabled) {
                fitChild(it, 0f, 0f, w, h)
            }
        }
    }

    class ThemeVars {
        val panel = "panel"
    }

    companion object {
        val themeVars = ThemeVars()
    }
}