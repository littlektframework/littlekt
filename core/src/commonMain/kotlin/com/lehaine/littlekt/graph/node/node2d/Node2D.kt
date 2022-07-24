package com.lehaine.littlekt.graph.node.node2d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.CanvasItem
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [Node2D] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Node2D] context in order to initialize any values
 * @return the newly created [Node2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.node2d(callback: @SceneGraphDslMarker Node2D.() -> Unit = {}): Node2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Node2D().also(callback).addTo(this)
}

/**
 * Adds a [Node2D] to the current [SceneGraph.root] as a child and then triggers the [Node2D]
 * @param callback the callback that is invoked with a [Node2D] context in order to initialize any values
 * @return the newly created [Node2D]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.node2d(callback: @SceneGraphDslMarker Node2D.() -> Unit = {}): Node2D {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.node2d(callback)
}

/**
 * A [Node] with extended 2D transformations.
 * @author Colton Daily
 * @date 3/17/2022
 */
open class Node2D : CanvasItem() {
    var ySort: Boolean = false
        set(value) {
            field = value
            nodes.sort = if (field) SORT_BY_Y else null
        }

    companion object {
        private val SORT_BY_Y: Comparator<Node> = Comparator { a, b ->
            if (a is CanvasItem && b is CanvasItem) {
                return@Comparator a.globalY.compareTo(b.globalY)
            }
            if (a is CanvasItem) {
                return@Comparator 1
            }
            if (b is CanvasItem) {
                return@Comparator -1
            }
            return@Comparator 0
        }

    }
}