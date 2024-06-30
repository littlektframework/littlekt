package com.littlekt.input

import com.littlekt.math.geom.Point
import com.littlekt.util.datastructure.fastForEach
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

/**
 * @author Colton Daily
 * @date 11/22/2021
 */
class JsInput(val canvas: HTMLCanvasElement) : Input {
    private val inputCache = InputCache()
    private var mouseX = 0f
    private var mouseY = 0f
    private var logicalMouseX = 0f
    private var logicalMouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private val touchedPointers = mutableListOf<Pointer>()

    private val _inputProcessors = mutableListOf<InputProcessor>()
    override val inputProcessors: List<InputProcessor>
        get() = _inputProcessors

    override val gamepads: Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

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
            Key.ARROW_RIGHT
        )

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
                gamepads[ge.gamepad.index].connected = true
                _connectedGamepads += gamepads[ge.gamepad.index]
            }
        )
        window.addEventListener(
            "gamepaddisconnected",
            { e ->
                val ge = e.unsafeCast<JsGamepadEvent>()
                gamepads[ge.gamepad.index].connected = false
                _connectedGamepads -= gamepads[ge.gamepad.index]
            }
        )
        checkForGamepads()
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
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputCache.onTouchDown(x, y, it.getPointer)
        }
    }

    private fun touchMove(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputCache.onMove(x, y, touchedPointers.lastOrNull() ?: Pointer.POINTER1)
        }
    }

    private fun touchEnd(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputCache.onTouchUp(x, y, it.getPointer)
        }
    }

    private fun mouseDown(event: Event) {
        event as MouseEvent
        val rect = canvas.getBoundingClientRect()
        val x = event.clientX.toFloat() - rect.left.toFloat()
        val y = event.clientY.toFloat() - rect.top.toFloat()
        inputCache.onTouchDown(x, y, event.button.getPointer)
        touchedPointers += event.button.getPointer
    }

    private fun mouseUp(event: Event) {
        event as MouseEvent
        val rect = canvas.getBoundingClientRect()
        val x = event.clientX.toFloat() - rect.left.toFloat()
        val y = event.clientY.toFloat() - rect.top.toFloat()
        inputCache.onTouchUp(x, y, event.button.getPointer)
        touchedPointers -= event.button.getPointer
    }

    private fun mouseMove(event: Event) {
        event as MouseEvent
        val rect = canvas.getBoundingClientRect()
        val x = event.clientX.toFloat() - rect.left.toFloat()
        val y = event.clientY.toFloat() - rect.top.toFloat()
        _deltaX = x - logicalMouseX
        _deltaY = y - logicalMouseY
        mouseX = x
        mouseY = y
        logicalMouseX = mouseX
        logicalMouseY = mouseY

        inputCache.onMove(mouseX, mouseY, touchedPointers.lastOrNull() ?: Pointer.POINTER1)
    }

    private fun scroll(event: Event) {
        event as WheelEvent
        event.preventDefault()
        inputCache.onScroll(-event.deltaX.toFloat(), -event.deltaY.toFloat())
    }

    fun update() {
        updateGamepads()
        inputCache.processEvents(inputProcessors)
    }

    fun reset() {
        inputCache.reset()
        _deltaX = 0f
        _deltaY = 0f
    }

    private fun checkForGamepads() {
        try {
            if (navigator.getGamepads != null) {
                val jsGamepads = navigator.getGamepads().unsafeCast<JsArray<JsGamePad?>>()
                gamepads.fastForEach { it.connected = false }

                for (gamepadId in 0 until jsGamepads.length) {
                    val controller = jsGamepads[gamepadId] ?: continue
                    val gamepad = gamepads.getOrNull(gamepadId) ?: continue
                    gamepad.apply {
                        this.connected = controller.connected
                        this.name = controller.id
                    }
                }
            }
        } catch (e: dynamic) {
            console.error(e)
        }
    }

    private fun updateGamepads() {
        if (connectedGamepads.isEmpty()) return

        try {
            if (navigator.getGamepads != null) {
                val jsGamepads = navigator.getGamepads().unsafeCast<JsArray<JsGamePad?>>()

                for (gamepadId in 0 until jsGamepads.length) {
                    val controller = jsGamepads[gamepadId] ?: continue
                    val gamepad = gamepads.getOrNull(gamepadId) ?: continue
                    gamepad.apply {
                        for (n in 0 until controller.buttons.length) {
                            val button = controller.buttons[n]
                            this.rawButtonsPressed[n] = button.value
                        }
                        inputCache.updateGamepadTrigger(GameButton.L2, gamepad)
                        inputCache.updateGamepadTrigger(GameButton.R2, gamepad)
                        inputCache.updateGamepadButtons(gamepad)

                        for (n in 0 until controller.axes.length) {
                            this.rawAxes[n] = controller.axes[n]
                        }
                        inputCache.updateGamepadStick(GameStick.LEFT, gamepad)
                        inputCache.updateGamepadStick(GameStick.RIGHT, gamepad)
                    }
                }
            }
        } catch (e: dynamic) {
            console.error(e)
        }
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
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][button] else 0f
    }

    override fun getGamepadJoystickDistance(stick: GameStick, gamepad: Int): Point {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad][stick] else Point.ZERO
    }

    override fun getGamepadJoystickXDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getX(stick) else 0f
    }

    override fun getGamepadJoystickYDistance(stick: GameStick, gamepad: Int): Float {
        return if (connectedGamepads.isNotEmpty()) gamepads[gamepad].getY(stick) else 0f
    }

    override fun setCursorPosition(x: Int, y: Int) {
        TODO("Not yet implemented")
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

private external val navigator: dynamic

private external interface JsArray<T> {
    val length: Int
}

private inline operator fun <T> JsArray<T>.get(index: Int): T = this.asDynamic()[index]

private external interface JsGamepadButton {
    val value: Float
    val pressed: Boolean
}

private external interface JsGamePad {
    val axes: JsArray<Float>
    val buttons: JsArray<JsGamepadButton>
    val connected: Boolean
    val id: String
    val index: Int
    val mapping: String
    val timestamp: Float
}

@JsName("GamepadEvent")
private external interface JsGamepadEvent {
    val gamepad: JsGamePad
}
