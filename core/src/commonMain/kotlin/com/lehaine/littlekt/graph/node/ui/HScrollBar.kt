package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.node.component.Orientation

/**
 * @author Colton Daily
 * @date 10/17/2022
 */
open class HScrollBar : ScrollBar(orientation = Orientation.HORIZONTAL) {

    init {
        verticalSizeFlags = SizeFlag.NONE
    }
}