package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.AlignMode
import com.littlekt.graph.node.resource.Theme
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal fun createVBoxContainer(separation: Int?, align: AlignMode?): VBoxContainer =
    VBoxContainer().also {
        if (separation != null) it.separation = separation
        if (align != null) it.align = align
    }

/**
 * Adds a [VBoxContainer] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.vBoxContainer(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {},
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createVBoxContainer(separation, align).addTo(this, modifier, callback)
}

/**
 * Adds a [VBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.vBoxContainer(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {},
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.vBoxContainer(separation, align, modifier = modifier, callback = callback)
}

/**
 * Adds a [VBoxContainer] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.column(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {},
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createVBoxContainer(separation, align).addTo(this, modifier, callback)
}

/**
 * Adds a [VBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to
 *   initialize any values
 * @return the newly created [VBoxContainer]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.column(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {},
): VBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.column(separation, align, modifier = modifier, callback = callback)
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
