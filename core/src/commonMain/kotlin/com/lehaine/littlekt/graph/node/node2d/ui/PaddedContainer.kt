package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker

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
    private var _paddingLeft = 0f
    private var _paddingRight = 0f
    private var _paddingTop = 0f
    private var _paddingBottom = 0f

    var paddingLeft: Float
        get() = _paddingLeft
        set(value) {
            if (value == _paddingLeft) return
            _paddingLeft = value
            onMinimumSizeChanged()
        }
    var paddingRight: Float
        get() = _paddingRight
        set(value) {
            if (value == _paddingRight) return
            _paddingRight = value
            onMinimumSizeChanged()
        }
    var paddingTop: Float
        get() = _paddingTop
        set(value) {
            if (value == _paddingTop) return
            _paddingTop = value
            onMinimumSizeChanged()
        }
    var paddingBottom: Float
        get() = _paddingBottom
        set(value) {
            if (value == _paddingBottom) return
            _paddingBottom = value
            onMinimumSizeChanged()
        }

    /**
     * Sets the padding to all the sides to the same value.
     * @return the side with the largest padding
     */
    var padding
        get() = maxOf(_paddingLeft, _paddingRight, _paddingTop, _paddingBottom)
        set(value) {
            _paddingLeft = value
            _paddingRight = value
            _paddingTop = value
            _paddingBottom = value
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
        nodes.forEach {
            if (it is Control && it.enabled) {
                val w = width - paddingLeft - paddingRight
                val h = height - paddingTop - paddingBottom
                fitChild(it, paddingLeft, paddingBottom, w, h)
                it.computeMargins()
            }
        }
    }
}