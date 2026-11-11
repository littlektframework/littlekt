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
internal fun createHBoxContainer(separation: Int?, align: AlignMode?): HBoxContainer =
    HBoxContainer().also {
        if (separation != null) it.separation = separation
        if (align != null) it.align = align
    }

/**
 * Adds a [HBoxContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.hBoxContainer(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {},
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createHBoxContainer(separation, align).addTo(this, modifier, callback)
}

/**
 * Adds a [HBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.hBoxContainer(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {},
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.hBoxContainer(separation, align, modifier = modifier, callback = callback)
}

/**
 * Adds a [HBoxContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.row(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {},
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createHBoxContainer(separation, align).addTo(this, modifier, callback)
}

/**
 * Adds a [HBoxContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param separation the space between children
 * @param align the alignment of the children
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.row(
    separation: Int? = null,
    align: AlignMode? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {},
): HBoxContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.row(separation, align, modifier = modifier, callback = callback)
}

/**
 * A vertical [BoxContainer] by adding [Control] from left to right.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class HBoxContainer : BoxContainer(), RowScope {

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
