package com.littlekt.graph.node.resource

import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.Font
import com.littlekt.resources.Fonts
import com.littlekt.resources.Textures

/**
 * Tracks all the drawables, fonts, colors, and constants of a theme used in a [Control].
 *
 * @author Colton Daily
 * @date 1/19/2022
 */
class Theme(
    /**
     * A map of drawables mapped by Node type, mapped by variable name and value.
     *
     * Eg:
     * ```
     * drawables["Button"]["pressed"] // directly
     * drawables["Button"][Button.themeVars.pressed] // or we can use the theme variables on each control
     * ```
     */
    val drawables: Map<String, Map<String, Drawable>> = mapOf(),

    /**
     * A map of fonts mapped by Node type, mapped by variable name and value.
     *
     * Eg:
     * ```
     * fonts["Button"]["font"] // directly
     * fonts["Button"][Button.themeVars.font] // or we can use the theme variables on each control
     * ```
     */
    val fonts: Map<String, Map<String, BitmapFont>> = mapOf(),

    /**
     * A map of colors mapped by Node type, mapped by variable name and value.
     *
     * Eg:
     * ```
     * colors["Button"]["fontColor"] // directly
     * colors["Button"][Button.themeVars.fontColor] // or we can use the theme variables on each control
     * ```
     */
    val colors: Map<String, Map<String, Color>> = mapOf(),

    /**
     * A map of constants mapped by Node type, mapped by variable name and value.
     *
     * Eg:
     * ```
     * constants["HBoxContainer"]["separation"] // directly
     * constants["HBoxContainer"][HBoxContainer.themeVars.separation] // or we can use the theme variables on each control
     * ```
     */
    val constants: Map<String, Map<String, Int>> = mapOf(),

    /**
     * A [BitmapFont] that is used a default font for any [Control] that needs a font but doesn't
     * necessarily need explicitly set.
     */
    val defaultFont: BitmapFont? = null,
) {

    companion object {
        /**
         * The last resort [Drawable] when all else fails when needing a drawable. Defaults to an
         * [emptyDrawable].
         */
        val FALLBACK_DRAWABLE: Drawable = emptyDrawable()

        /**
         * The last resort [Font] when all else fails when needing a font. Defaults to
         * [Fonts.default].
         */
        val FALLBACK_FONT: BitmapFont = Fonts.default

        private var _theme: Theme? = null

        /**
         * The default [Theme] that every [Control] should use by default, when a theme is
         * specified. This may be changed.
         */
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
 * Creates a new [Theme] using the default theme values and allowing to add or override any
 * additional theme values.
 *
 * @param extraDrawables A map of drawables mapped by Node type, mapped by variable name and value.
 *   Eg:
 * ```
 * drawables["Button"]["pressed"] // directly
 * drawables["Button"][Button.themeVars.pressed] // or we can use the theme variables on each control
 * ```
 *
 * @param extraFonts A map of fonts mapped by Node type, mapped by variable name and value. Eg:
 * ```
 * fonts["Button"]["font"] // directly
 * fonts["Button"][Button.themeVars.font] // or we can use the theme variables on each control
 * ```
 *
 * @param extraColors A map of colors mapped by Node type, mapped by variable name and value. Eg:
 * ```
 * colors["Button"]["fontColor"] // directly
 * colors["Button"][Button.themeVars.fontColor] // or we can use the theme variables on each control
 * ```
 *
 * @param extraConstants A map of constants mapped by Node type, mapped by variable name and value.
 *   Eg:
 * ```
 * constants["HBoxContainer"]["separation"] // directly
 * constants["HBoxContainer"][HBoxContainer.themeVars.separation] // or we can use the theme variables on each control
 * ```
 *
 * @param defaultFont A [BitmapFont] that is used a default fo necessarily need explicitly set.
 * @return the newly created theme
 */
fun createDefaultTheme(
    extraDrawables: Map<String, Map<String, Drawable>> = mapOf(),
    extraFonts: Map<String, Map<String, BitmapFont>> = mapOf(),
    extraColors: Map<String, Map<String, Color>> = mapOf(),
    extraConstants: Map<String, Map<String, Int>> = mapOf(),
    defaultFont: BitmapFont? = null,
): Theme {
    val greyButtonNinePatch = NinePatch(Textures.atlas.getByPrefix("grey_button").slice, 5, 5, 5, 4)
    val greyOutlineNinePatch =
        NinePatch(Textures.atlas.getByPrefix("grey_outline").slice, 2, 2, 2, 2)
    val panelNinePatch = NinePatch(Textures.atlas.getByPrefix("grey_panel").slice, 6, 6, 6, 6)

    val greyBoxNinePatch = NinePatch(Textures.atlas.getByPrefix("grey_box").slice, 7, 7, 6, 6)

    val greySliderBg = NinePatch(Textures.atlas.getByPrefix("grey_sliderBg").slice, 3, 3, 3, 3)

    val grayGrabber = NinePatch(Textures.atlas.getByPrefix("grey_grabber").slice, 3, 3, 3, 3)
    val darkBlue = Color.fromHex("242b33")
    val lightBlue = Color.fromHex("3d4754")

    val drawables =
        mapOf(
            "Button" to
                mapOf(
                    Button.themeVars.normal to
                        NinePatchDrawable(greyButtonNinePatch).apply { tint = lightBlue },
                    Button.themeVars.normal to
                        NinePatchDrawable(greyButtonNinePatch).apply { tint = lightBlue },
                    Button.themeVars.pressed to
                        NinePatchDrawable(greyButtonNinePatch).apply {
                            tint = lightBlue.scaleRgb(0.6f)
                        },
                    Button.themeVars.hover to
                        NinePatchDrawable(greyButtonNinePatch).apply {
                            tint = lightBlue.lighten(0.2f)
                        },
                    Button.themeVars.disabled to
                        NinePatchDrawable(greyButtonNinePatch).apply {
                            tint = lightBlue.lighten(0.5f)
                        },
                    Button.themeVars.focus to
                        NinePatchDrawable(greyOutlineNinePatch).apply { tint = Color.WHITE },
                ),
            "Panel" to
                mapOf(
                    Panel.themeVars.panel to
                        NinePatchDrawable(panelNinePatch).apply { tint = lightBlue }
                ),
            "ProgressBar" to
                mapOf(
                    ProgressBar.themeVars.bg to
                        NinePatchDrawable(greySliderBg).apply { tint = darkBlue },
                    ProgressBar.themeVars.fg to
                        NinePatchDrawable(greySliderBg).apply { tint = lightBlue.lighten(0.5f) }
                ),
            "LineEdit" to
                mapOf(
                    LineEdit.themeVars.bg to
                        NinePatchDrawable(greyBoxNinePatch).apply {
                            minWidth = 50f
                            minHeight = 25f
                            tint = darkBlue
                        },
                    LineEdit.themeVars.disabled to
                        NinePatchDrawable(greyBoxNinePatch).apply {
                            minWidth = 50f
                            minHeight = 25f
                            tint = darkBlue.lighten(0.2f)
                        },
                    LineEdit.themeVars.caret to
                        TextureSliceDrawable(Textures.white).apply { minWidth = 1f },
                    LineEdit.themeVars.selection to
                        TextureSliceDrawable(Textures.white).apply { tint = lightBlue },
                    LineEdit.themeVars.focus to
                        NinePatchDrawable(greyOutlineNinePatch).apply { tint = Color.WHITE },
                ),
            "ScrollContainer" to mapOf(ScrollContainer.themeVars.panel to emptyDrawable()),
            "VScrollBar" to
                mapOf(
                    ScrollBar.themeVars.scroll to
                        NinePatchDrawable(greySliderBg).apply { tint = darkBlue },
                    ScrollBar.themeVars.grabber to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue },
                    ScrollBar.themeVars.grabberPressed to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue.scaleRgb(0.6f) },
                    ScrollBar.themeVars.grabberHighlight to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue.lighten(0.2f) }
                ),
            "HScrollBar" to
                mapOf(
                    ScrollBar.themeVars.scroll to
                        NinePatchDrawable(greySliderBg).apply { tint = darkBlue },
                    ScrollBar.themeVars.grabber to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue },
                    ScrollBar.themeVars.grabberPressed to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue.scaleRgb(0.6f) },
                    ScrollBar.themeVars.grabberHighlight to
                        NinePatchDrawable(grayGrabber).apply { tint = lightBlue.lighten(0.2f) }
                )
        ) + extraDrawables

    val fonts = extraFonts

    val colors =
        mapOf(
            "Button" to mapOf(Button.themeVars.fontColor to Color.WHITE),
            "Label" to mapOf(Label.themeVars.fontColor to Color.WHITE),
            "LineEdit" to
                mapOf(
                    LineEdit.themeVars.fontColor to Color.WHITE,
                    LineEdit.themeVars.fontColorPlaceholder to Color.LIGHT_GRAY,
                    LineEdit.themeVars.fontColorDisabled to Color.LIGHT_GRAY
                )
        ) + extraColors

    val constants =
        mapOf(
            "VBoxContainer" to mapOf(BoxContainer.themeVars.separation to 5),
            "HBoxContainer" to mapOf(BoxContainer.themeVars.separation to 5)
        ) + extraConstants

    return Theme(
        drawables = drawables,
        fonts = fonts,
        colors = colors,
        constants = constants,
        defaultFont = defaultFont
    )
}
