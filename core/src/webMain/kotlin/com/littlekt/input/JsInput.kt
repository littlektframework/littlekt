package com.littlekt.input

import com.littlekt.math.geom.Point
import com.littlekt.util.nativeGet
import com.littlekt.util.nativeIndexOf
import com.littlekt.util.nativeSet
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.WheelEvent
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsName
import kotlin.js.JsNumber
import kotlin.js.unsafeCast

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
class JsInput(val canvas: HTMLCanvasElement) : Input {
    private val inputCache = InputCache()
    private var mouseX = 0f
    private var mouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private val touchedPointers = mutableListOf<Pointer>()

    /** Holds the references to active touch identifiers indexed by assigned pointer number. */
    private val touchIdentifiers = createTouchIdentifiers()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = createGamepads()

    private val _connectedGamepads = mutableListOf<GamepadInfo>()
    override val connectedGamepads: List<GamepadInfo>
        get() = _connectedGamepads

    override val catchKeys: MutableList<Key> =
        mutableListOf(
            Key.SPACE,
            Key.SHIFT_LEFT,
            Key.SHIFT_RIGHT,
            Key.CTRL_LEFT,
            Key.CTRL_RIGHT,
            Key.TAB,
            Key.ARROW_UP,
            Key.ARROW_DOWN,
            Key.ARROW_LEFT,
            Key.ARROW_RIGHT,
        )

    private val Document.pointerLockElementSafe: Element?
        get() = this.unsafeCast<DocumentWithPointerLock>().pointerLockElement

    override val cursorLocked: Boolean
        get() = document.pointerLockElementSafe != null

    init {
        document.addEventListener("keydown", ::keyDown, false)
        document.addEventListener("keyup", ::keyUp, false)
        document.addEventListener("keypress", ::keyPress, false)
        canvas.addEventListener("touchstart", ::touchStart, false)
        canvas.addEventListener("touchmove", ::touchMove, false)
        canvas.addEventListener("touchend", ::touchEnd, false)
        canvas.addEventListener("mousedown", ::mouseDown, false)
        canvas.addEventListener("mouseup", ::mouseUp, false)
        canvas.addEventListener("mousemove", ::mouseMove, false)
        canvas.addEventListener("wheel", ::scroll, false)

        window.addEventListener(
            "gamepadconnected",
            { e ->
                val ge = e.unsafeCast<JsGamepadEvent>()
                nativeGet(gamepads, ge.gamepad.index).connected = true
                _connectedGamepads += nativeGet(gamepads, ge.gamepad.index)
            },
        )
        window.addEventListener(
            "gamepaddisconnected",
            { e ->
                val ge = e.unsafeCast<JsGamepadEvent>()
                nativeGet(gamepads, ge.gamepad.index).connected = false
                _connectedGamepads -= nativeGet(gamepads, ge.gamepad.index)
            },
        )
        nativeCheckForGamepads(gamepads)
    }

    private fun keyDown(event: Event) {
        event as KeyboardEvent
        if (catchKeys.contains(event.jsKey)) {
            event.preventDefault()
        }
        inputCache.onKeyDown(event.jsKey)
    }

    private fun keyUp(event: Event) {
        event as KeyboardEvent
        if (catchKeys.contains(event.jsKey)) {
            event.preventDefault()
        }
        inputCache.onKeyUp(event.jsKey)
    }

    private fun keyPress(event: Event) {
        event as KeyboardEvent
        inputCache.onCharTyped(event.charCode.toChar())
        inputCache.onKeyRepeat(event.jsKey)
    }

    private fun touchStart(event: Event) {
        event.stopPropagation()
        event.preventDefault()
        event as TouchEvent
        (0 until event.changedTouches.length).forEach {
            val touchEvent = event.changedTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            val pointerIndex = nativeIndexOf(touchIdentifiers, -1)
            if (pointerIndex >= 0) {
                nativeSet(touchIdentifiers, pointerIndex, touchEvent.identifier)
                inputCache.onTouchDown(x, y, pointerIndex.getPointer)
            }
        }
    }

