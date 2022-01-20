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
inline fun SceneGraph.paddedContainer(callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {}) =
    root.paddedContainer(callback)

/**
 * A [Container] with padding.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class PaddedContainer : Container() {

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [Button]
         */
        object ThemeVars {
            const val PADDING_LEFT = "paddingLeft"
            const val PADDING_RIGHT = "paddingRight"
            const val PADDING_TOP = "paddingTop"
            const val PADDING_BOTTOM = "paddingBottom"
        }
    }

    var paddingLeft: Int = 0
        get() = getThemeConstant(ThemeVars.PADDING_LEFT)
        set(value) {
            if (value == field) return
            constantsOverride[ThemeVars.PADDING_LEFT] = value
            onMinimumSizeChanged()
        }
    var paddingRight: Int = 0
        get() = getThemeConstant(ThemeVars.PADDING_RIGHT)
        set(value) {
            if (value == field) return
            constantsOverride[ThemeVars.PADDING_RIGHT] = value
            onMinimumSizeChanged()
        }
    var paddingTop: Int = 0
        get() = getThemeConstant(ThemeVars.PADDING_TOP)
        set(value) {
            if (value == field) return
            constantsOverride[ThemeVars.PADDING_TOP] = value
            onMinimumSizeChanged()
        }
    var paddingBottom: Int = 0
        get() = getThemeConstant(ThemeVars.PADDING_BOTTOM)
        set(value) {
            if (value == field) return
            constantsOverride[ThemeVars.PADDING_BOTTOM] = value
            onMinimumSizeChanged()
        }

    fun padding(value: Int) {
        paddingLeft = value
        paddingTop = value
        paddingBottom = value
        paddingRight = value
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
        nodes.forEach {
            if (it is Control && it.enabled) {
                val w = width - paddingLeft - paddingRight
                val h = height - paddingTop - paddingBottom
                fitChild(it, paddingLeft.toFloat(), paddingBottom.toFloat(), w, h)
                it.computeMargins()
            }
        }
    }
}