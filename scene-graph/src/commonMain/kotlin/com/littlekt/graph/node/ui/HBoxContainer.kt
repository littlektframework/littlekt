package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Theme
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Adds a [HBoxContainer] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.hBoxContainer(
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return HBoxContainer().also(callback).addTo(this)
}

/**
 * Adds a [HBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.hBoxContainer(
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.hBoxContainer(callback)
}

/** Adds a [HBoxContainer] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.row(callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return HBoxContainer().also(callback).addTo(this)
}

/**
 * Adds a [HBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.row(
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.row(callback)
}

/**
 * A vertical [BoxContainer] by adding [Control] from left to right.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class HBoxContainer : BoxContainer() {

    class ThemeVars {
        val separation = BoxContainer.themeVars.separation
    }

    init {
        vertical = false
    }

    companion object {
        /** [Theme] related variable names when setting theme values for a [HBoxContainer] */
        val themeVars = ThemeVars()
    }
}