    private fun touchMove(event: Event) {
        event.stopPropagation()
        event.preventDefault()
        event as TouchEvent
        (0 until event.changedTouches.length).forEach {
            val touchEvent = event.changedTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            val pointerIndex = nativeIndexOf(touchIdentifiers, touchEvent.identifier)
            if (pointerIndex >= 0) {
                inputCache.onMove(x, y, pointerIndex.getPointer)
            }
        }
    }

    private fun touchEnd(event: Event) {
        event.stopPropagation()
        event.preventDefault()
        event as TouchEvent
        (0 until event.changedTouches.length).forEach {
            val touchEvent = event.changedTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            val pointerIndex = nativeIndexOf(touchIdentifiers, touchEvent.identifier)
            if (pointerIndex >= 0) {
                nativeSet(touchIdentifiers, pointerIndex, -1)
                inputCache.onTouchUp(x, y, pointerIndex.getPointer)
            }
        }
    }


    private fun mouseDown(event: Event) {
        event as ExtendedMouseEvent
        _deltaX = 0f
        _deltaY = 0f
        if (cursorLocked) {
            mouseX += event.movementX.toFloat()
            mouseY += event.movementY.toFloat()
            inputCache.onTouchDown(mouseX, mouseY, event.button.getPointer)
        } else {
            val rect = canvas.getBoundingClientRect()
            val x = event.clientX.toFloat() - rect.left.toFloat()
            val y = event.clientY.toFloat() - rect.top.toFloat()
            mouseX = x
            mouseY = y
            inputCache.onTouchDown(x, y, event.button.getPointer)
        }
        touchedPointers += event.button.getPointer
    }

    private fun mouseUp(event: Event) {
        event as ExtendedMouseEvent
        if (cursorLocked) {
            _deltaX = event.movementX.toFloat()
            _deltaY = event.movementY.toFloat()
            mouseX += event.movementX.toFloat()
            mouseY += event.movementY.toFloat()
            inputCache.onTouchUp(mouseX, mouseY, event.button.getPointer)
        } else {
            val rect = canvas.getBoundingClientRect()
            val x = event.clientX.toFloat() - rect.left.toFloat()
            val y = event.clientY.toFloat() - rect.top.toFloat()
            _deltaX = x - mouseX
            _deltaY = y - mouseY
            mouseX = x
            mouseY = y
            inputCache.onTouchUp(x, y, event.button.getPointer)
        }
        touchedPointers -= event.button.getPointer
    }

    private fun mouseMove(event: Event) {
        event as ExtendedMouseEvent
        if (cursorLocked) {
            _deltaX = event.movementX.toFloat()
            _deltaY = event.movementY.toFloat()
            mouseX += event.movementX.toFloat()
            mouseY += event.movementY.toFloat()
        } else {
            val rect = canvas.getBoundingClientRect()
            val x = event.clientX.toFloat() - rect.left.toFloat()
            val y = event.clientY.toFloat() - rect.top.toFloat()
            _deltaX = x - mouseX
            _deltaY = y - mouseY
            mouseX = x
            mouseY = y
        }

        inputCache.onMove(mouseX, mouseY, touchedPointers.lastOrNull() ?: Pointer.POINTER1)
    }

    private fun scroll(event: Event) {
        event as WheelEvent
        event.preventDefault()
        inputCache.onScroll(-event.deltaX.toFloat(), -event.deltaY.toFloat())
    }

    fun update() {
        if (connectedGamepads.isEmpty()) return
        nativeUpdateGamepads(gamepads, inputCache)
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
        _deltaX = 0f
        _deltaY = 0f
    }

    override val x: Int
        get() = mouseX.toInt()

    override val y: Int
        get() = mouseY.toInt()

    override val deltaX: Int
        get() = _deltaX.toInt()

    override val deltaY: Int
        get() = _deltaY.toInt()

    override val isTouching: Boolean
        get() = inputCache.isTouching

    override val justTouched: Boolean
        get() = inputCache.justTouched

    override val pressure: Float
        get() = getPressure(Pointer.POINTER1)

    override val currentEventTime: Long
        get() = inputCache.currentEventTime

    override val axisLeftX: Float
        get() = getGamepadJoystickXDistance(GameStick.LEFT)

    override val axisLeftY: Float
        get() = getGamepadJoystickYDistance(GameStick.LEFT)

