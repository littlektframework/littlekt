package com.lehaine.littlekt.input

import kotlinx.browser.document
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
    private val inputManager = InputManager()
    private var mouseX = 0f
    private var mouseY = 0f
    private var logicalMouseX = 0f
    private var logicalMouseY = 0f
    private var _deltaX = 0f
    private var _deltaY = 0f
    private var lastChar: Char = 0.toChar()

    override var inputProcessor: InputProcessor? = null

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
    }

    private fun keyDown(event: Event) {
        event as KeyboardEvent
        inputManager.onKeyDown(event.jsKey)
    }

    private fun keyUp(event: Event) {
        event as KeyboardEvent
        inputManager.onKeyUp(event.jsKey)
    }

    private fun keyPress(event: Event) {
        event as KeyboardEvent
        inputManager.onKeyType(event.charCode.toChar())
    }

    private fun touchStart(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputManager.onTouchDown(x, y, it.getPointer)
        }
    }

    private fun touchMove(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputManager.onMove(x, y, it.getPointer)
        }
    }

    private fun touchEnd(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val touchEvent = event.targetTouches.item(it)!!
            val rect = canvas.getBoundingClientRect()
            val x = touchEvent.clientX.toFloat() - rect.left.toFloat()
            val y = touchEvent.clientY.toFloat() - rect.top.toFloat()
            inputManager.onTouchUp(x, y, it.getPointer)
        }
    }

    private fun mouseDown(event: Event) {
        event as MouseEvent
        val rect = canvas.getBoundingClientRect()
        val x = event.clientX.toFloat() - rect.left.toFloat()
        val y = event.clientY.toFloat() - rect.top.toFloat()
        inputManager.onTouchDown(x, y, event.button.getPointer)
    }

    private fun mouseUp(event: Event) {
        event as MouseEvent
        val rect = canvas.getBoundingClientRect()
        val x = event.clientX.toFloat() - rect.left.toFloat()
        val y = event.clientY.toFloat() - rect.top.toFloat()
        inputManager.onTouchUp(x, y, event.button.getPointer)
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

        // todo handle hdpi mode pixels vs logical

        inputManager.onMove(mouseX, mouseY, Pointer.POINTER1)
    }


    private fun scroll(event: Event) {
        event as WheelEvent
    }

    fun update() {
        inputManager.processEvents(inputProcessor)
    }

    fun reset() {
        inputManager.reset()
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
        get() = inputManager.isTouching
    override val justTouched: Boolean
        get() = inputManager.justTouched
    override val pressure: Float
        get() = getPressure(Pointer.POINTER1)

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

    override fun isTouched(pointer: Pointer): Boolean {
        return inputManager.isTouching(pointer)
    }

    override fun getPressure(pointer: Pointer): Float {
        return if (isTouched(pointer)) 1f else 0f
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return inputManager.isKeyJustPressed(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return inputManager.isKeyPressed(key)
    }

    override fun setCursorPosition(x: Int, y: Int) {
        TODO("Not yet implemented")
    }
}