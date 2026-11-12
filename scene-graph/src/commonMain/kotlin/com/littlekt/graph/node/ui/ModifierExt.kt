package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.TextureFont

/** Anchors the control to fill its parent. See [Control.fillParent]. */
fun Modifier.fillParent(margin: Float = 0f) = then(Modifier.Element { it.fillParent(margin) })

/** Anchors the control using a preset. See [Control.anchor]. */
fun Modifier.anchor(layout: Control.AnchorLayout, margin: Float = 0f) =
    then(Modifier.Element { it.anchor(layout, margin) })

/** Anchors the control to the center of its parent. See [Control.centerInParent]. */
fun Modifier.centerInParent() = then(Modifier.Element { it.centerInParent() })

/** Sets all four anchors of the control. See [Control.anchors]. */
fun Modifier.anchors(left: Float, top: Float, right: Float, bottom: Float) =
    then(Modifier.Element { it.anchors(left, top, right, bottom) })

/** Expands the control on both axes in its parent [Container]. See [Control.expand]. */
fun Modifier.expand(ratio: Float = 1f) = then(Modifier.Element { it.expand(ratio) })

/** Expands the control horizontally in its parent [Container]. See [Control.expandWidth]. */
fun Modifier.expandWidth(ratio: Float = 1f) = then(Modifier.Element { it.expandWidth(ratio) })

/** Expands the control vertically in its parent [Container]. See [Control.expandHeight]. */
fun Modifier.expandHeight(ratio: Float = 1f) = then(Modifier.Element { it.expandHeight(ratio) })

/** Keeps the control at its minimum size at the start. See [Control.shrinkBegin]. */
fun Modifier.shrinkBegin(horizontal: Boolean = true, vertical: Boolean = true) =
    then(Modifier.Element { it.shrinkBegin(horizontal, vertical) })

/** Keeps the control at its minimum size in the center. See [Control.shrinkCenter]. */
fun Modifier.shrinkCenter(horizontal: Boolean = true, vertical: Boolean = true) =
    then(Modifier.Element { it.shrinkCenter(horizontal, vertical) })

/** Keeps the control at its minimum size at the end. See [Control.shrinkEnd]. */
fun Modifier.shrinkEnd(horizontal: Boolean = true, vertical: Boolean = true) =
    then(Modifier.Element { it.shrinkEnd(horizontal, vertical) })

/** Sets the minimum size of the control. See [Control.minSize]. */
fun Modifier.minSize(width: Float, height: Float) =
    then(Modifier.Element { it.minSize(width, height) })

/** Sets the size of the control. See [Control.size]. */
fun Modifier.size(width: Float, height: Float) = then(Modifier.Element { it.size(width, height) })

/** Wraps the control in a [PaddedContainer] with [value] padding on every side. */
fun Modifier.padding(value: Int) = padding(value, value, value, value)

/** Wraps the control in a [PaddedContainer] with [horizontal] & [vertical] padding. */
fun Modifier.padding(horizontal: Int, vertical: Int) =
    padding(horizontal, vertical, horizontal, vertical)

/** Wraps the control in a [PaddedContainer] with padding on each side. */
fun Modifier.padding(left: Int, top: Int, right: Int, bottom: Int) =
    then(
        Modifier.Wrapper {
            PaddedContainer().apply {
                padding(left, top, right, bottom)
                addChild(it)
            }
        }
    )

/** Wraps the control in a [PanelContainer] that draws the theme's panel behind it. */
fun Modifier.panel() = then(Modifier.Wrapper { PanelContainer().apply { addChild(it) } })

/** Wraps the control in a [PanelContainer] that draws [drawable] behind it. */
fun Modifier.background(drawable: Drawable) =
    then(
        Modifier.Wrapper {
            PanelContainer().apply {
                panel = drawable
                addChild(it)
            }
        }
    )

/** Overrides the `fontColor` theme color of the control, such as for a [Label] or [Button]. */
fun Modifier.fontColor(color: Color) = color("fontColor", color)

/** Overrides a theme color of the control. */
fun Modifier.color(name: String, color: Color) =
    then(Modifier.Element { it.colorOverrides[name] = color })

/** Overrides a theme drawable of the control. */
fun Modifier.drawable(name: String, drawable: Drawable) =
    then(Modifier.Element { it.drawableOverrides[name] = drawable })

/** Overrides a theme constant of the control. */
fun Modifier.constant(name: String, value: Int) =
    then(Modifier.Element { it.constantOverrides[name] = value })

/** Overrides a theme font of the control. */
fun Modifier.font(name: String, font: TextureFont) =
    then(Modifier.Element { it.fontOverrides[name] = font })

/** Sets the [Theme] of the control and its children. */
fun Modifier.theme(theme: Theme) = then(Modifier.Element { it.theme = theme })

/** Sets the tint of the control. See [Control.color]. */
fun Modifier.tint(color: Color) = then(Modifier.Element { it.color = color })

/** Shows or hides the control. */
fun Modifier.visible(visible: Boolean) = then(Modifier.Element { it.visible = visible })

/** Enables or disables the control. */
fun Modifier.enabled(enabled: Boolean) = then(Modifier.Element { it.enabled = enabled })

/** Sets how the control handles mouse input. See [Control.mouseFilter]. */
fun Modifier.mouseFilter(filter: Control.MouseFilter) =
    then(Modifier.Element { it.mouseFilter = filter })
