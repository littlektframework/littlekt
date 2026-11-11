package com.littlekt.graph.node.ui

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.ui.Control.AnchorLayout
import com.littlekt.graph.node.ui.Control.SizeFlag

/**
 * Adds an empty [Control] that expands to fill the free space of its parent [Container], pushing
 * its siblings apart.
 *
 * ```
 * row {
 *     label("Score")
 *     spacer()
 *     button("Pause")
 * }
 * ```
 */
fun Node.spacer(modifier: Modifier = Modifier): Control =
    Control()
        .also {
            it.mouseFilter = Control.MouseFilter.IGNORE
            it.expand()
        }
        .addTo(this, modifier) {}

/**
 * Anchors this [Control] to fill its parent.
 *
 * @param margin the distance to keep from each edge of the parent
 */
fun Control.fillParent(margin: Float = 0f) = anchor(AnchorLayout.FULL, margin)

/** Anchors this [Control] to the center of its parent. */
fun Control.centerInParent() = anchor(AnchorLayout.CENTER)

/** Sets all four anchors of this [Control]. */
fun Control.anchors(left: Float, top: Float, right: Float, bottom: Float) {
    anchorLeft = left
    anchorTop = top
    anchorRight = right
    anchorBottom = bottom
}

/**
 * Expands this [Control] to fill the free space of its parent [Container] on both axes.
 *
 * @param ratio the [Control.stretchRatio] used to share the free space with siblings
 */
fun Control.expand(ratio: Float = 1f) {
    expandWidth(ratio)
    expandHeight(ratio)
}

/**
 * Expands this [Control] to fill the free horizontal space of its parent [Container].
 *
 * @param ratio the [Control.stretchRatio] used to share the free space with siblings
 */
fun Control.expandWidth(ratio: Float = 1f) {
    horizontalSizing = SizeFlag.FILL or SizeFlag.EXPAND
    stretchRatio = ratio
}

/**
 * Expands this [Control] to fill the free vertical space of its parent [Container].
 *
 * @param ratio the [Control.stretchRatio] used to share the free space with siblings
 */
fun Control.expandHeight(ratio: Float = 1f) {
    verticalSizing = SizeFlag.FILL or SizeFlag.EXPAND
    stretchRatio = ratio
}

/** Keeps this [Control] at its minimum size, at the start of its space in a [Container]. */
fun Control.shrinkBegin(horizontal: Boolean = true, vertical: Boolean = true) =
    shrink(SizeFlag.SHRINK_BEGIN, horizontal, vertical)

/** Keeps this [Control] at its minimum size, centered in its space in a [Container]. */
fun Control.shrinkCenter(horizontal: Boolean = true, vertical: Boolean = true) =
    shrink(SizeFlag.SHRINK_CENTER, horizontal, vertical)

/** Keeps this [Control] at its minimum size, at the end of its space in a [Container]. */
fun Control.shrinkEnd(horizontal: Boolean = true, vertical: Boolean = true) =
    shrink(SizeFlag.SHRINK_END, horizontal, vertical)

private fun Control.shrink(flag: SizeFlag, horizontal: Boolean, vertical: Boolean) {
    if (horizontal) horizontalSizing = flag
    if (vertical) verticalSizing = flag
}

/** Sets the [Control.minWidth] and [Control.minHeight] of this [Control]. */
fun Control.minSize(width: Float, height: Float) {
    minWidth = width
    minHeight = height
}
