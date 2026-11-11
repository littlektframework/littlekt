package com.littlekt.graph.node.resource

import com.littlekt.graph.node.annotation.ThemeDslMarker
import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.BitmapFont

/**
 * Builds a new [Theme]. Values of the [base] theme are kept unless they are set in [block].
 *
 * ```
 * val myTheme = theme {
 *     button {
 *         normal = FlatDrawable(Color.DARK_GRAY, cornerRadius = 4)
 *         fontColor = Color.WHITE
 *     }
 *     column { separation = 8 }
 *     type("MyControl") { colors["outline"] = Color.RED }
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
@ThemeDslMarker
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
        values<T>().apply(block)
    }

    /** Sets the theme values of [Button]s. */
    fun button(block: ButtonThemeScope.() -> Unit) = ButtonThemeScope(values<Button>()).block()

    /** Sets the theme values of [Label]s. */
    fun label(block: LabelThemeScope.() -> Unit) = LabelThemeScope(values<Label>()).block()

    /** Sets the theme values of [LineEdit]s. */
    fun lineEdit(block: LineEditThemeScope.() -> Unit) =
        LineEditThemeScope(values<LineEdit>()).block()

    /** Sets the theme values of [Panel]s. */
    fun panel(block: PanelThemeScope.() -> Unit) = PanelThemeScope(values<Panel>()).block()

    /** Sets the theme values of [PanelContainer]s. */
    fun panelContainer(block: PanelContainerThemeScope.() -> Unit) =
        PanelContainerThemeScope(values<PanelContainer>()).block()

    /** Sets the theme values of [ProgressBar]s. */
    fun progressBar(block: ProgressBarThemeScope.() -> Unit) =
        ProgressBarThemeScope(values<ProgressBar>()).block()

    /** Sets the theme values of [ScrollContainer]s. */
    fun scrollContainer(block: ScrollContainerThemeScope.() -> Unit) =
        ScrollContainerThemeScope(values<ScrollContainer>()).block()

    /** Sets the theme values of [VScrollBar]s. */
    fun vScrollBar(block: ScrollBarThemeScope.() -> Unit) =
        ScrollBarThemeScope(values<VScrollBar>()).block()

    /** Sets the theme values of [HScrollBar]s. */
    fun hScrollBar(block: ScrollBarThemeScope.() -> Unit) =
        ScrollBarThemeScope(values<HScrollBar>()).block()

    /** Sets the theme values of both [VScrollBar]s & [HScrollBar]s. */
    fun scrollBars(block: ScrollBarThemeScope.() -> Unit) {
        vScrollBar(block)
        hScrollBar(block)
    }

    /** Sets the theme values of [VBoxContainer]s. */
    fun column(block: BoxContainerThemeScope.() -> Unit) =
        BoxContainerThemeScope(values<VBoxContainer>()).block()

    /** Sets the theme values of [HBoxContainer]s. */
    fun row(block: BoxContainerThemeScope.() -> Unit) =
        BoxContainerThemeScope(values<HBoxContainer>()).block()

    /** Sets the theme values of [PaddedContainer]s. */
    fun paddedContainer(block: PaddedContainerThemeScope.() -> Unit) =
        PaddedContainerThemeScope(values<PaddedContainer>()).block()

    @PublishedApi
    internal inline fun <reified T : Control> values(): ThemeTypeBuilder =
        type(T::class.simpleName ?: error("Theme types need a class name."))

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
@ThemeDslMarker
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
