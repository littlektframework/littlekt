package com.lehaine.littlekt.input

import com.lehaine.littlekt.input.internal.InternalTouchEvent
import com.lehaine.littlekt.input.internal.InternalTouchEventDirection
import com.lehaine.littlekt.math.Vector2
import com.lehaine.littlekt.util.ObjectPool

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
typealias KeyCode = Int

class TouchManager(lastKeyCode: KeyCode) {

    private val touchSignalCache = TouchSignal.values()

    private val type = Array<Any?>(TouchSignal.values().size) { null }
    private val touch = Array<Vector2?>(TouchSignal.values().size) { null }
    private val justTouch = Array<Vector2?>(TouchSignal.values().size) { null }

    private val keyPressed = Array(lastKeyCode + 1) { false }
    private val justKeyPressed = Array(lastKeyCode + 1) { false }
    private val justPressedKeyCode = mutableSetOf<KeyCode>()

    /**
     * Any key pressed will be computed regarding the number of key pressed.
     * If more than 0, it means that a least one key is pressed.
     */
    private var numberOfKeyPressed = 0

    /**
     * Is any key is currently pressed?
     */
    val isAnyKeyPressed: Boolean
        get() = numberOfKeyPressed != 0

    /**
     * Is any key was just pressed?
     */
    var isAnyKeyJustPressed = false
        private set

    private val eventsPool = InternalTouchEventObjectPool()

    inner class InternalTouchEventObjectPool : ObjectPool<InternalTouchEvent>(10) {

        override fun newInstance(): InternalTouchEvent {
            return InternalTouchEvent()
        }

        override fun destroyInstance(obj: InternalTouchEvent) = Unit
    }

    private val queueEvents = mutableListOf<InternalTouchEvent>()

    /**
     * Converting the platform specific touch [id] into a [TouchSignal].
     */
    fun getTouchSignal(id: Any): TouchSignal {
        type.forEachIndexed { index, savedId ->
            if (id == savedId) {
                return touchSignalCache[index]
            }
        }
        return assignTouchSignal(id)
    }

    private fun assignTouchSignal(id: Any): TouchSignal {
        val ordinal = type.indexOfFirst { it == null }
        val touchSignal = touchSignalCache[ordinal]
        type[ordinal] = id
        return touchSignal
    }

    /**
     * To be called when a touch has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchDown(touchSignal: TouchSignal, x: Float, y: Float) {
        val event = eventsPool.newInstance()

        val ordinal = touchSignal.ordinal
        // Keep the position if the touch is currently touched
        val position = touch[ordinal] ?: event.position

        event.direction = InternalTouchEventDirection.DOWN
        event.touchSignal = touchSignal
        event.position = position.apply {
            position.x = x
            position.y = y
        }
        queueEvents.add(event)
    }

    /**
     * To be called when a touch move has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchMove(touchSignal: TouchSignal, x: Float, y: Float) {
        val event = eventsPool.newInstance()
        event.direction = InternalTouchEventDirection.MOVE
        event.position.x = x
        event.position.y = y
        event.touchSignal = touchSignal
        queueEvents.add(event)
    }

    /**
     * To be called when a touch up has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchUp(touchSignal: TouchSignal) {
        val event = eventsPool.newInstance()
        event.direction = InternalTouchEventDirection.UP
        event.touchSignal = touchSignal
        queueEvents.add(event)
    }

    fun isTouched(touchSignal: TouchSignal): Vector2? {
        return touch[touchSignal.ordinal]
    }

    fun isJustTouched(touchSignal: TouchSignal): Vector2? {
        return justTouch[touchSignal.ordinal]
    }

    fun isKeyPressed(keyCode: KeyCode): Boolean = keyPressed[keyCode]

    fun isKeyJustPressed(keyCode: KeyCode): Boolean = justKeyPressed[keyCode]

    fun onKeyPressed(keyCode: KeyCode) {
        val event = eventsPool.newInstance()
        event.direction = InternalTouchEventDirection.DOWN
        event.keycode = keyCode
        queueEvents.add(event)
    }

    fun onKeyReleased(keyCode: KeyCode) {
        val event = eventsPool.newInstance()
        event.direction = InternalTouchEventDirection.UP
        event.keycode = keyCode
        queueEvents.add(event)
    }

    /**
     * Process received touch events
     *
     * Previous received events will be discarded.
     */
    fun processReceivedEvent() {
        // Reset previous justTouch
        (justTouch.indices).forEach { i ->
            justTouch[i] = null
        }

        // Reset previous key pressed
        justPressedKeyCode.forEach {
            justKeyPressed[it] = false
        }
        justPressedKeyCode.clear()
        isAnyKeyJustPressed = false

        queueEvents.forEach { event ->
            if (event.isTouchEvent) {
                processTouchEvent(event)
            } else {
                processKeyEvent(event)
            }
        }
        eventsPool.free(queueEvents)
        queueEvents.clear()
    }

    private fun processKeyEvent(event: InternalTouchEvent) {
        val keycode = event.keycode!!
        when (event.direction) {
            InternalTouchEventDirection.DOWN -> {
                keyPressed[keycode] = true
                justKeyPressed[keycode] = true
                justPressedKeyCode.add(keycode)
                isAnyKeyJustPressed = true
                numberOfKeyPressed++
            }
            InternalTouchEventDirection.UP -> {
                keyPressed[keycode] = false
                numberOfKeyPressed--
            }
            InternalTouchEventDirection.MOVE -> throw IllegalArgumentException("${event.keycode} is not supposed to move.")
        }
    }

    private fun processTouchEvent(event: InternalTouchEvent) {
        when (event.direction) {
            InternalTouchEventDirection.DOWN -> {
                justTouch[event.touchSignal.ordinal] = event.position
                touch[event.touchSignal.ordinal] = event.position
            }
            InternalTouchEventDirection.MOVE -> {
                touch[event.touchSignal.ordinal]?.run {
                    x = event.position.x
                    y = event.position.y
                }
            }
            InternalTouchEventDirection.UP -> {
                touch[event.touchSignal.ordinal] = null
            }
        }
    }
}