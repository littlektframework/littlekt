package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.resource.Orientation
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Adds a [VScrollBar] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.vScrollBar(callback: @SceneGraphDslMarker VScrollBar.() -> Unit = {}): VScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return VScrollBar().also(callback).addTo(this)
}

/**
 * Adds a [VScrollBar] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.vScrollBar(callback: @SceneGraphDslMarker VScrollBar.() -> Unit = {}): VScrollBar {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.vScrollBar(callback)
}


/**
 * A vertical [ScrollBar] which goes top [min] to bottom [max].
 * @author Colton Daily
 * @date 10/17/2022
 */
open class VScrollBar : ScrollBar(orientation = Orientation.VERTICAL) {

    init {
        horizontalSizeFlags = SizeFlag.NONE
    }
}