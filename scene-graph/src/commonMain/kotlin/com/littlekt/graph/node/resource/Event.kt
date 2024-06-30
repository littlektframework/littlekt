package com.littlekt.graph.node.resource

/**
 * A event class that tracks when it has been captured, handled, cancelled, and finished.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Event {
    /** @return true means event occurred during the capture phase */
    var capture = false

    /** @return true means propagate to target's parents */
    var bubbles = true

    /** @return true the event was handled (the scene will eat the input) */
    var handled = false

    /** @return true event propagation was stopped */
    var stopped = false

    /**
     * @return true propagation was stopped and any action that this event would cause should not
     *   happen
     */
    var cancelled = false

    /** Mark this event has handled. */
    fun handle() {
        handled = true
    }

    /** Cancels this event and marks it as handled. */
    fun cancel() {
        cancelled = true
        stopped = true
        handled = true
    }

    /** Stop this event. */
    fun stop() {
        stopped = true
    }

    /** Reset this event back to its default state. */
    open fun reset() {
        capture = false
        bubbles = true
        handled = false
        stopped = false
        cancelled = false
    }
}
