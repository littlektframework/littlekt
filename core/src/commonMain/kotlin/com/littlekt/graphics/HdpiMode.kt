package com.littlekt.graphics

import com.littlekt.Graphics

/**
 * @author Colton Daily
 * @date 2/7/2023
 */
enum class HdpiMode {
    /**
     * Mouse coordinates, [Graphics.width] and [Graphics.height] will return as logical coordinates
     * according to the defined HDPI scaling. The window is sized by the monitor's content scale on
     * every platform, so a window is the same physical size at 100% or 150%. The back buffer is
     * still drawn at full resolution.
     */
    LOGICAL,

    /**
     * Mouse coordinates, [Graphics.width] and [Graphics.height] will return as raw pixel
     * coordinates, ignoring any defined HDPI scaling. The window size is in pixels.
     */
    PIXELS
}
