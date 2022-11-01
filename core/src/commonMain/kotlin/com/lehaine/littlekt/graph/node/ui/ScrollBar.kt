package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.Orientation
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.math.clamp
import kotlin.math.max

/**
 * A abstract [Range]-based [Control] that displays a draggable area ([page]). See [HScrollBar] or [VScrollBar] in order to
 * use it.
 * @author Colton Daily
 * @date 10/16/2022
 */
abstract class ScrollBar(val orientation: Orientation = Orientation.VERTICAL) : Range() {

    /**
     * The [Drawable] of the normal state of the increment button.
     */
    var increment: Drawable
        get() = getThemeDrawable(themeVars.incrementIcon)
        set(value) {
            drawableOverrides[themeVars.incrementIcon] = value
        }

    /**
     * The [Drawable] of the pressed state of the increment button.
     */
    var incrementPressed: Drawable
        get() = getThemeDrawable(themeVars.incrementPressedIcon)
        set(value) {
            drawableOverrides[themeVars.incrementPressedIcon] = value
        }

    /**
     * The [Drawable] of the highlight/hover state of the increment button.
     */
    var incrementHighlight: Drawable
        get() = getThemeDrawable(themeVars.incrementHighlightIcon)
        set(value) {
            drawableOverrides[themeVars.incrementHighlightIcon] = value
        }

    /**
     * The [Drawable]of the normal state of the decrement button.
     */
    var decrement: Drawable
        get() = getThemeDrawable(themeVars.decrementIcon)
        set(value) {
            drawableOverrides[themeVars.decrementIcon] = value
        }

    /**
     * The [Drawable] of the pressed state of the decrement button.
     */
    var decrementPressed: Drawable
        get() = getThemeDrawable(themeVars.decrementPressedIcon)
        set(value) {
            drawableOverrides[themeVars.decrementPressedIcon] = value
        }

    /**
     * The [Drawable] of the highlight/hover state of the decrement button.
     */
    var decrementHighlight: Drawable
        get() = getThemeDrawable(themeVars.decrementHighlightIcon)
        set(value) {
            drawableOverrides[themeVars.decrementHighlightIcon] = value
        }

    /**
     * The [Drawable] of the normal state of the scrolling background.
     */
    var scroll: Drawable
        get() = getThemeDrawable(themeVars.scroll)
        set(value) {
            drawableOverrides[themeVars.scroll] = value
        }

    /**
     * The [Drawable] of the focused state of the scrolling background.
     */
    var scrollFocused: Drawable
        get() = getThemeDrawable(themeVars.scrollFocused)
        set(value) {
            drawableOverrides[themeVars.scrollFocused] = value
        }

    /**
     * The [Drawable] of the normal state of the "grabber" of the scrollbar.
     */
    var grabber: Drawable
        get() = getThemeDrawable(themeVars.grabber)
        set(value) {
            drawableOverrides[themeVars.grabber] = value
        }

    /**
     * The [Drawable] of the pressed state of the "grabber" of the scrollbar.
     */
    var grabbedPressed: Drawable
        get() = getThemeDrawable(themeVars.grabberPressed)
        set(value) {
            drawableOverrides[themeVars.grabberPressed] = value
        }

    /**
     * The [Drawable] of the highlight/hover state of the "grabber" of the scrollbar.
     */
    var grabberHighlight: Drawable
        get() = getThemeDrawable(themeVars.grabberHighlight)
        set(value) {
            drawableOverrides[themeVars.grabberHighlight] = value
        }


    private var positionAtTouch: Float = 0f
    private var valueAtTouch: Float = 0f

    private var highlight = HighlightStatus.NONE
    private var dragActive = false
    private var decrementActive = false
    private var incrementActive = false

