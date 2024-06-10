package com.littlekt.graph.node.ui

import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.util.Signal
import com.littlekt.graph.util.SingleSignal
import kotlin.js.JsName

/**
 * The abstract base for different buttons. Other types of buttons inherit from this.
 *
 * @author Colton Daily
 * @date 1/17/2022
 */
abstract class BaseButton : Control() {

    private val status = Status()
    private var _toggleMode = false
    private var buttonGroup: ButtonGroup = ButtonGroup()
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
                    DrawMode.PRESSED
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
    val pressing: Boolean
        get() = status.pressAttempt

    @JsName("isPressed")
    var pressed: Boolean
        get() = if (_toggleMode) status.pressed else status.pressAttempt
        set(value) {
            if (!_toggleMode) return
            if (status.pressed == value) return
            status.pressed = value

            if (value) {
                unpressGroup()
            }
            _toggled(status.pressed)
        }

    val hovered: Boolean
        get() = status.hovering

    var disabled: Boolean
        get() = status.disabled
        set(value) {
            if (status.disabled == value) return
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
    val onToggled: SingleSignal<Boolean> = SingleSignal()
    val onButtonDown: Signal = Signal()
    val onButtonUp: Signal = Signal()

    override fun onRemovedFromScene() {
        super.onRemovedFromScene()
        if (!toggleMode) {
            status.pressed = false
        }
        status.hovering = false
        status.pressAttempt = false
        status.pressingInside = false
    }

    override fun onFocusLost() {
        super.onFocusLost()
        status.pressAttempt = false
        status.pressingInside = false
    }

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)

        if (event.type == InputEvent.Type.MOUSE_ENTER) {
            status.hovering = true
        }
        if (event.type == InputEvent.Type.MOUSE_EXIT) {
            status.hovering = false
            status.pressAttempt = false
            status.pressingInside = false
            event.handle()
        }

        val uiAccept = scene?.uiInputSignals?.uiAccept
        if (
            event.type == InputEvent.Type.TOUCH_DOWN ||
                event.type == InputEvent.Type.ACTION_DOWN && event.inputType == uiAccept
        ) {
            status.pressAttempt = true
            status.pressingInside = true
            onButtonDown.emit()
            event.handle()
        }

        if (status.pressAttempt && status.pressingInside) {
            if (_toggleMode) {
                val isPressed =
                    event.type == InputEvent.Type.TOUCH_DOWN ||
                        event.type == InputEvent.Type.ACTION_DOWN && event.inputType == uiAccept
                // TODO check if shortcut input then: isPressed = false
                if (
                    (isPressed && actionMode == ActionMode.BUTTON_PRESS) ||
                        (!isPressed && actionMode == ActionMode.BUTTON_RELEASE)
                ) {
                    if (actionMode == ActionMode.BUTTON_PRESS) {
                        status.pressAttempt = false
                        status.pressingInside = false
                    }
                    status.pressed = !status.pressed
                    unpressGroup()
                    _toggled(status.pressed)
                    _pressed()
                }
            } else if (
                ((event.type == InputEvent.Type.TOUCH_DOWN ||
                    event.type == InputEvent.Type.ACTION_DOWN && event.inputType == uiAccept) &&
                    actionMode == ActionMode.BUTTON_PRESS) ||
                    ((event.type == InputEvent.Type.TOUCH_UP ||
                        event.type == InputEvent.Type.ACTION_UP && event.inputType == uiAccept) &&
                        actionMode == ActionMode.BUTTON_RELEASE)
            ) {
                _pressed()
            }
        }

        if (
            event.type == InputEvent.Type.TOUCH_UP ||
                event.type == InputEvent.Type.ACTION_UP && event.inputType == uiAccept
        ) {
            if (
                event.type == InputEvent.Type.ACTION_UP ||
                    event.type == InputEvent.Type.TOUCH_UP &&
                        !hasPoint(event.canvasX, event.canvasY)
            ) {
                status.hovering = false
            }
            status.pressAttempt = false
            status.pressingInside = false
            onButtonUp.emit()
        }
    }

    /** Assigns the [BaseButton] to the [ButtonGroup] and removes itself from its previous. */
    fun setButtonGroup(group: ButtonGroup) {
        buttonGroup.buttons -= this
        buttonGroup = group
        buttonGroup.buttons += this
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

    override fun onDestroy() {
        super.onDestroy()
        onButtonDown.clear()
        onButtonUp.clear()
        onPressed.clear()
        onToggled.clear()
    }

    enum class DrawMode {
        NORMAL,
        PRESSED,
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
    internal val buttons = mutableSetOf<BaseButton>()
}
