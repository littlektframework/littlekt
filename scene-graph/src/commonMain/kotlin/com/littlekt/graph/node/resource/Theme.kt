package com.littlekt.graph.node.resource

import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.TextureFont
import com.littlekt.graphics.g2d.font.Font
import com.littlekt.resources.Fonts

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
    val fonts: Map<String, Map<String, TextureFont>> = mapOf(),

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
     * A [TextureFont] that is used a default font for any [Control] that needs a font but doesn't
     * necessarily need explicitly set.
     */
    val defaultFont: TextureFont? = null,
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
        val FALLBACK_FONT: TextureFont
            get() = Fonts.default

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

        internal fun initDefaultTheme(srgb: Boolean) {
            if (_theme == null) _theme = createDefaultTheme(srgb = srgb)
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
 * @param defaultFont the font of any [Control] without its own font. Defaults to
 *   [Fonts.defaultRaster].
 * @param palette the colors of the theme, such as [ThemePalette.DARK] or [ThemePalette.LIGHT]
 * @param cornerRadius the corner radius, in pixels, of buttons, panels & inputs
 * @param srgb `true` when rendering to an sRGB target, which converts the [palette] to linear
 * @return the newly created theme
 */
fun createDefaultTheme(
    extraDrawables: Map<String, Map<String, Drawable>> = mapOf(),
    extraFonts: Map<String, Map<String, TextureFont>> = mapOf(),
    extraColors: Map<String, Map<String, Color>> = mapOf(),
    extraConstants: Map<String, Map<String, Int>> = mapOf(),
    defaultFont: TextureFont? = null,
    palette: ThemePalette = ThemePalette.DARK,
    cornerRadius: Int = 4,
    srgb: Boolean = false,
): Theme {
    val p = if (srgb) palette.toLinear() else palette
    val r = cornerRadius
    val disabledControl = p.control.withAlpha(0.45f)
    val disabledBorder = p.border.withAlpha(0.45f)
    val theme = theme {
        this.defaultFont = defaultFont ?: Fonts.defaultRaster
        button {
            normal = FlatDrawable(p.control, p.border, 1, r)
            hover = FlatDrawable(p.controlHover, p.border, 1, r)
            pressed = FlatDrawable(p.controlPressed, p.accent, 1, r)
            hoverPressed = FlatDrawable(p.controlHover, p.accent, 1, r)
            disabled = FlatDrawable(disabledControl, disabledBorder, 1, r)
            focus = FlatDrawable(Color.CLEAR, p.accent, 2, r)
            fontColor = p.text
            fontColorDisabled = p.textMuted
        }
        label { fontColor = p.text }
        panel {
            panel = FlatDrawable(p.surface, p.border, 1, r + 2, 8f)
            panelForeground = FlatDrawable(p.surface, p.border, 1, r + 2, 8f)
        }
        progressBar {
            bg = FlatDrawable(p.field, p.border, 1, r)
            fg = FlatDrawable(p.accent, cornerRadius = r)
            fontColor = p.text
        }
        lineEdit {
            bg = inputField(p.field, p.border, r)
            disabled = inputField(p.field.withAlpha(0.45f), disabledBorder, r)
            focus = FlatDrawable(Color.CLEAR, p.accent, 2, r)
            caret = FlatDrawable(p.text).apply { minWidth = 1f }
            selection = FlatDrawable(p.accent.withAlpha(0.4f))
            fontColor = p.text
            fontColorPlaceholder = p.textMuted
            fontColorDisabled = p.textMuted
        }
        scrollContainer { panel = emptyDrawable() }
        scrollBars {
            val track = FlatDrawable(p.control, cornerRadius = 3, margin = 0f)
            scroll = track
            scrollFocused = track
            grabber = FlatDrawable(p.textMuted.withAlpha(0.6f), cornerRadius = 3, margin = 0f)
            grabberHighlight = FlatDrawable(p.textMuted, cornerRadius = 3, margin = 0f)
            grabberPressed = FlatDrawable(p.accent, cornerRadius = 3, margin = 0f)
        }
        column { separation = 6 }
        row { separation = 6 }
    }

    return Theme(
        drawables = theme.drawables + extraDrawables,
        fonts = theme.fonts + extraFonts,
        colors = theme.colors + extraColors,
        constants = theme.constants + extraConstants,
        defaultFont = theme.defaultFont,
    )
}

private fun inputField(color: Color, border: Color, radius: Int) =
    FlatDrawable(color, border, 1, radius).apply {
        marginLeft = 8f
        marginRight = 8f
        minWidth = 50f
        minHeight = 28f
    }
