package com.littlekt.graph.node.resource

import com.littlekt.graph.node.annotation.ThemeDslMarker
import com.littlekt.graph.node.ui.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.font.BitmapFont

private fun <T : Any> MutableMap<String, T>.update(name: String, value: T?) {
    if (value == null) remove(name) else this[name] = value
}

/**
 * The typed theme values of a [Button], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class ButtonThemeScope(private val values: ThemeTypeBuilder) {
    var normal: Drawable?
        get() = values.drawables[Button.themeVars.normal]
        set(value) = values.drawables.update(Button.themeVars.normal, value)

    var pressed: Drawable?
        get() = values.drawables[Button.themeVars.pressed]
        set(value) = values.drawables.update(Button.themeVars.pressed, value)

    var hover: Drawable?
        get() = values.drawables[Button.themeVars.hover]
        set(value) = values.drawables.update(Button.themeVars.hover, value)

    var hoverPressed: Drawable?
        get() = values.drawables[Button.themeVars.hoverPressed]
        set(value) = values.drawables.update(Button.themeVars.hoverPressed, value)

    var disabled: Drawable?
        get() = values.drawables[Button.themeVars.disabled]
        set(value) = values.drawables.update(Button.themeVars.disabled, value)

    var focus: Drawable?
        get() = values.drawables[Button.themeVars.focus]
        set(value) = values.drawables.update(Button.themeVars.focus, value)

    var fontColor: Color?
        get() = values.colors[Button.themeVars.fontColor]
        set(value) = values.colors.update(Button.themeVars.fontColor, value)

    var fontColorDisabled: Color?
        get() = values.colors[Button.themeVars.fontColorDisabled]
        set(value) = values.colors.update(Button.themeVars.fontColorDisabled, value)

    var font: BitmapFont?
        get() = values.fonts[Button.themeVars.font]
        set(value) = values.fonts.update(Button.themeVars.font, value)

}


/**
 * The typed theme values of a [Label], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class LabelThemeScope(private val values: ThemeTypeBuilder) {
    var fontColor: Color?
        get() = values.colors[Label.themeVars.fontColor]
        set(value) = values.colors.update(Label.themeVars.fontColor, value)

    var font: BitmapFont?
        get() = values.fonts[Label.themeVars.font]
        set(value) = values.fonts.update(Label.themeVars.font, value)

}


/**
 * The typed theme values of a [LineEdit], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class LineEditThemeScope(private val values: ThemeTypeBuilder) {
    var bg: Drawable?
        get() = values.drawables[LineEdit.themeVars.bg]
        set(value) = values.drawables.update(LineEdit.themeVars.bg, value)

    var pressed: Drawable?
        get() = values.drawables[LineEdit.themeVars.pressed]
        set(value) = values.drawables.update(LineEdit.themeVars.pressed, value)

    var disabled: Drawable?
        get() = values.drawables[LineEdit.themeVars.disabled]
        set(value) = values.drawables.update(LineEdit.themeVars.disabled, value)

    var focus: Drawable?
        get() = values.drawables[LineEdit.themeVars.focus]
        set(value) = values.drawables.update(LineEdit.themeVars.focus, value)

    var caret: Drawable?
        get() = values.drawables[LineEdit.themeVars.caret]
        set(value) = values.drawables.update(LineEdit.themeVars.caret, value)

    var selection: Drawable?
        get() = values.drawables[LineEdit.themeVars.selection]
        set(value) = values.drawables.update(LineEdit.themeVars.selection, value)

    var fontColor: Color?
        get() = values.colors[LineEdit.themeVars.fontColor]
        set(value) = values.colors.update(LineEdit.themeVars.fontColor, value)

    var fontColorDisabled: Color?
        get() = values.colors[LineEdit.themeVars.fontColorDisabled]
        set(value) = values.colors.update(LineEdit.themeVars.fontColorDisabled, value)

    var fontColorPlaceholder: Color?
        get() = values.colors[LineEdit.themeVars.fontColorPlaceholder]
        set(value) = values.colors.update(LineEdit.themeVars.fontColorPlaceholder, value)

    var font: BitmapFont?
        get() = values.fonts[LineEdit.themeVars.font]
        set(value) = values.fonts.update(LineEdit.themeVars.font, value)

}


/**
 * The typed theme values of a [Panel], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class PanelThemeScope(private val values: ThemeTypeBuilder) {
    var panel: Drawable?
        get() = values.drawables[Panel.themeVars.panel]
        set(value) = values.drawables.update(Panel.themeVars.panel, value)

    var panelForeground: Drawable?
        get() = values.drawables[Panel.themeVars.panelForeground]
        set(value) = values.drawables.update(Panel.themeVars.panelForeground, value)

}


/**
 * The typed theme values of a [PanelContainer], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class PanelContainerThemeScope(private val values: ThemeTypeBuilder) {
    var panel: Drawable?
        get() = values.drawables[PanelContainer.themeVars.panel]
        set(value) = values.drawables.update(PanelContainer.themeVars.panel, value)

}


/**
 * The typed theme values of a [ProgressBar], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class ProgressBarThemeScope(private val values: ThemeTypeBuilder) {
    var bg: Drawable?
        get() = values.drawables[ProgressBar.themeVars.bg]
        set(value) = values.drawables.update(ProgressBar.themeVars.bg, value)

    var fg: Drawable?
        get() = values.drawables[ProgressBar.themeVars.fg]
        set(value) = values.drawables.update(ProgressBar.themeVars.fg, value)

    var fontColor: Color?
        get() = values.colors[ProgressBar.themeVars.fontColor]
        set(value) = values.colors.update(ProgressBar.themeVars.fontColor, value)

    var font: BitmapFont?
        get() = values.fonts[ProgressBar.themeVars.font]
        set(value) = values.fonts.update(ProgressBar.themeVars.font, value)

}


/**
 * The typed theme values of a [VScrollBar] or [HScrollBar], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class ScrollBarThemeScope(private val values: ThemeTypeBuilder) {
    var scroll: Drawable?
        get() = values.drawables[ScrollBar.themeVars.scroll]
        set(value) = values.drawables.update(ScrollBar.themeVars.scroll, value)

    var scrollFocused: Drawable?
        get() = values.drawables[ScrollBar.themeVars.scrollFocused]
        set(value) = values.drawables.update(ScrollBar.themeVars.scrollFocused, value)

    var grabber: Drawable?
        get() = values.drawables[ScrollBar.themeVars.grabber]
        set(value) = values.drawables.update(ScrollBar.themeVars.grabber, value)

    var grabberPressed: Drawable?
        get() = values.drawables[ScrollBar.themeVars.grabberPressed]
        set(value) = values.drawables.update(ScrollBar.themeVars.grabberPressed, value)

    var grabberHighlight: Drawable?
        get() = values.drawables[ScrollBar.themeVars.grabberHighlight]
        set(value) = values.drawables.update(ScrollBar.themeVars.grabberHighlight, value)

    var incrementIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.incrementIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.incrementIcon, value)

    var incrementPressedIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.incrementPressedIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.incrementPressedIcon, value)

    var incrementHighlightIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.incrementHighlightIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.incrementHighlightIcon, value)

    var decrementIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.decrementIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.decrementIcon, value)

    var decrementPressedIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.decrementPressedIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.decrementPressedIcon, value)

    var decrementHighlightIcon: Drawable?
        get() = values.drawables[ScrollBar.themeVars.decrementHighlightIcon]
        set(value) = values.drawables.update(ScrollBar.themeVars.decrementHighlightIcon, value)

}


/**
 * The typed theme values of a [ScrollContainer], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class ScrollContainerThemeScope(private val values: ThemeTypeBuilder) {
    var panel: Drawable?
        get() = values.drawables[ScrollContainer.themeVars.panel]
        set(value) = values.drawables.update(ScrollContainer.themeVars.panel, value)

}


/**
 * The typed theme values of a [VBoxContainer] or [HBoxContainer], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class BoxContainerThemeScope(private val values: ThemeTypeBuilder) {
    var separation: Int?
        get() = values.constants[BoxContainer.themeVars.separation]
        set(value) = values.constants.update(BoxContainer.themeVars.separation, value)

}


/**
 * The typed theme values of a [PaddedContainer], used in a [theme] builder. `null` leaves a value unset.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
@ThemeDslMarker
class PaddedContainerThemeScope(private val values: ThemeTypeBuilder) {
    var paddingLeft: Int?
        get() = values.constants[PaddedContainer.themeVars.paddingLeft]
        set(value) = values.constants.update(PaddedContainer.themeVars.paddingLeft, value)

    var paddingTop: Int?
        get() = values.constants[PaddedContainer.themeVars.paddingTop]
        set(value) = values.constants.update(PaddedContainer.themeVars.paddingTop, value)

    var paddingRight: Int?
        get() = values.constants[PaddedContainer.themeVars.paddingRight]
        set(value) = values.constants.update(PaddedContainer.themeVars.paddingRight, value)

    var paddingBottom: Int?
        get() = values.constants[PaddedContainer.themeVars.paddingBottom]
        set(value) = values.constants.update(PaddedContainer.themeVars.paddingBottom, value)

    /** Sets the padding of every side to [value]. */
    fun padding(value: Int) = padding(value, value)

    /** Sets the left & right padding to [horizontal] and the top & bottom padding to [vertical]. */
    fun padding(horizontal: Int, vertical: Int) {
        paddingLeft = horizontal
        paddingRight = horizontal
        paddingTop = vertical
        paddingBottom = vertical
    }

}
