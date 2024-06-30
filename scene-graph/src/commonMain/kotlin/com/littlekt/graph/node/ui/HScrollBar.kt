package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Orientation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Adds a [HScrollBar] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.hScrollBar(callback: @SceneGraphDslMarker HScrollBar.() -> Unit = {}): HScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return HScrollBar().also(callback).addTo(this)
}

/**
 * Adds a [HScrollBar] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.hScrollBar(
    callback: @SceneGraphDslMarker HScrollBar.() -> Unit = {}
): HScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.hScrollBar(callback)
}

/**
 * A horizontal [ScrollBar] which goes left [min] to right [max].
 *
 * @author Colton Daily
 * @date 10/17/2022
 */
open class HScrollBar : ScrollBar(orientation = Orientation.HORIZONTAL) {

    init {
        verticalSizing = SizeFlag.NONE
    }
}
