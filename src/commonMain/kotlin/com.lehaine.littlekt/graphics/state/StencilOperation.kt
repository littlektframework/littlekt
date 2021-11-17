package com.lehaine.littlekt.graphics.state

import com.lehaine.littlekt.GL

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
enum class StencilOperation(val glFlag: Int) {
    /**
     * Keeps the current value.
     */
    KEEP(GL.KEEP),

    /**
     * Sets the stencil buffer value to 0.
     */
    ZERO(GL.ZERO),

    /**
     * Sets the stencil buffer value to ref, as specified by glStencilFunc.
     */
    REPLACE(GL.REPLACE),

    /**
     * Increments the current stencil buffer value. Wraps stencil buffer value to zero when incrementing the maximum representable unsigned value.
     */
    INCREMENT(GL.INCR_WRAP),

    /**
     * Decrements the current stencil buffer value. Wraps stencil buffer value to the maximum representable unsigned value when decrementing a stencil buffer value of zero.
     */
    DECREMENT(GL.DECR_WRAP),

    /**
     * Increments the current stencil buffer value. Clamps to the maximum representable unsigned value.
     */
    INCREMENT_SATURATION(GL.INCR),

    /**
     * Decrements the current stencil buffer value. Clamps to 0.
     */
    DECREMENT_SATURATION(GL.DECR),

    /**
     * Bitwise inverts the current stencil buffer value.
     */
    INVERT(GL.INVERT)
}