    override fun uiInput(event: InputEvent<*>) {
        if (event.type == InputEvent.Type.MOUSE_EXIT) {
            highlight = HighlightStatus.NONE
            return
        }

        val scrolling = event.type == InputEvent.Type.SCROLLED
        val touchDown = event.type == InputEvent.Type.TOUCH_DOWN
        val dragging = event.type == InputEvent.Type.TOUCH_DRAGGED

        if (scrolling) {
            event.handle()
            if (event.scrollAmountY > 0) {
                value += page / 4f
            }

            if (event.scrollAmountY < 0) {
                value -= page / 4f
            }
        }

        if (touchDown) {
            event.handle()
            var offset = if (orientation == Orientation.VERTICAL) event.localY else event.localX
            val decrementSize = if (orientation == Orientation.VERTICAL) decrement.minHeight else decrement.minWidth
            val incrementSize = if (orientation == Orientation.VERTICAL) increment.minHeight else increment.minWidth
            val grabberOffset = getGrabberOffset()
            val grabberSize = getGrabberSize()
            val total = if (orientation == Orientation.VERTICAL) height else width

            if (offset < decrementSize) {
                decrementActive = true
                value -= step
                return
            }

            if (offset > total - incrementSize) {
                incrementActive = true
                value += step
                return
            }

            offset -= decrementSize

            if (offset < grabberOffset) {
                value = (value - page).clamp(min, max - page)
                return
            }

            offset -= grabberOffset

            if (offset < grabberSize) {
                positionAtTouch = grabberOffset + offset
                valueAtTouch = ratio
                dragActive = true
            } else {
                value = (value - page).clamp(min, max - page)
            }
        }

        if (event.type == InputEvent.Type.TOUCH_UP) {
            incrementActive = false
            decrementActive = false
            dragActive = false
        }

        if (dragging && dragActive) {
            var offset = if (orientation == Orientation.VERTICAL) event.localY else event.localX
            val decrementSize = if (orientation == Orientation.VERTICAL) decrement.minHeight else decrement.minWidth
            offset -= decrementSize

            val diff = (offset - positionAtTouch) / getAreaSize()
            ratio = valueAtTouch + diff
        }

        if (event.type == InputEvent.Type.MOUSE_ENTER || event.type == InputEvent.Type.MOUSE_HOVER) {
            val offset = if (orientation == Orientation.VERTICAL) event.localY else event.localX
            val decrementSize = if (orientation == Orientation.VERTICAL) decrement.minHeight else decrement.minWidth
            val incrementSize = if (orientation == Orientation.VERTICAL) increment.minHeight else increment.minWidth
            val total = if (orientation == Orientation.VERTICAL) height else width

            highlight = if (offset < decrementSize) {
                HighlightStatus.DECREMENT
            } else if (offset > total - incrementSize) {
                HighlightStatus.INCREMENT
            } else {
                HighlightStatus.RANGE
            }
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        val increment = if (incrementActive) {
            incrementPressed
        } else if (highlight == HighlightStatus.INCREMENT) {
            incrementHighlight
        } else {
            increment
        }
        val decrement = if (decrementActive) {
            decrementPressed
        } else if (highlight == HighlightStatus.DECREMENT) {
            decrementHighlight
        } else {
            decrement
        }

        val bg = if (hasFocus) scrollFocused else scroll
        val grabber = if (dragActive) {
            grabbedPressed
        } else if (highlight == HighlightStatus.RANGE) {
            grabberHighlight
        } else {
            grabber
        }

        decrement.draw(
            batch,
            globalPosition.x,
            globalPosition.y,
            decrement.minWidth,
            decrement.minHeight,
            globalScaleX,
            globalScaleY,
            globalRotation
        )

        var offsetX = globalPosition.x
        var offsetY = globalPosition.y
        if (orientation == Orientation.HORIZONTAL) {
            offsetX += decrement.minWidth
        } else {
            offsetY += decrement.minHeight
        }

        var areaWidth = width
        var areaHeight = height
        if (orientation == Orientation.HORIZONTAL) {
            areaWidth -= increment.minWidth + decrement.minWidth
        } else {
            areaHeight -= increment.minHeight + decrement.minHeight
        }

        bg.draw(batch, offsetX, offsetY, areaWidth, areaHeight, globalScaleX, globalScaleY, globalRotation)

        if (orientation == Orientation.HORIZONTAL) {
            offsetX += areaWidth
        } else {
            offsetY += areaHeight
        }

        increment.draw(
            batch,
            offsetX,
            offsetY,
            increment.minWidth,
            increment.minHeight,
            globalScaleX,
            globalScaleY,
            globalRotation
        )

        val grabberWidth: Float
        val grabberHeight: Float
        var grabberX = globalPosition.x
        var grabberY = globalPosition.y
        if (orientation == Orientation.HORIZONTAL) {
            grabberWidth = getGrabberSize()
            grabberHeight = height
            grabberX += getGrabberOffset() + decrement.minWidth + bg.marginLeft
        } else {
            grabberWidth = width
            grabberHeight = getGrabberSize()
            grabberY += getGrabberOffset() + decrement.minHeight + bg.marginTop
        }

        grabber.draw(batch, grabberX, grabberY, grabberWidth, grabberHeight, globalScaleX, globalScaleY, globalRotation)
    }

    private fun getGrabberSize(): Float {
        val range = max - min
        if (range <= 0) return 0f

        val page = if (page > 0f) page else 0f
        val area = getAreaSize()
        val grabberSize = page / range * area
        return grabberSize + getGrabberMinSize()
    }

    private fun getGrabberMinSize(): Float {
        return if (orientation == Orientation.VERTICAL) grabber.minHeight else grabber.minWidth
    }

    private fun getGrabberOffset(): Float = getAreaSize() * ratio
    private fun getAreaSize(): Float {
        when (orientation) {
            Orientation.VERTICAL -> {
                var area = height
                area -= scroll.minHeight
                area -= increment.minHeight
                area -= decrement.minHeight
                area -= getGrabberMinSize()
                return area
            }

            Orientation.HORIZONTAL -> {
                var area = width
                area -= scroll.minWidth
                area -= increment.minWidth
                area -= decrement.minWidth
                area -= getGrabberMinSize()
                return area
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        if (orientation == Orientation.VERTICAL) {
            _internalMinWidth = max(increment.minWidth, scroll.minWidth)
            _internalMinHeight = increment.minHeight
            _internalMinHeight += decrement.minHeight
            _internalMinHeight += scroll.minHeight
            _internalMinHeight += getGrabberMinSize()
        } else if (orientation == Orientation.HORIZONTAL) {
            _internalMinHeight = max(increment.minHeight, scroll.minHeight)
            _internalMinWidth = increment.minWidth
            _internalMinWidth += decrement.minWidth
            _internalMinWidth += scroll.minWidth
            _internalMinWidth += getGrabberMinSize()
        }


        minSizeInvalid = false
    }

    enum class HighlightStatus {
        NONE,
        DECREMENT,
        RANGE,
        INCREMENT
    }

    class ThemeVars {
        val incrementIcon = "incrementIcon"
        val decrementIcon = "decrementIcon"
        val incrementPressedIcon = "incrementPressedIcon"
        val decrementPressedIcon = "decrementPressedIcon"
        val incrementHighlightIcon = "incrementHighlightIcon"
        val decrementHighlightIcon = "decrementHighlightIcon"
        val scroll = "scroll"
        val scrollFocused = "scrollFocused"
        val grabber = "grabber"
        val grabberPressed = "grabberPressed"
        val grabberHighlight = "grabberHighlight"
    }

    companion object {

        /**
         * [Theme] related variable names when setting theme values for a [ScrollBar]
         */
        val themeVars = ThemeVars()
    }
}