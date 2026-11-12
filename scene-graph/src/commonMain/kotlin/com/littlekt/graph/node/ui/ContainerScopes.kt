package com.littlekt.graph.node.ui

import com.littlekt.graph.node.ui.Control.SizeFlag
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.VAlign

/**
 * The [Modifier]s only available to the children of a row, such as an [HBoxContainer].
 *
 * ```
 * row {
 *     label("Name", modifier = Modifier.align(VAlign.CENTER))
 *     lineEdit(modifier = Modifier.weight(2f))
 *     button("Save", modifier = Modifier.weight(1f))
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface RowScope {
    /**
     * Shares the free width of the row with the other weighted children.
     *
     * @param weight the share of the free width compared to the other weighted children
     * @param fill `true` to fill the shared width or `false` to keep the minimum width in it
     */
    fun Modifier.weight(weight: Float = 1f, fill: Boolean = true): Modifier =
        then(
            Modifier.Element {
                it.horizontalSizing =
                    if (fill) SizeFlag.FILL or SizeFlag.EXPAND else SizeFlag.EXPAND
                it.stretchRatio = weight
            }
        )

    /** Keeps the child at its minimum height and aligns it vertically in the row. */
    fun Modifier.align(alignment: VAlign): Modifier =
        then(Modifier.Element { it.verticalSizing = alignment.toSizeFlag() })

    /** Stretches the child to the full height of the row. This is the default. */
    fun Modifier.fillHeight(): Modifier =
        then(Modifier.Element { it.verticalSizing = SizeFlag.FILL })
}

/**
 * The [Modifier]s only available to the children of a stack, such as a [StackContainer],
 * [PanelContainer] or [PaddedContainer]. Children fill the stack by default.
 *
 * ```
 * stack {
 *     textureRect { slice = background }
 *     label("Paused", modifier = Modifier.align(HAlign.CENTER, VAlign.CENTER))
 *     button("Resume", modifier = Modifier.align(HAlign.RIGHT, VAlign.BOTTOM))
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface StackScope {
    /** Keeps the child at its minimum size and aligns it on both axes in the stack. */
    fun Modifier.align(horizontal: HAlign, vertical: VAlign): Modifier =
        align(horizontal).align(vertical)

    /** Keeps the child at its minimum width and aligns it horizontally in the stack. */
    fun Modifier.align(alignment: HAlign): Modifier =
        then(Modifier.Element { it.horizontalSizing = alignment.toSizeFlag() })

    /** Keeps the child at its minimum height and aligns it vertically in the stack. */
    fun Modifier.align(alignment: VAlign): Modifier =
        then(Modifier.Element { it.verticalSizing = alignment.toSizeFlag() })

    /** Stretches the child to the full width of the stack. This is the default. */
    fun Modifier.fillWidth(): Modifier =
        then(Modifier.Element { it.horizontalSizing = SizeFlag.FILL })

    /** Stretches the child to the full height of the stack. This is the default. */
    fun Modifier.fillHeight(): Modifier =
        then(Modifier.Element { it.verticalSizing = SizeFlag.FILL })
}

/**
 * The [Modifier]s only available to the children of a [ScrollContainer]. Children keep their
 * minimum size by default and scroll when it is larger than the visible area.
 *
 * ```
 * scrollContainer {
 *     column(modifier = Modifier.fillViewportWidth()) {
 *         repeat(20) { button("Item $it") }
 *     }
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface ScrollScope {
    /** Stretches the child to at least the visible width so only its height scrolls. */
    fun Modifier.fillViewportWidth(): Modifier =
        then(Modifier.Element { it.horizontalSizing = SizeFlag.FILL or SizeFlag.EXPAND })

    /** Stretches the child to at least the visible height so only its width scrolls. */
    fun Modifier.fillViewportHeight(): Modifier =
        then(Modifier.Element { it.verticalSizing = SizeFlag.FILL or SizeFlag.EXPAND })

    /** Stretches the child to at least the visible area on both axes. */
    fun Modifier.fillViewport(): Modifier = fillViewportWidth().fillViewportHeight()
}

private fun HAlign.toSizeFlag(): SizeFlag =
    when (this) {
        HAlign.LEFT -> SizeFlag.SHRINK_BEGIN
        HAlign.CENTER -> SizeFlag.SHRINK_CENTER
        HAlign.RIGHT -> SizeFlag.SHRINK_END
    }

private fun VAlign.toSizeFlag(): SizeFlag =
    when (this) {
        VAlign.TOP -> SizeFlag.SHRINK_END
        VAlign.CENTER -> SizeFlag.SHRINK_CENTER
        VAlign.BOTTOM -> SizeFlag.SHRINK_BEGIN
    }

/**
 * The [Modifier]s only available to the children of a column, such as a [VBoxContainer].
 *
 * ```
 * column {
 *     label("Title", modifier = Modifier.align(HAlign.CENTER))
 *     scrollContainer(modifier = Modifier.weight())
 *     button("Close", modifier = Modifier.align(HAlign.RIGHT))
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface ColumnScope {
    /**
     * Shares the free height of the column with the other weighted children.
     *
     * @param weight the share of the free height compared to the other weighted children
     * @param fill `true` to fill the shared height or `false` to keep the minimum height in it
     */
    fun Modifier.weight(weight: Float = 1f, fill: Boolean = true): Modifier =
        then(
            Modifier.Element {
                it.verticalSizing =
                    if (fill) SizeFlag.FILL or SizeFlag.EXPAND else SizeFlag.EXPAND
                it.stretchRatio = weight
            }
        )

    /** Keeps the child at its minimum width and aligns it horizontally in the column. */
    fun Modifier.align(alignment: HAlign): Modifier =
        then(Modifier.Element { it.horizontalSizing = alignment.toSizeFlag() })

    /** Stretches the child to the full width of the column. This is the default. */
    fun Modifier.fillWidth(): Modifier =
        then(Modifier.Element { it.horizontalSizing = SizeFlag.FILL })
}
