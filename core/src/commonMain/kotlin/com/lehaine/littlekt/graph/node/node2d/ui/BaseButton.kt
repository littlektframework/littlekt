package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.TypedSignal

/**
 * @author Colton Daily
 * @date 1/17/2022
 */
abstract class BaseButton : Control() {

    private val status = Status()
    private var toggleMode = false
    private val actionMode = ActionMode.BUTTON_RELEASE

    val onPressed: Signal = Signal()
    val onToggled: TypedSignal<Boolean> = TypedSignal()
    val onButtonDown: Signal = Signal()
    val onButtonUp: Signal = Signal()

    override fun uiInput(event: InputEvent) {
        super.uiInput(event)
        if (event.type == InputEvent.Type.TOUCH_DOWN) {
            status.pressAttempt = true
            status.pressingInside = true
            onButtonDown.emit()
        }

        if (status.pressAttempt && status.pressingInside) {
            if (toggleMode) {
                val isPressed = event.type == InputEvent.Type.TOUCH_DOWN
                // TODO check if shortcut input then: isPressed = false
                if ((isPressed && actionMode == ActionMode.BUTTON_PRESS) || (!isPressed && actionMode == ActionMode.BUTTON_RELEASE)) {
                    if (actionMode == ActionMode.BUTTON_PRESS) {
                        status.pressAttempt = false
                        status.pressingInside = false
                    }
                    status.pressed = !status.pressed
                    // TODO _unpressGroup
                    _toggled(status.pressed)
                    _pressed()
                }
            } else if ((event.type == InputEvent.Type.TOUCH_DOWN && actionMode == ActionMode.BUTTON_PRESS)
                || (event.type != InputEvent.Type.TOUCH_DOWN && actionMode == ActionMode.BUTTON_RELEASE)
            ) {
                _pressed()
            }
        }

        if (event.type != InputEvent.Type.TOUCH_DOWN) {
            if (!hasPoint(event.sceneX, event.sceneY)) {
                status.hovering = false
            }
            status.pressAttempt = false
            status.pressingInside = false
            onButtonUp.emit()
        }
    }

    private fun _pressed() {
        pressed()
        onPressed.emit()
    }

    private fun _toggled(pressed: Boolean) {
        toggled(pressed)
        onToggled.emit(pressed)
    }

    open fun pressed() = Unit

    open fun toggled(pressed: Boolean) = Unit

    enum class ActionMode {
        BUTTON_PRESS,
        BUTTON_RELEASE
    }

    private class Status {
        var pressed = false
        var hovering = false
        var pressAttempt = false
        var pressingInside = false
        var disabled = false
    }
}