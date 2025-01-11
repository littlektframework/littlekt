package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [PanelContainer] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [PanelContainer] context in order to
 *   initialize any values
 * @return the newly created [PanelContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.panelContainer(
    callback: @SceneGraphDslMarker PanelContainer.() -> Unit = {}
): PanelContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return PanelContainer().also(callback).addTo(this)
}

/**
 * Adds a [PanelContainer] to the [SceneGraph.root] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [PanelContainer] context in order to
 *   initialize any values
 * @return the newly created [PanelContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.panelContainer(
    callback: @SceneGraphDslMarker PanelContainer.() -> Unit = {}
): PanelContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.panelContainer(callback)
}

/**
 * A [Container] that fits controls inside the area of the [Drawable].
 *
 * @author Colton Daily
 * @date 1/21/2022
 */
open class PanelContainer : Container() {

    var panel: Drawable
        get() =
            if (hasThemeDrawable(themeVars.panel)) getThemeDrawable(themeVars.panel)
            else getThemeDrawable(themeVars.panel, "Panel")
        set(value) {
            drawableOverrides[themeVars.panel] = value
            onMinimumSizeChanged()
        }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        panel.let {
            it.draw(
                batch = batch,
                x = globalX - originX,
                y = globalY - originY,
                originX = originX,
                originY = originY,
                width = width,
                height = height,
                scaleX = scaleX,
                scaleY = scaleY,
                rotation = rotation,
                color = it.tint,
            )
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        var maxWidth = 0f
        var maxHeight = 0f

        nodes.forEach {
            if (it is Control && it.enabled && it.visible && !it.isDestroyed) {
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
        w -= panel.marginRight + panel.marginLeft
        h -= panel.marginBottom + panel.marginTop

        nodes.forEach {
            if (it is Control && it.enabled && !it.isDestroyed) {
                fitChild(it, panel.marginLeft - originX, panel.marginBottom - originY, w, h)
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
