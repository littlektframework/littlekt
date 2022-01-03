package com.lehaine.littlekt.graph.node.component

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Event {
    /**
     * @return true means event occurred during the capture phase
     */
    var capture = false

    /**
     * @return true means propagate to target's parents
     */
    var bubbles = true

    /**
     * @return true the event was handled (the scene will eat the input)
     */
    var handled = false

    /**
     * @return true event propagation was stopped
     */
    var stopped = false

    /**
     * @return true propagation was stopped and any action that this event would cause should not happen
     */
    var cancelled = false

    fun handle() {
        handled = true
    }

    fun cancel() {
        cancelled = true
        stopped = true
        handled = true
    }

    fun stop() {
        stopped = true
    }

    open fun reset() {
        capture = false
        bubbles = true
        handled = false
        stopped = false
        cancelled = false
    }
}