package com.lehaine.littlekt.graph.node.ui

import kotlin.math.max

/**
 * @author Colton Daily
 * @date 10/16/2022
 */
class ScrollContainer : Container() {
    var horizontalScrollMode: ScrollMode = ScrollMode.AUTO
    var verticalScrollMode: ScrollMode = ScrollMode.AUTO

    override fun calculateMinSize() {

        var largestChildWidth = 0f
        var largestChildHeight = 0f

        nodes.forEach {
            if (it !is Control) return@forEach
            if (!it.visible) return

            largestChildWidth = max(it.combinedMinWidth, largestChildWidth)
            largestChildHeight = max(it.combinedMinHeight, largestChildHeight)
        }

        if (horizontalScrollMode == ScrollMode.DISABLED) {
            _internalMinWidth = max(_internalMinWidth, largestChildWidth)
        }

        if (verticalScrollMode == ScrollMode.DISABLED) {
            _internalMinHeight = max(_internalMinHeight, largestChildHeight)
        }

        val showHorizontalScroll =
            horizontalScrollMode == ScrollMode.ALWAYS || (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > _internalMinWidth)
        val showVerticalScroll = verticalScrollMode == ScrollMode.ALWAYS || (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > _internalMinHeight)

        minSizeInvalid = false
    }

    enum class ScrollMode {
        DISABLED,
        AUTO,
        ALWAYS,
        NEVER
    }
}