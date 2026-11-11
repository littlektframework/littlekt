package com.littlekt.graph.node.resource

import com.littlekt.graph.node.ui.Control
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.BitmapFont

/**
 * Builds a new [Theme]. Values of the [base] theme are kept unless they are set in [block].
 *
 * ```
 * val myTheme = theme {
 *     type<Button> {
 *         drawables[Button.themeVars.normal] = FlatDrawable(Color.DARK_GRAY, cornerRadius = 4)
 *         colors[Button.themeVars.fontColor] = Color.WHITE
 *     }
 *     type<VBoxContainer> { constants[BoxContainer.themeVars.separation] = 8 }
 * }
 * ```
 *
 * @param base the theme to start from, such as [Theme.defaultTheme]
 * @return the newly created theme
 */
inline fun theme(base: Theme? = null, block: ThemeBuilder.() -> Unit): Theme =
    ThemeBuilder(base).apply(block).build()

/**
 * A builder for a [Theme]. See [theme].
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class ThemeBuilder(base: Theme? = null) {
    private val types = mutableMapOf<String, ThemeTypeBuilder>()

    /** The default font of the theme. */
    var defaultFont: BitmapFont? = base?.defaultFont

    init {
        if (base != null) {
            base.drawables.forEach { (type, values) -> type(type).drawables.putAll(values) }
            base.fonts.forEach { (type, values) -> type(type).fonts.putAll(values) }
            base.colors.forEach { (type, values) -> type(type).colors.putAll(values) }
            base.constants.forEach { (type, values) -> type(type).constants.putAll(values) }
        }
    }

    /** Sets the theme values of a [Control] type by its class name, such as `"Button"`. */
    fun type(name: String, block: ThemeTypeBuilder.() -> Unit = {}): ThemeTypeBuilder =
        types.getOrPut(name) { ThemeTypeBuilder() }.apply(block)

    /** Sets the theme values of the [Control] type [T]. */
    inline fun <reified T : Control> type(noinline block: ThemeTypeBuilder.() -> Unit) {
        type(T::class.simpleName ?: error("Theme types need a class name."), block)
    }

    /** @return the built [Theme] */
    fun build(): Theme =
        Theme(
            drawables = types.collect { it.drawables },
            fonts = types.collect { it.fonts },
            colors = types.collect { it.colors },
            constants = types.collect { it.constants },
            defaultFont = defaultFont,
        )

    private inline fun <V> Map<String, ThemeTypeBuilder>.collect(
        values: (ThemeTypeBuilder) -> Map<String, V>
    ): Map<String, Map<String, V>> {
        val result = mutableMapOf<String, Map<String, V>>()
        forEach { (type, builder) ->
            val map = values(builder)
            if (map.isNotEmpty()) result[type] = map.toMap()
        }
        return result
    }
}

/**
 * The theme values of a single [Control] type. Keys are the control's theme variables, such as
 * `Button.themeVars.normal`.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class ThemeTypeBuilder {
    /** The drawables of this type. */
    val drawables: MutableMap<String, Drawable> = mutableMapOf()

    /** The fonts of this type. */
    val fonts: MutableMap<String, BitmapFont> = mutableMapOf()

    /** The colors of this type. */
    val colors: MutableMap<String, Color> = mutableMapOf()

    /** The constants of this type. */
    val constants: MutableMap<String, Int> = mutableMapOf()
}
