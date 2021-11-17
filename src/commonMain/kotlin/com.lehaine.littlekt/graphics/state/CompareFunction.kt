package com.lehaine.littlekt.graphics.state

import com.lehaine.littlekt.GL

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
enum class CompareFunction(val glFlag: Int) {

    /**
     * Always passes.
     */
    ALWAYS(GL.ALWAYS),

    /**
     * Never passes.
     */
    NEVER(GL.NEVER),

    /**
     * Passes the test when the new pixel value is less than current pixel value.
     */
    LESS(GL.LESS),

    /**
     * Passes the test when the new pixel value is less than or equal to current pixel value.
     */
    LESS_EQUAL(GL.LEQUAL),

    /**
     * Passes the test when the new pixel value is equal to current pixel value.
     */
    EQUAL(GL.EQUAL),

    /**
     * Passes the test when the new pixel value is greater than or equal to current pixel value.
     */
    GREATER_EQUAL(GL.GEQUAL),

    /**
     * Passes the test when the new pixel value is greater than current pixel value.
     */
    GREATER(GL.GREATER),

    /**
     * Passes the test when the new pixel value does not equal to current pixel value.
     */
    NOT_EQUAL(GL.NOTEQUAL)


}