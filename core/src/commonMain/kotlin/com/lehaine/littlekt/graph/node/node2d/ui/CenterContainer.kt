package com.lehaine.littlekt.graph.node.node2d.ui


import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import kotlin.math.floor
import kotlin.math.max

/**
 * Adds a [CenterContainer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CenterContainer] context in order to initialize any values
 * @return the newly created [CenterContainer]
 */
inline fun Node.centerContainer(callback: @SceneGraphDslMarker CenterContainer.() -> Unit = {}) =
    CenterContainer().also(callback).addTo(this)

/**
 * Adds a [CenterContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CenterContainer] context in order to initialize any values
 * @return the newly created [CenterContainer]
 */
inline fun SceneGraph.centerContainer(callback: @SceneGraphDslMarker CenterContainer.() -> Unit = {}) =
    root.centerContainer(callback)

/**
 * A [Container] that centers any children within it.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class CenterContainer : Container() {
    override fun onSortChildren() {
        nodes.forEach {
            if (it is Control && it.enabled) {
                val newX = floor((width - it.combinedMinWidth) * 0.5f)
                val newY = floor((height - it.combinedMinHeight) * 0.5f)
                fitChild(it, newX, newY, it.combinedMinWidth, it.combinedMinHeight)
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinHeight = 0f
        _internalMinWidth = 0f
        nodes.forEach {
            if (it is Control) {
                _internalMinWidth = max(_internalMinWidth, it.combinedMinWidth)
                _internalMinHeight = max(_internalMinHeight, it.combinedMinHeight)
            }
        }
        minSizeInvalid = false
    }
}