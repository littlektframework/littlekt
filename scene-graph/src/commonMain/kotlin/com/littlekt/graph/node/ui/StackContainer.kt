package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [StackContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.stackContainer(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker StackContainer.() -> Unit = {},
): StackContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return StackContainer().addTo(this, modifier, callback)
}

/**
 * Adds a [StackContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.stackContainer(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker StackContainer.() -> Unit = {},
): StackContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.stackContainer(modifier, callback)
}

/**
 * Adds a [StackContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.stack(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker StackContainer.() -> Unit = {},
): StackContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return StackContainer().addTo(this, modifier, callback)
}

/**
 * Adds a [StackContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.stack(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker StackContainer.() -> Unit = {},
): StackContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.stackContainer(modifier, callback)
}

/**
 * A [Container] that layers its children on top of each other without drawing anything itself.
 * Each child fills the container unless it is aligned with a [StackScope] modifier. Later children
 * are drawn on top.
 *
 * ```
 * stack {
 *     textureRect { slice = background }
 *     label("Paused", modifier = Modifier.align(HAlign.CENTER, VAlign.CENTER))
 *     button("Resume", modifier = Modifier.align(HAlign.RIGHT, VAlign.BOTTOM))
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
open class StackContainer : Container(), StackScope {

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

        _internalMinWidth = maxWidth
        _internalMinHeight = maxHeight
        minSizeInvalid = false
    }

    override fun onSortChildren() {
        val x = 0f - originX
        val y = 0f - originY
        nodes.forEach {
            if (it is Control && it.enabled && it.visible && !it.isDestroyed) {
                fitChild(it, x, y, width, height)
            }
        }
    }
}
