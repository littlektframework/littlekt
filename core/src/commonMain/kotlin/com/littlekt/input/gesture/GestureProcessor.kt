package com.littlekt.input.gesture

import com.littlekt.input.InputProcessor
import com.littlekt.input.Pointer
import com.littlekt.math.Vec2f
import com.littlekt.util.datastructure.fastForEach

/**
 * An interface that handles gesture events.
 *
 * See [GestureController] for subscribing to an [InputProcessor] that emits gesture events.
 *
 * @author Colton Daily
 * @date 10/24/2022
 */
interface GestureProcessor {

    /**
     * Triggered when a pointer touches the screen.
     *
     * @see InputProcessor.touchDown
     */
    fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean = false

    /**
     * Triggered whe na tap occurred. A tap occurs when a pointer touches and lets go of the same
     * area of the screen without moving outside the tap zone. The tap zone is a rectangular area
     * around the initial tap position.
     *
     * @param screenX the x-coord of the tap
     * @param screenY the y-coord of the tap
     * @param count total number of taps
     * @param pointer the pointer that tapped
     * @return true if event is handled; false otherwise
     * @see GestureController.tapWidth
     * @see GestureController.tapHeight
     */
    fun tap(screenX: Float, screenY: Float, count: Int, pointer: Pointer): Boolean = false

    /**
     * Triggered when a touch is pressed for a certain duration to be considered a long press.
     *
     * @param screenX the x-coord of the long press
     * @param screenY the y-coord of the long press
     * @return true if event is handled; false otherwise
     * @see GestureController.longPressDuration
     */
    fun longPress(screenX: Float, screenY: Float): Boolean = false

    /**
     * Triggered when a user "flings" the screen by dragging and lifting their pointer. Reports last
     * velocity in pixels per second.
     *
     * @param velocityX the x velocity in seconds
     * @param velocityY the y velocity in seconds
     * @param pointer the pointer that flung
     * @return true if event is handled; false otherwise
     */
    fun fling(velocityX: Float, velocityY: Float, pointer: Pointer): Boolean = false

    /**
     * Triggered when a pointer is dragged over the screen.
     *
     * @param screenX the x-coord on the last drag event
     * @param screenY the y-coord on the last drag event
     * @param dx the difference in pixels to [screenX]
     * @param dy the different in pixels to [screenY]
     * @return true if event is handled; false otherwise
     */
    fun pan(screenX: Float, screenY: Float, dx: Float, dy: Float): Boolean = false

    /**
     * Triggered when panning is finished.
     *
     * @param screenX the last x-coord
     * @param screenY the last y-coord
     * @param pointer the last pointer
     * @return true if event is handled; false otherwise
     */
    fun panStop(screenX: Float, screenY: Float, pointer: Pointer): Boolean = false

    /**
     * Triggered when two pointers perform a pinch zoom gesture.
     *
     * @param initialDistance the distance between pointers when the gesture started
     * @param distance current distance between pointers
     * @return true if event is handled; false otherwise
     */
    fun zoom(initialDistance: Float, distance: Float): Boolean = false

    /**
     * Triggered when two pointers perform a pinch gesture.
     *
     * @param initialPos1 initial position of the first pointer
     * @param initialPos2 initial position of the second pointer
     * @param pos1 current position of the first pointer
     * @param pos2 current position of the second pointer
     * @return true if event is handled; false otherwise
     */
    fun pinch(initialPos1: Vec2f, initialPos2: Vec2f, pos1: Vec2f, pos2: Vec2f): Boolean = false

    /** Triggered when pinching is finished */
    fun pinchStop() = Unit
}

/**
 * A builder class to create a [GestureProcessor] easily.
 *
 * @author Colton Daily
 * @date 10/24/2022
 */
