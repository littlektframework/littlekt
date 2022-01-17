package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.TypedSignal
import kotlin.js.JsName

/**
 * The abstract base for different buttons. Other types of buttons inherit from this.
 * @author Colton Daily
 * @date 1/17/2022
 */
abstract class BaseButton : Control() {

    private val status = Status()
    private var _toggleMode = false
    var buttonGroup: ButtonGroup = ButtonGroup()

    val drawMode: DrawMode
        get() {
            if (status.disabled) {
                return DrawMode.DISABLED
            }
            if (!status.pressAttempt && status.hovering) {
                if (status.pressed) {
                    return DrawMode.HOVER_PRESSED
                }
                return DrawMode.HOVER
            } else {
                var pressing: Boolean
                if (status.pressAttempt) {
                    pressing = status.pressingInside
                    if (status.pressed) {
                        pressing = !pressing
                    }
                } else {
                    pressing = status.pressed
                }
                return if (pressing) {
                    DrawMode.PRESSRED
                } else {
                    DrawMode.NORMAL
                }
            }
        }
    var toggleMode: Boolean
        get() = _toggleMode
        set(value) {
            if (!value) {
                pressed = false
            }
            _toggleMode = value
        }
    var actionMode = ActionMode.BUTTON_RELEASE
    val pressing: Boolean get() = status.pressAttempt

    @JsName("isPressed")
    var pressed: Boolean = false
        get() = if (_toggleMode) status.pressed else status.pressAttempt
        set(value) {
            if (!_toggleMode) return
            if (field == value) return
            status.pressed = value

            if (value) {
                unpressGroup()
            }
            _toggled(status.pressed)
        }
    val hovered: Boolean get() = status.hovering
    var disabled: Boolean = false
        get() = status.disabled
        set(value) {
            if (field == value) return
            status.disabled = value
            if (value) {
                if (!_toggleMode) {
                    status.pressed = false
                }
                status.pressAttempt = false
                status.pressingInside = false
            }
        }

    val onPressed: Signal = Signal()
    val onToggled: TypedSignal<Boolean> = TypedSignal()
    val onButtonDown: Signal = Signal()
    val onButtonUp: Signal = Signal()

    override fun _onRemovedFromScene() {
        super._onRemovedFromScene()
        if (!toggleMode) {
            status.pressed = false
        }
        status.hovering = false
        status.pressAttempt = false
        status.pressingInside = false
    }

    override fun uiInput(event: InputEvent) {
        super.uiInput(event)
        if (event.type == InputEvent.Type.MOUSE_ENTER) {
            status.hovering = true
        }
        if (event.type == InputEvent.Type.MOUSE_EXIT) {
            status.hovering = false
        }

        if (event.type == InputEvent.Type.TOUCH_DOWN) {
            status.pressAttempt = true
            status.pressingInside = true
            onButtonDown.emit()
        }

        if (status.pressAttempt && status.pressingInside) {
            if (_toggleMode) {
                val isPressed = event.type == InputEvent.Type.TOUCH_DOWN
                // TODO check if shortcut input then: isPressed = false
                if ((isPressed && actionMode == ActionMode.BUTTON_PRESS) || (!isPressed && actionMode == ActionMode.BUTTON_RELEASE)) {
                    if (actionMode == ActionMode.BUTTON_PRESS) {
                        status.pressAttempt = false
                        status.pressingInside = false
                    }
                    status.pressed = !status.pressed
                    unpressGroup()
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

    private fun unpressGroup() {
        if (toggleMode) {
            status.pressed = true
        }

        buttonGroup.buttons.forEach {
            if (it == this) {
                return@forEach
            }
            it.pressed = false
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

    protected open fun pressed() = Unit

    protected open fun toggled(pressed: Boolean) = Unit

    enum class DrawMode {
        NORMAL,
        PRESSRED,
        HOVER,
        DISABLED,
        HOVER_PRESSED
    }

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

class ButtonGroup {
    val buttons = mutableSetOf<BaseButton>()

}