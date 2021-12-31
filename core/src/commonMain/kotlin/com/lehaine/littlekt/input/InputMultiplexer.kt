package com.lehaine.littlekt.input

import com.lehaine.littlekt.util.fastForEach
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

/**
 * @author Colt Daily
 * @date 12/31/21
 */
class InputMultiplexer<InputSignal>(val input: Input) : InputProcessor {

    var axisDeadZone = 0.3f
    var mode = InputMode.KEYBOARD

    private val keyBindings = mutableMapOf<InputSignal, List<Key>>()
    private val buttonBindings = mutableMapOf<InputSignal, List<GameButton>>()
    private val axisBindings = mutableMapOf<InputSignal, List<GameAxis>>()

    private val axes = mutableMapOf<InputSignal, InputAxis<InputSignal>>()
    private val vectors = mutableMapOf<InputSignal, InputVector<InputSignal>>()

    enum class InputMode {
        KEYBOARD, GAMEPAD
    }

    /**
     * Create a binding of multiple keys and buttons into a single [InputSignal].
     * @param type the [InputSignal] that is triggered by one of the keys or buttons
     * @param keys a list of [Key] types that triggers the [InputSignal]
     * @param buttons a list of [GameButton] types that triggers the [InputSignal]
     * @param axes a list of [GameAxis] types that triggers the [InputSignal]
     * @see [Key]
     * @see [GameButton]
     */
    fun addBinding(
        type: InputSignal, keys: List<Key> = emptyList(), buttons: List<GameButton> = emptyList(), axes: List<GameAxis>
    ) {
        keyBindings[type] = keys.toList()
        buttonBindings[type] = buttons.toList()
    }

    /**
     * Create an axis from two [InputSignal] bindings. In order to have a proper axis, the two signals must have
     * been bound in [addBinding]. The axis is calculated as follows: `positive - negative`.
     * Note: this will replace any existing axis if it exists.
     *
     * @param type the [InputSignal] that refers to this axis.
     * @param positive the positive [InputSignal] of this axis.
     * @param negative the negative [InputSignal] of this axis.
     * @see [addBinding]
     */
    fun addAxis(type: InputSignal, positive: InputSignal, negative: InputSignal) {
        axes[type] = InputAxis(positive, negative)
    }

    /**
     * Create a vector from four [InputSignal] bindings. Think of it as two axes in as a single result.
     * In order to have a proper vector, the four signals must have been bound in [addBinding].
     * The vector is calculated as follows: `positiveX - negativeX` and `positiveY - negativeY`.
     * Note: this will replace any existing vector if it exists.
     *
     * @param type the [InputSignal] that refers to this axis.
     * @param positiveX the positive `X` axis [InputSignal] of this vector.
     * @param positiveY the positive `Y` axis [InputSignal] of this vector.
     * @param negativeX the negative `X` axis [InputSignal] of this vector.
     * @param negativeY the negative `Y` axis [InputSignal] of this vector.
     * @see [addBinding]
     */
    fun addVector(
        type: InputSignal,
        positiveX: InputSignal,
        positiveY: InputSignal,
        negativeX: InputSignal,
        negativeY: InputSignal
    ) {
        vectors[type] = InputVector(positiveX, positiveY, negativeX, negativeY)
    }


    override fun keyDown(key: Key): Boolean {
        return super.keyDown(key)
    }

    override fun keyUp(key: Key): Boolean {
        return super.keyUp(key)
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        return super.touchDown(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        return super.touchUp(screenX, screenY, pointer)
    }

    override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        return super.gamepadButtonPressed(button, pressure, gamepad)
    }

    override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
        return super.gamepadButtonReleased(button, gamepad)
    }

    private inline fun onButtonEvent(
        type: InputSignal, predicate: (strength: Float, isAxis: Boolean) -> Boolean
    ): Boolean {
        if (input.connectedGamepads.isNotEmpty()) {
            input.gamepads.fastForEach { gamepad ->
                buttonBindings[type]?.fastForEach {
                    if (predicate(gamepad[it], false)) {
                        return true
                    }
                }
                axisBindings[type]?.fastForEach {
                    if (predicate(gamepad[it], true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private inline fun getButtonStrength(
        type: InputSignal, predicate: (strength: Float, isAxis: Boolean) -> Boolean
    ): Float {
        if (input.connectedGamepads.isNotEmpty()) {
            input.gamepads.fastForEach { gamepad ->
                buttonBindings[type]?.fastForEach {
                    if (predicate(gamepad[it], false)) {
                        return gamepad[it]
                    }
                }
                axisBindings[type]?.fastForEach {
                    if (predicate(gamepad[it], true)) {
                        return if (it == GameAxis.LY || it == GameAxis.RY) {
                            -gamepad[it]
                        } else {
                            gamepad[it]
                        }
                    }
                }
            }
        }
        return 0f
    }

    private inline fun getKeyStrength(type: InputSignal, predicate: (Key) -> Boolean): Float {
        keyBindings[type]?.fastForEach {
            if (predicate(it)) {
                return 1f
            }
        }
        return 0f
    }

    private inline fun getKeyEvent(type: InputSignal, predicate: (Key) -> Boolean): Boolean {
        keyBindings[type]?.fastForEach {
            if (predicate(it)) {
                return true
            }
        }
        return false
    }

    fun strength(type: InputSignal, deadZone: Float = axisDeadZone): Float {
        return if (mode == InputMode.KEYBOARD) {
            getKeyStrength(type) { input.isKeyPressed(it) }
        } else {
            getButtonStrength(type) { strength, isAxis ->
                (isAxis && abs(strength) >= deadZone) || !isAxis && strength != 0f
            }
        }
    }

    fun dist(type: InputSignal, deadZone: Float = axisDeadZone) = abs(strength(type, deadZone))

    fun angle(xAxes: InputSignal, yAxes: InputSignal, deadZone: Float = axisDeadZone) =
        atan2(strength(yAxes, deadZone), strength(xAxes, deadZone))

    fun dist(xAxes: InputSignal, yAxes: InputSignal, deadZone: Float = axisDeadZone) =
        max(abs(strength(xAxes, deadZone)), abs(strength(yAxes, deadZone)))

}

private data class InputAxis<T>(val positive: T, val negative: T)

private data class InputVector<T>(val positiveX: T, val positiveY: T, val negativeX: T, val negativeY: T)