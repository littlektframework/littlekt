package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.graph.node.node2d.ui.Button
import com.lehaine.littlekt.graph.node.node2d.ui.Label
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
     * constants["Button"]["MyVar"]
     */
    val constants: Map<String, Map<String, Int>> = mapOf(),
    val defaultFont: BitmapFont? = null,
) {

    companion object {
        val FALLBACK_DRAWABLE = TextureSliceDrawable(Textures.white)
        val FALLBACK_FONT = Fonts.default

        var defaultTheme = Theme(
            drawables = mapOf(
                "Button" to mapOf(
                    Button.themeVars.normal to NinePatchDrawable(
                        NinePatch(
                            Textures.atlas.getByPrefix("grey_button_up").slice,
                            4,
                            4,
                            8,
                            4
                        )
                    ).apply { modulate = Color.LIGHT_BLUE },
                    Button.themeVars.pressed to NinePatchDrawable(
                        NinePatch(
                            Textures.atlas.getByPrefix("grey_button_down").slice,
                            4,
                            4,
                            4,
                            4
                        )
                    ).apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.scale(0.6f) } },
                    Button.themeVars.hover to NinePatchDrawable(
                        NinePatch(
                            Textures.atlas.getByPrefix("grey_button_up").slice,
                            4,
                            4,
                            8,
                            4
                        )
                    ).apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.scale(0.8f) } },
                    Button.themeVars.disabled to NinePatchDrawable(
                        NinePatch(
                            Textures.atlas.getByPrefix("grey_button_up").slice,
                            4,
                            4,
                            8,
                            4
                        )
                    ).apply { modulate = Color.LIGHT_BLUE.toMutableColor().also { it.lighten(0.5f) } },
                )
            ),
            colors = mapOf(
                "Button" to mapOf(Button.themeVars.fontColor to Color.WHITE),
                "Label" to mapOf(Label.themeVars.fontColor to Color.WHITE)
            )
        )
    }
}