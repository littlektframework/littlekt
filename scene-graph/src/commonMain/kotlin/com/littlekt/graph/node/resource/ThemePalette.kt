package com.littlekt.graph.node.resource

import com.littlekt.graphics.Color

/**
 * The colors used by [createDefaultTheme]. Use [DARK] or [LIGHT] and [copy] to change any color,
 * such as the [accent]. Colors are in sRGB.
 *
 * @param background the color behind the UI, such as a clear color
 * @param surface the color of panels
 * @param control the color of buttons
 * @param controlHover the color of hovered buttons
 * @param controlPressed the color of pressed buttons
 * @param field the color of inputs, tracks & progress bar backgrounds
 * @param border the color of borders
 * @param accent the color of focus outlines, selections, progress & grabbers
 * @param text the color of text
 * @param textMuted the color of placeholder & disabled text
 * @author Colton Daily
 * @date 11/11/2026
 */
data class ThemePalette(
    val background: Color,
    val surface: Color,
    val control: Color,
    val controlHover: Color,
    val controlPressed: Color,
    val field: Color,
    val border: Color,
    val accent: Color,
    val text: Color,
    val textMuted: Color,
) {

    /** @return a copy of this palette with every color converted to linear, for sRGB targets */
    fun toLinear(): ThemePalette =
        ThemePalette(
            background = background.toLinear(),
            surface = surface.toLinear(),
            control = control.toLinear(),
            controlHover = controlHover.toLinear(),
            controlPressed = controlPressed.toLinear(),
            field = field.toLinear(),
            border = border.toLinear(),
            accent = accent.toLinear(),
            text = text.toLinear(),
            textMuted = textMuted.toLinear(),
        )

    companion object {
        /** A dark palette with a blue accent. */
        val DARK =
            ThemePalette(
                background = Color.fromHex("16181d"),
                surface = Color.fromHex("23262e"),
                control = Color.fromHex("2f343f"),
                controlHover = Color.fromHex("3a404d"),
                controlPressed = Color.fromHex("262a33"),
                field = Color.fromHex("1a1d23"),
                border = Color.fromHex("3b414d"),
                accent = Color.fromHex("5b8cff"),
                text = Color.fromHex("e6e9ef"),
                textMuted = Color.fromHex("8b93a1"),
            )

        /** A light palette with a blue accent. */
        val LIGHT =
            ThemePalette(
                background = Color.fromHex("e9ecf1"),
                surface = Color.fromHex("f7f8fa"),
                control = Color.fromHex("e4e7ec"),
                controlHover = Color.fromHex("d9dde4"),
                controlPressed = Color.fromHex("cdd2da"),
                field = Color.fromHex("ffffff"),
                border = Color.fromHex("c3c9d3"),
                accent = Color.fromHex("3d6fe6"),
                text = Color.fromHex("1d2129"),
                textMuted = Color.fromHex("6b7280"),
            )
    }
}
