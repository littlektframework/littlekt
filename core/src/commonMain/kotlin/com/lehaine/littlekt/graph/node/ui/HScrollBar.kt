package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Orientation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [HScrollBar] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.hScrollBar(callback: @SceneGraphDslMarker HScrollBar.() -> Unit = {}): HScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return HScrollBar().also(callback).addTo(this)
}

/**
 * Adds a [HScrollBar] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.hScrollBar(callback: @SceneGraphDslMarker HScrollBar.() -> Unit = {}): HScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.hScrollBar(callback)
}

/**
 * @author Colton Daily
 * @date 10/17/2022
 */
open class HScrollBar : ScrollBar(orientation = Orientation.HORIZONTAL) {

    init {
        verticalSizeFlags = SizeFlag.NONE
    }
}