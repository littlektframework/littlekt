package com.lehaine.littlekt.graph.node.component.ui

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.font.Font

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
class Theme {
    private val _styles = mutableMapOf<String, MutableMap<String, StyleBox>>()

    /**
     * A map of styles mapped by Node type, mapped by variable name and value.
     *
     * Eg: `styles["Button"]["pressed"]`
     */
    val styles: Map<String, Map<String, StyleBox>> get() = _styles

    private val _fonts = mutableMapOf<String, MutableMap<String, Font>>()

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg: `fonts["Button"]["font"]`
     */
    val fonts: Map<String, Map<String, Font>> get() = _fonts

    private val _fontSizes = mutableMapOf<String, MutableMap<String, Int>>()

    /**
     * A map of font sizes mapped by Node type, mapped by variable name and value.
     *
     * `fontSizes["Button"]["var"]`
     */
    val fontSizes: Map<String, Map<String, Int>> get() = _fontSizes

    private val _colors = mutableMapOf<String, MutableMap<String, Color>>()

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg: `colors["Button"]["fontColor"]`
     */
    val colors: Map<String, Map<String, Color>> get() = _colors

    private val _constants = mutableMapOf<String, MutableMap<String, Int>>()

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * constants["Button"]["MyVar"]
     */
    val constants: Map<String, Map<String, Int>> get() = _constants


    companion object {
        const val DEFAULT_BASE_SCALE = 1f
        const val DEFAULT_FONT_SIZE = 16

        var defaultTheme = Theme()
    }
}