    override val axisRightX: Float
        get() = getGamepadJoystickXDistance(GameStick.RIGHT)

    override val axisRightY: Float
        get() = getGamepadJoystickYDistance(GameStick.RIGHT)

    override fun getX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) x else 0
    }

    override fun getY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) y else 0
    }

    override fun getDeltaX(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaX else 0
    }

    override fun getDeltaY(pointer: Pointer): Int {
        return if (pointer == Pointer.POINTER1) deltaY else 0
    }

    override fun isJustTouched(pointer: Pointer): Boolean {
        return inputCache.isJustTouched(pointer)
    }

    override fun isTouching(pointer: Pointer): Boolean {
        return inputCache.isTouching(pointer)
    }

    override fun isTouching(totalPointers: Int): Boolean {
        return inputCache.isTouching(totalPointers)
    }

    override fun isTouchJustReleased(pointer: Pointer): Boolean {
        return inputCache.isTouchJustReleased(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isJustTouched(pointer)) 1f else 0f
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return inputCache.isKeyJustPressed(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return inputCache.isKeyPressed(key)
    }

    override fun isKeyJustReleased(key: Key): Boolean {
        return inputCache.isKeyJustReleased(key)
    }

    override fun isGamepadButtonJustPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustPressed(button, gamepad)
    }

    override fun isGamepadButtonPressed(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonPressed(button, gamepad)
    }

    override fun isGamepadButtonJustReleased(button: GameButton, gamepad: Int): Boolean {
        return inputCache.isGamepadButtonJustReleased(button, gamepad)
    }

    override fun getGamepadButtonPressure(button: GameButton, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) nativeGet(gamepads, gamepad)[button] else 0f
    }

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point {
        return if (connectedGamepads.isNotEmpty()) nativeGet(gamepads, gamepad)[stick] else Point.ZERO
    }

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) nativeGet(gamepads, gamepad).getX(stick) else 0f
    }

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) nativeGet(gamepads, gamepad).getY(stick) else 0f
    }

    override fun setCursorPosition(x: Int, y: Int) {
        // no-op
    }

    override fun lockCursor() {
        nativeLockCursor(canvas)
    }

    override fun releaseCursor() {
        nativeReleaseCursor(canvas)
    }

    override fun addInputProcessor(processor: InputProcessor) {
        _inputProcessors += processor
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        _inputProcessors -= processor
    }

    override fun showSoftKeyboard() = Unit

    override fun hideSoftKeyboard() = Unit
}

expect fun createGamepads(): Array<GamepadInfo>

expect fun createTouchIdentifiers(): IntArray

external interface DocumentWithPointerLock : JsAny {
    val pointerLockElement: Element?
}

expect fun nativeLockCursor(canvas: HTMLCanvasElement)

expect fun nativeReleaseCursor(canvas: HTMLCanvasElement)
expect fun nativeCheckForGamepads(gamepads: Array<GamepadInfo>)
expect fun nativeUpdateGamepads(gamepads: Array<GamepadInfo>, inputCache: InputCache)

external interface JsGamepadButton : JsAny {
    val value: Double
    val pressed: Boolean
}

external interface JsGamePad {
    val axes: JsArray<JsNumber>
    val buttons: JsArray<JsGamepadButton>
    val connected: Boolean
    val id: String
    val index: Int
    val mapping: String
    val timestamp: Float
}

@JsName("GamepadEvent")
private external interface JsGamepadEvent : JsAny {
    val gamepad: JsGamePad
}

private external interface ExtendedMouseEvent : UnionElementOrMouseEvent, JsAny {
    val screenX: Int
    val screenY: Int
    val clientX: Int
    val clientY: Int
    val ctrlKey: Boolean
    val shiftKey: Boolean
    val altKey: Boolean
    val metaKey: Boolean
    val button: Short
    val buttons: Short
    val relatedTarget: EventTarget?
    val region: String?
    val pageX: Double
    val pageY: Double
    val x: Double
    val y: Double
    val offsetX: Double
    val offsetY: Double
    val movementX: Double
    val movementY: Double
    fun getModifierState(keyArg: String): Boolean

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}