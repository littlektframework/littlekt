package com.littlekt

import com.littlekt.graphics.Color

/** @return the [color] as linear when the surface is sRGB, so it shows up as the given [color]. */
fun Graphics.surfaceColor(color: Color): Color =
    if (preferredFormat.srgb) color.toLinear() else color
