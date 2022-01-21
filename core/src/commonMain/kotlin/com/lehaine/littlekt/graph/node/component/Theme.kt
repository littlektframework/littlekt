package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graph.node.node2d.ui.Button
import com.lehaine.littlekt.graph.node.node2d.ui.Label
import com.lehaine.littlekt.graph.node.node2d.ui.Panel
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.NinePatch
import com.lehaine.littlekt.graphics.Textures
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
     * constants["Button"]["myVar"]
     */
    val constants: Map<String, Map<String, Int>> = mapOf(),
    val defaultFont: BitmapFont? = null,
) {

    companion object {
        val FALLBACK_DRAWABLE = TextureSliceDrawable(Textures.white)
        val FALLBACK_FONT = Fonts.default


        private var _theme: Theme? = null
        var defaultTheme: Theme
            get() {
                if (_theme == null) {
                    _theme = createDefaultTheme()
                }
                return _theme!!
            }
            set(value) {
                _theme = value
            }
    }
}

fun createDefaultTheme(): Theme {
    val greyButtonNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_button").slice,
        5,
        5,
        5,
        4
    )
    val panelNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_panel").slice,
        6,
        6,
        6,
        6
    )
    return Theme(
        drawables = mapOf(
            "Button" to mapOf(
                Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                    .apply { modulate = Color.LIGHT_BLUE },
                Button.themeVars.pressed to NinePatchDrawable(greyButtonNinePatch)
                    .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.scaleRgb(0.6f) } },
                Button.themeVars.hover to NinePatchDrawable(greyButtonNinePatch)
                    .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.2f) } },
                Button.themeVars.disabled to NinePatchDrawable(greyButtonNinePatch)
                    .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.5f) } },
            ),
            "Panel" to mapOf(
                Panel.themeVars.panel to NinePatchDrawable(panelNinePatch).apply {
                    modulate = Color.LIGHT_BLUE
                }
            )
        ),
        colors = mapOf(
            "Button" to mapOf(Button.themeVars.fontColor to Color.WHITE),
            "Label" to mapOf(Label.themeVars.fontColor to Color.WHITE)
        )
    )
}