class GestureProcessorBuilder {
    private val touchDown = mutableListOf<(Float, Float, Pointer) -> Boolean>()
    private val tap = mutableListOf<(Float, Float, Int, Pointer) -> Boolean>()
    private val longPress = mutableListOf<(Float, Float) -> Boolean>()
    private val fling = mutableListOf<(Float, Float, Pointer) -> Boolean>()
    private val pan = mutableListOf<(Float, Float, Float, Float) -> Boolean>()
    private val panStop = mutableListOf<(Float, Float, Pointer) -> Boolean>()
    private val zoom = mutableListOf<(Float, Float) -> Boolean>()
    private val pinch = mutableListOf<(Vec2f, Vec2f, Vec2f, Vec2f) -> Boolean>()
    private val pinchStop = mutableListOf<() -> Unit>()

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.touchDown
     */
    fun onTouchDownHandle(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Boolean) {
        touchDown += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.touchDown
     */
    fun onTouchDown(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Unit) {
        touchDown += { screenX, screenY, pointer ->
            action(screenX, screenY, pointer)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.tap
     */
    fun onTapHandle(
        action: (screenX: Float, screenY: Float, count: Int, pointer: Pointer) -> Boolean
    ) {
        tap += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.tap
     */
    fun onTap(action: (screenX: Float, screenY: Float, count: Int, pointer: Pointer) -> Unit) {
        tap += { screenX, screenY, count, pointer ->
            action(screenX, screenY, count, pointer)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.longPress
     */
    fun onLongPressHandle(action: (screenX: Float, screenY: Float) -> Boolean) {
        longPress += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.longPress
     */
    fun onLongPress(action: (screenX: Float, screenY: Float) -> Unit) {
        longPress += { screenX, screenY ->
            action(screenX, screenY)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.fling
     */
    fun onFlingHandle(action: (velocityX: Float, velocityY: Float, pointer: Pointer) -> Boolean) {
        fling += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.fling
     */
    fun onFling(action: (velocityX: Float, velocityY: Float, pointer: Pointer) -> Unit) {
        fling += { velocityX, velocityY, pointer ->
            action(velocityX, velocityY, pointer)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.pan
     */
    fun onPanHandle(action: (screenX: Float, screenY: Float, dx: Float, dy: Float) -> Boolean) {
        pan += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.pan
     */
    fun onPan(action: (screenX: Float, screenY: Float, dx: Float, dy: Float) -> Unit) {
        pan += { screenX, screenY, dx, dy ->
            action(screenX, screenY, dx, dy)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.panStop
     */
    fun onPanStopHandle(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Boolean) {
        panStop += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.panStop
     */
    fun onPanStop(action: (screenX: Float, screenY: Float, pointer: Pointer) -> Unit) {
        panStop += { screenX, screenY, pointer ->
            action(screenX, screenY, pointer)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.zoom
     */
    fun onZoomHandle(action: (initialDistance: Float, distance: Float) -> Boolean) {
        zoom += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.zoom
     */
    fun onZoom(action: (initialDistance: Float, distance: Float) -> Unit) {
        zoom += { initialDistance, distance ->
            action(initialDistance, distance)
            false
        }
    }

    /**
     * Need to specify if the event is handled.
     *
     * @see GestureProcessor.pinch
     */
    fun onPinchHandle(
        action: (initialPos1: Vec2f, initialPos2: Vec2f, pos1: Vec2f, pos2: Vec2f) -> Boolean
    ) {
        pinch += action
    }

    /**
     * Marks the event as unhandled by default.
     *
     * @see GestureProcessor.pinch
     */
    fun onPinch(
        action: (initialPos1: Vec2f, initialPos2: Vec2f, pos1: Vec2f, pos2: Vec2f) -> Unit
    ) {
        pinch += { initialPos1, initialPos2, pos1, pos2 ->
            action(initialPos1, initialPos2, pos1, pos2)
            false
        }
    }

    /** @see GestureProcessor.pinchStop */
    fun onPinchStop(action: () -> Unit) {
        pinchStop += action
    }

    /** Builds the [GestureProcessor]. */
    fun build() =
        object : GestureProcessor {
            override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                var handled = false
                touchDown.fastForEach { if (it.invoke(screenX, screenY, pointer)) handled = true }
                return handled
            }

            override fun tap(
                screenX: Float,
                screenY: Float,
                count: Int,
                pointer: Pointer
            ): Boolean {
                var handled = false
                tap.fastForEach { if (it.invoke(screenX, screenY, count, pointer)) handled = true }
                return handled
            }

            override fun longPress(screenX: Float, screenY: Float): Boolean {
                var handled = false
                longPress.fastForEach { if (it.invoke(screenX, screenY)) handled = true }
                return handled
            }

            override fun fling(velocityX: Float, velocityY: Float, pointer: Pointer): Boolean {
                var handled = false
                fling.fastForEach { if (it.invoke(velocityX, velocityY, pointer)) handled = true }
                return handled
            }

            override fun pan(screenX: Float, screenY: Float, dx: Float, dy: Float): Boolean {
                var handled = false
                pan.fastForEach { if (it.invoke(screenX, screenY, dx, dy)) handled = true }
                return handled
            }

            override fun panStop(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
                var handled = false
                panStop.fastForEach { if (it.invoke(screenX, screenY, pointer)) handled = true }
                return handled
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                var handled = false
                zoom.fastForEach { if (it.invoke(initialDistance, distance)) handled = true }
                return handled
            }

            override fun pinch(
                initialPos1: Vec2f,
                initialPos2: Vec2f,
                pos1: Vec2f,
                pos2: Vec2f
            ): Boolean {
                var handled = false
                pinch.fastForEach {
                    if (it.invoke(initialPos1, initialPos2, pos1, pos2)) handled = true
                }
                return handled
            }

            override fun pinchStop() {
                pinchStop.fastForEach { it.invoke() }
            }
        }
}
