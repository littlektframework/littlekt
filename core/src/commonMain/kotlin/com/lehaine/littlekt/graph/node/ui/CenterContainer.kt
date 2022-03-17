package com.lehaine.littlekt.graph.node.ui


import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.floor

/**
 * Adds a [CenterContainer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CenterContainer] context in order to initialize any values
 * @return the newly created [CenterContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.centerContainer(callback: @SceneGraphDslMarker CenterContainer.() -> Unit = {}): CenterContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return CenterContainer().also(callback).addTo(this)
}

/**
 * Adds a [CenterContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [CenterContainer] context in order to initialize any values
 * @return the newly created [CenterContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.centerContainer(callback: @SceneGraphDslMarker CenterContainer.() -> Unit = {}): CenterContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.centerContainer(callback)
}

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

        _internalMinWidth = maxWidth
        _internalMinHeight = maxHeight
        minSizeInvalid = false
    }
}