package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.Orientation
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.input.Pointer

/**
 * @author Colton Daily
 * @date 10/16/2022
 */
open class ScrollBar(val orientation: Orientation = Orientation.VERTICAL) : Range() {

    private val increment: Drawable
        get() = getThemeDrawable(themeVars.incrementIcon)
    private val incrementPressed: Drawable
        get() = getThemeDrawable(themeVars.incrementPressedIcon)
    private val incrementHighlight: Drawable
        get() = getThemeDrawable(themeVars.incrementHighlightIcon)
    private val decrement: Drawable
        get() = getThemeDrawable(themeVars.decrementIcon)
    private val decrementPressed: Drawable
        get() = getThemeDrawable(themeVars.decrementPressedIcon)
    private val decrementHighlight: Drawable
        get() = getThemeDrawable(themeVars.decrementHighlightIcon)
    private val scroll: Drawable
        get() = getThemeDrawable(themeVars.scroll)
    private val scrollFocus: Drawable
        get() = getThemeDrawable(themeVars.scrollFocused)
    private val scrollOffset: Drawable
        get() = getThemeDrawable(themeVars.scrollOffset)
    private val grabber: Drawable
        get() = getThemeDrawable(themeVars.grabber)
    private val grabberPressed: Drawable
        get() = getThemeDrawable(themeVars.grabberPressed)
    private val grabberHighlight: Drawable
        get() = getThemeDrawable(themeVars.grabberHighlight)

    private var draggingPointer: Pointer? = null

    override fun uiInput(event: InputEvent<*>) {
        val scrolling = event.type == InputEvent.Type.SCROLLED
        if (scrolling) {
            event.handle()
            if (event.scrollAmountY > 0) {
                value += page / 4f
            }

            if (event.scrollAmountY < 0) {
                value -= page / 4f
            }
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        val increment = getThemeDrawable(themeVars.incrementIcon)
        val decrement = getThemeDrawable(themeVars.decrementIcon)

        val bg = if (hasFocus) getThemeDrawable(themeVars.scrollFocused) else getThemeDrawable(themeVars.scroll)
        val grabber = getThemeDrawable(themeVars.grabber)

        decrement.draw(
            batch,
            0f,
            0f,
            decrement.minWidth,
            decrement.minHeight,
            globalScaleX,
            globalScaleY,
            globalRotation
        )

        var offsetX = 0f
        var offsetY = 0f
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
        var grabberX = 0f
        var grabberY = 0f
        if (orientation == Orientation.HORIZONTAL) {
            grabberWidth = getGrabberSize()
            grabberHeight = height
            grabberX = getGrabberOffset() + decrement.minWidth + bg.marginLeft
        } else {
            grabberWidth = width
            grabberHeight = getGrabberSize()
            grabberY = getGrabberOffset() + decrement.minHeight + bg.marginTop
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
        val scrollOffset = "hscroll"
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