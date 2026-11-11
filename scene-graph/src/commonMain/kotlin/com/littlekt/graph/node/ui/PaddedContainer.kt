package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Theme
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal fun createPaddedContainer(padding: Int?): PaddedContainer =
    PaddedContainer().also {
        if (padding != null) it.padding(padding)
    }

/**
 * Adds a [PaddedContainer] to the current [Node] as a child and then triggers the [callback].
 *
 * @param padding the padding of every side
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.paddedContainer(
    padding: Int? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {},
): PaddedContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createPaddedContainer(padding).addTo(this, modifier, callback)
}

/**
 * Adds a [PaddedContainer] with [value] padding on every side to the current [Node] as a child
 * and then triggers the [callback].
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.padded(
    value: Int,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {},
): PaddedContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return paddedContainer(value, modifier, callback)
}

/**
 * Adds a [PaddedContainer] with [horizontal] & [vertical] padding to the current [Node] as a
 * child and then triggers the [callback].
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.padded(
    horizontal: Int,
    vertical: Int,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {},
): PaddedContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return PaddedContainer()
        .also { it.padding(horizontal, vertical) }
        .addTo(this, modifier, callback)
}

/**
 * Adds a [PaddedContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param padding the padding of every side
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.paddedContainer(
    padding: Int? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker PaddedContainer.() -> Unit = {},
): PaddedContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.paddedContainer(padding, modifier = modifier, callback = callback)
}

/**
 * A [Container] with padding.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class PaddedContainer : Container() {

    class ThemeVars {
        val paddingLeft = "paddingLeft"
        val paddingRight = "paddingRight"
        val paddingTop = "paddingTop"
        val paddingBottom = "paddingBottom"
    }

    companion object {
        /** [Theme] related variable names when setting theme values for a [PaddedContainer] */
        val themeVars = ThemeVars()
    }

    var paddingLeft: Int
        get() = getThemeConstant(themeVars.paddingLeft)
        set(value) {
            if (constantOverrides[themeVars.paddingLeft] == value) return
            constantOverrides[themeVars.paddingLeft] = value
            onMinimumSizeChanged()
        }

    var paddingRight: Int
        get() = getThemeConstant(themeVars.paddingRight)
        set(value) {
            if (constantOverrides[themeVars.paddingRight] == value) return
            constantOverrides[themeVars.paddingRight] = value
            onMinimumSizeChanged()
        }

    var paddingTop: Int
        get() = getThemeConstant(themeVars.paddingTop)
        set(value) {
            if (constantOverrides[themeVars.paddingTop] == value) return
            constantOverrides[themeVars.paddingTop] = value
            onMinimumSizeChanged()
        }

    var paddingBottom: Int
        get() = getThemeConstant(themeVars.paddingBottom)
        set(value) {
            if (constantOverrides[themeVars.paddingBottom] == value) return
            constantOverrides[themeVars.paddingBottom] = value
            onMinimumSizeChanged()
        }

    /** Sets the padding of every side to [value]. */
    fun padding(value: Int) = padding(value, value, value, value)

    /** Sets the left & right padding to [horizontal] and the top & bottom padding to [vertical]. */
    fun padding(horizontal: Int, vertical: Int) =
        padding(horizontal, vertical, horizontal, vertical)

    /** Sets the padding of each side. */
    fun padding(left: Int, top: Int, right: Int, bottom: Int) {
        constantOverrides[themeVars.paddingLeft] = left
        constantOverrides[themeVars.paddingTop] = top
        constantOverrides[themeVars.paddingRight] = right
        constantOverrides[themeVars.paddingBottom] = bottom
        onMinimumSizeChanged()
    }

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

        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom

        _internalMinWidth = maxWidth
        _internalMinHeight = maxHeight
        minSizeInvalid = false
    }

    override fun onSortChildren() {
        val width = width
        val height = height
        nodes.forEach {
            if (it is Control && it.enabled && it.visible && !it.isDestroyed) {
                val w = width - paddingLeft - paddingRight
                val h = height - paddingTop - paddingBottom
                fitChild(
                    it,
                    paddingLeft.toFloat() - originX,
                    paddingBottom.toFloat() - originY,
                    w,
                    h,
                )
                it.computeMargins()
            }
        }
    }
}
