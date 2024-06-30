package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Theme
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [VBoxContainer] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.vBoxContainer(
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return VBoxContainer().also(callback).addTo(this)
}

/**
 * Adds a [VBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.vBoxContainer(
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.vBoxContainer(callback)
}

/**
 * Adds a [VBoxContainer] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.column(
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return VBoxContainer().also(callback).addTo(this)
}

/**
 * Adds a [VBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.column(
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.column(callback)
}

/**
 * A vertical [BoxContainer] by adding [Control] from bottom up.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class VBoxContainer : BoxContainer() {

    class ThemeVars {
        val separation = BoxContainer.themeVars.separation
    }

    init {
        vertical = true
    }

    companion object {
        /** [Theme] related variable names when setting theme values for a [VBoxContainer] */
        val themeVars = ThemeVars()
    }
}
