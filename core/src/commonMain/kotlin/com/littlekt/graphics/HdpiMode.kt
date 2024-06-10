package com.littlekt.graphics

import com.littlekt.Graphics

/**
 * @author Colton Daily
 * @date 2/7/2023
 */
enum class HdpiMode {
    /**
     * Mouse coordinates, [Graphics.width] and [Graphics.height] will return as logical coordinates
     * according to the defined HDPI scaling.
     */
    LOGICAL,

    /**
     * Mouse coordinates, [Graphics.width] and [Graphics.height] will return as raw pixel
     * coordinates, ignoring any defined HDPI scaling.
     */
    PIXELS
}
