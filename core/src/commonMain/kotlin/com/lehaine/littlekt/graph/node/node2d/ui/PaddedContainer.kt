package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Theme

/**
 * Adds a [PaddedContainer] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.paddedContainer(callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {}) =
    PaddedContainer().also(callback).addTo(this)

/**
 * Adds a [PaddedContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph<*>.paddedContainer(callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {}) =
    root.paddedContainer(callback)

/**
 * A [Container] with padding.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class PaddedContainer : Container() {

    class ThemeVars {
        val paddingLeft = "paddingLeft"
        val paddingRight = "paddingRight"
        val paddingTop = "paddingTop"
        val paddingBottom = "paddingBottom"
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [PaddedContainer]
         */
        val themeVars = ThemeVars()
    }

    var paddingLeft: Int = 0
        get() = getThemeConstant(themeVars.paddingLeft)
        set(value) {
            if (value == field) return
            constantOverrides[themeVars.paddingLeft] = value
            onMinimumSizeChanged()
        }
    var paddingRight: Int = 0
        get() = getThemeConstant(themeVars.paddingRight)
        set(value) {
            if (value == field) return
            constantOverrides[themeVars.paddingRight] = value
            onMinimumSizeChanged()
        }
    var paddingTop: Int = 0
        get() = getThemeConstant(themeVars.paddingTop)
        set(value) {
            if (value == field) return
            constantOverrides[themeVars.paddingTop] = value
            onMinimumSizeChanged()
        }
    var paddingBottom: Int = 0
        get() = getThemeConstant(themeVars.paddingBottom)
        set(value) {
            if (value == field) return
            constantOverrides[themeVars.paddingBottom] = value
            onMinimumSizeChanged()
        }

    fun padding(value: Int) {
        constantOverrides[themeVars.paddingRight] = value
        constantOverrides[themeVars.paddingTop] = value
        constantOverrides[themeVars.paddingBottom] = value
        constantOverrides[themeVars.paddingLeft] = value
        onMinimumSizeChanged()
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

        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom

        _internalMinWidth = maxWidth
        _internalMinHeight = maxHeight
        minSizeInvalid = false
    }

    override fun onSortChildren() {
        val width = width
        val height = height
        nodes.forEach {
            if (it is Control && it.enabled) {
                val w = width - paddingLeft - paddingRight
                val h = height - paddingTop - paddingBottom
                fitChild(it, paddingLeft.toFloat(), paddingTop.toFloat(), w, h)
                it.computeMargins()
            }
        }
    }
}