package com.lehaine.littlekt.graphics.state

import com.lehaine.littlekt.GL

/**
 * Defines a blend mode.
 * @author Colton Daily
 * @date 11/7/2021
 */
enum class Blend(val glFlag: Int) {
    /**
     * Each component of the color is multiplied by {1, 1, 1, 1}.
     */
    ONE(GL.ONE),

    /**
     * Each component of the color is multiplied by {0, 0, 0, 0}.
     */
    ZERO(GL.ZERO),

    /**
     * Each component of the color is multiplied by the source color.
     * {Rs, Gs, Bs, As}, where Rs, Gs, Bs, As are color source values.
     */
    SOURCE_COLOR(GL.SRC_COLOR),

    /**
     * Each component of the color is multiplied by the inverse of the source color.
     * {1 - Rs, 1 - Gs, 1 - Bs, 1 - As}, where Rs, Gs, Bs, As are color source values.
     */
    INVERSE_SOURCE_COLOR(GL.ONE_MINUS_SRC_COLOR),

    /**
     * Each component of the color is multiplied by the alpha value of the source.
     */
    SOURCE_ALPHA(GL.SRC_ALPHA),

    /**
     * Each component of the color is multiplied by the inverse of the alpha value of the source.
     * {1 - As, 1 - As, 1 - As, 1 - As}, where As is the source alpha value.
     */
    INVERSE_SOURCE_ALPHA(GL.ONE_MINUS_SRC_ALPHA),

    /**
     * Each component color is multiplied by the destination color.
     * {Rd, Gd, Bd, Ad}, where Rd, Gd, Bd, Ad are color destination values.
     */
    DESTINATION_COLOR(GL.DST_COLOR),

    /**
     * Each component of the color is multiplied by the inversed destination color.
     * {1 - Rd, 1 - Gd, 1 - Bd, 1 - Ad}, where Rd, Gd, Bd, Ad are color destination values.
     */
    INVERSE_DESTINATION_COLOR(GL.ONE_MINUS_DST_COLOR),

    /**
     * Each component of the color is multiplied by the alpha value of the destination.
     * {Ad, Ad, Ad, Ad}, where Ad is the destination alpha value.
     */
    DESTINATION_ALPHA(GL.DST_ALPHA),

    /**
     * Each component of the color is multiplied by the inversed alpha value of the destination.
     * {1 - Ad, 1 - Ad, 1 - Ad, 1 - Ad}, where Ad is the destination alpha value.
     */
    INVERSE_DESTINATION_ALPHA(GL.ONE_MINUS_DST_ALPHA),

    /**
     * Each component of the color is multiplied by a constant.
     */
    BLEND_FACTOR(GL.CONSTANT_COLOR),

    /**
     * Each component of the color is multiplied by a inversed constant.
     */
    INVERSE_BLEND_FACTOR(GL.ONE_MINUS_CONSTANT_COLOR),

    /**
     * Each component of the color is multiplied by either the alpha of the source color, or the inverse of the alpha of the source color, whichever is greater.
     * {f, f, f, 1}, where f = min(As, 1 - As), where As is the source alpha value.
     */
    SOURCE_ALPHA_SATURATION(GL.SRC_ALPHA_SATURATE)
}