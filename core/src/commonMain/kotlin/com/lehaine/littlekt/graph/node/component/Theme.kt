package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graph.node.node2d.ui.*
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

/**
 * Creates a new [Theme] using the default theme values and allowing to add or override any additional theme values.
 * @return the newly created theme
 */
fun createDefaultTheme(
    /**
     * A map of drawables mapped by Node type, mapped by variable name and value.
     *
     * Eg: `drawables["Button"]["pressed"]`
     */
    extraDrawables: Map<String, Map<String, Drawable>> = mapOf(),

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg: `fonts["Button"]["font"]`
     */
    extraFonts: Map<String, Map<String, BitmapFont>> = mapOf(),

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg: `colors["Button"]["fontColor"]`
     */
    extraColors: Map<String, Map<String, Color>> = mapOf(),

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * constants["Button"]["myVar"]
     */
    extraConstants: Map<String, Map<String, Int>> = mapOf(),
    defaultFont: BitmapFont? = null,
): Theme {
    val greyButtonNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_button").slice,
        5,
        5,
        5,
        4
    )
    val greyOutlineNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_outline").slice,
        2,
        2,
        2,
        2
    )
    val panelNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_panel").slice,
        6,
        6,
        6,
        6
    )

    val greyBoxNinePatch = NinePatch(
        Textures.atlas.getByPrefix("grey_box").slice,
        7,
        7,
        6,
        6
    )
    val drawables = mapOf(
        "Button" to mapOf(
            Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = Color.LIGHT_BLUE },
            Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = Color.LIGHT_BLUE },
            Button.themeVars.pressed to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.scaleRgb(0.6f) } },
            Button.themeVars.hover to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.2f) } },
            Button.themeVars.disabled to NinePatchDrawable(greyButtonNinePatch)
                .apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.5f) } },
            Button.themeVars.focus to NinePatchDrawable(greyOutlineNinePatch)
                .apply { modulate = Color.WHITE },
        ),
        "Panel" to mapOf(
            Panel.themeVars.panel to NinePatchDrawable(panelNinePatch).apply {
                modulate = Color.LIGHT_BLUE
            }
        ),
        "ProgressBar" to mapOf(
            ProgressBar.themeVars.bg to NinePatchDrawable(panelNinePatch).apply {
                modulate = Color.DARK_BLUE
            },
            ProgressBar.themeVars.fg to NinePatchDrawable(panelNinePatch).apply {
                modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.5f) }
            }
        ),
        "LineEdit" to mapOf(
            LineEdit.themeVars.bg to NinePatchDrawable(greyBoxNinePatch).apply {
                minWidth = 50f
                minHeight = 25f
                modulate = Color.DARK_BLUE
            },
            LineEdit.themeVars.caret to TextureSliceDrawable(Textures.white).apply {
                minWidth = 1f
            },
            LineEdit.themeVars.selection to TextureSliceDrawable(Textures.white).apply {
                modulate = Color.LIGHT_BLUE
            }
        )
    ) + extraDrawables

    val fonts = extraFonts

    val colors = mapOf(
        "Button" to mapOf(Button.themeVars.fontColor to Color.WHITE),
        "Label" to mapOf(Label.themeVars.fontColor to Color.WHITE)
    ) + extraColors

    val constants = extraConstants

    return Theme(
        drawables = drawables,
        fonts = fonts,
        colors = colors,
        constants = constants,
        defaultFont = defaultFont
    )
}