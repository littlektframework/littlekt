package com.lehaine.littlekt.graph.node.ui

/**
 * @author Colton Daily
 * @date 10/16/2022
 */
class ScrollBar: Range() {

    enum class HighlightStatus {
        NONE,
        DECREMENT,
        RANGE,
        INCREMENT
    }
}