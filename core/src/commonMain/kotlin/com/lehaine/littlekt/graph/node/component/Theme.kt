package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.font.BitmapFont

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
class Theme(
    /**
     * A map of drawables mapped by Node type, mapped by variable name and value.
     *
     * Eg: `drawables["Button"]["pressed"]`
     */
    val drawables: Map<String, Map<String, Drawable>> = mapOf(),

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg: `fonts["Button"]["font"]`
     */
    val fonts: Map<String, Map<String, BitmapFont>> = mapOf(),

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg: `colors["Button"]["fontColor"]`
     */
    val colors: Map<String, Map<String, Color>> = mapOf(),

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * constants["Button"]["MyVar"]
     */
    val constants: Map<String, Map<String, Int>> = mapOf()
) {


    companion object {
        const val DEFAULT_BASE_SCALE = 1f
        const val DEFAULT_FONT_SIZE = 16

        var defaultTheme = Theme(
            drawables = mapOf("Button" to mapOf("font", ),
            colors = mapOf("Button" to mapOf("fontColor" to Color.WHITE))
        )
    }
}