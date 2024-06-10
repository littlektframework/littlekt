package com.littlekt.graphics.g2d.shape

/**
 * The type of miter joint used for connecting.
 *
 * @author Colton Daily
 * @date 7/18/2022
 */
enum class JoinType {
    /**
     * No mitering is performed. This defaults to [ShapeRenderer.line] and is the fastest options.
     */
    NONE,

    /** A standard miter joint. */
    POINTY,

    /** A truncated miter joint. */
    SMOOTH
}
