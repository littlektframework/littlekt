package com.littlekt.graph.node.resource

import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.BitmapFont
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
        val FALLBACK_FONT: BitmapFont
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
 * @param defaultFont A [BitmapFont] that is used a default fo necessarily need explicitly set.
 * @param palette the colors of the theme, such as [ThemePalette.DARK] or [ThemePalette.LIGHT]
 * @param cornerRadius the corner radius, in pixels, of buttons, panels & inputs
 * @param srgb `true` when rendering to an sRGB target, which converts the [palette] to linear
 * @return the newly created theme
 */
fun createDefaultTheme(
    extraDrawables: Map<String, Map<String, Drawable>> = mapOf(),
    extraFonts: Map<String, Map<String, BitmapFont>> = mapOf(),
    extraColors: Map<String, Map<String, Color>> = mapOf(),
    extraConstants: Map<String, Map<String, Int>> = mapOf(),
    defaultFont: BitmapFont? = null,
    palette: ThemePalette = ThemePalette.DARK,
    cornerRadius: Int = 4,
    srgb: Boolean = false,
): Theme {
    val p = if (srgb) palette.toLinear() else palette
    val r = cornerRadius
    val disabledControl = p.control.withAlpha(0.45f)
    val disabledBorder = p.border.withAlpha(0.45f)
    val theme = theme {
        this.defaultFont = defaultFont ?: Fonts.defaultNoOutline
        type<Button> {
            drawables[Button.themeVars.normal] = FlatDrawable(p.control, p.border, 1, r)
            drawables[Button.themeVars.hover] = FlatDrawable(p.controlHover, p.border, 1, r)
            drawables[Button.themeVars.pressed] = FlatDrawable(p.controlPressed, p.accent, 1, r)
            drawables[Button.themeVars.hoverPressed] =
                FlatDrawable(p.controlHover, p.accent, 1, r)
            drawables[Button.themeVars.disabled] =
                FlatDrawable(disabledControl, disabledBorder, 1, r)
            drawables[Button.themeVars.focus] = FlatDrawable(Color.CLEAR, p.accent, 2, r)
            colors[Button.themeVars.fontColor] = p.text
            colors[Button.themeVars.fontColorDisabled] = p.textMuted
        }
        type<Label> { colors[Label.themeVars.fontColor] = p.text }
        type<Panel> {
            drawables[Panel.themeVars.panel] = FlatDrawable(p.surface, p.border, 1, r + 2, 8f)
            drawables[Panel.themeVars.panelForeground] =
                FlatDrawable(p.surface, p.border, 1, r + 2, 8f)
        }
        type<ProgressBar> {
            drawables[ProgressBar.themeVars.bg] = FlatDrawable(p.field, p.border, 1, r)
            drawables[ProgressBar.themeVars.fg] = FlatDrawable(p.accent, cornerRadius = r)
            colors[ProgressBar.themeVars.fontColor] = p.text
        }
        type<LineEdit> {
            drawables[LineEdit.themeVars.bg] =
                FlatDrawable(p.field, p.border, 1, r).apply {
                    marginLeft = 8f
                    marginRight = 8f
                    minWidth = 50f
                    minHeight = 28f
                }
            drawables[LineEdit.themeVars.disabled] =
                FlatDrawable(p.field.withAlpha(0.45f), disabledBorder, 1, r).apply {
                    marginLeft = 8f
                    marginRight = 8f
                    minWidth = 50f
                    minHeight = 28f
                }
            drawables[LineEdit.themeVars.focus] = FlatDrawable(Color.CLEAR, p.accent, 2, r)
            drawables[LineEdit.themeVars.caret] = FlatDrawable(p.text).apply { minWidth = 1f }
            drawables[LineEdit.themeVars.selection] = FlatDrawable(p.accent.withAlpha(0.4f))
            colors[LineEdit.themeVars.fontColor] = p.text
            colors[LineEdit.themeVars.fontColorPlaceholder] = p.textMuted
            colors[LineEdit.themeVars.fontColorDisabled] = p.textMuted
        }
        type<ScrollContainer> { drawables[ScrollContainer.themeVars.panel] = emptyDrawable() }
        val track = FlatDrawable(p.control, cornerRadius = 3, margin = 0f)
        val grabber = FlatDrawable(p.textMuted.withAlpha(0.6f), cornerRadius = 3, margin = 0f)
        val grabberHover = FlatDrawable(p.textMuted, cornerRadius = 3, margin = 0f)
        val grabberPressed = FlatDrawable(p.accent, cornerRadius = 3, margin = 0f)
        listOf("VScrollBar", "HScrollBar").forEach {
            type(it) {
                drawables[ScrollBar.themeVars.scroll] = track
                drawables[ScrollBar.themeVars.scrollFocused] = track
                drawables[ScrollBar.themeVars.grabber] = grabber
                drawables[ScrollBar.themeVars.grabberHighlight] = grabberHover
                drawables[ScrollBar.themeVars.grabberPressed] = grabberPressed
            }
        }
        type<VBoxContainer> { constants[BoxContainer.themeVars.separation] = 6 }
        type<HBoxContainer> { constants[BoxContainer.themeVars.separation] = 6 }
    }

    return Theme(
        drawables = theme.drawables + extraDrawables,
        fonts = theme.fonts + extraFonts,
        colors = theme.colors + extraColors,
        constants = theme.constants + extraConstants,
        defaultFont = theme.defaultFont,
    )
}
