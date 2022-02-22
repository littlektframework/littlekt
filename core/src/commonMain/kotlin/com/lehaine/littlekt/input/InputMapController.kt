package com.lehaine.littlekt.input

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.util.fastForEach
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

/**
 * An [InputProcessor] that handles [Key], [GameButton], and [GameAxis] inputs and converts them into a single input signal
 * to be used similarly as [Input] except with a custom [InputSignal] type.
 *
 * Bindings can be added to [addBinding] for multiple input sources. These bindings then can be added as an axis or a vector.
 * The input signal can then be checked like [Input] normally would be using input methods this class provides.
 * Additionally, the strength, distances, and angles of each input can be calculated.
 *
 * @param input the current input of the context
 * @param processor an [InputMapProcessor] that reacts to [InputSignal] action events
 * @see addBinding
 * @see addAxis
 * @see addVector
 * @author Colt Daily
 * @date 12/31/21
 */
class InputMapController<InputSignal>(
    private val input: Input,
    private val processor: InputMapProcessor<InputSignal>? = null
) :
    InputProcessor {

    var axisDeadZone = 0.3f
    var mode = InputMode.KEYBOARD

    private val keyBindings = mutableMapOf<InputSignal, List<Key>>()
    private val keyToType = mutableMapOf<Key, InputSignal>()
    private val buttonBindings = mutableMapOf<InputSignal, List<GameButton>>()
    private val buttonToType = mutableMapOf<GameButton, InputSignal>()
    private val axisBindings = mutableMapOf<InputSignal, List<GameAxis>>()
    private val axisToType = mutableMapOf<GameAxis, InputSignal>()

    private val axes = mutableMapOf<InputSignal, InputAxis<InputSignal>>()
    private val vectors = mutableMapOf<InputSignal, InputVector<InputSignal>>()

    private val tempVec2f = MutableVec2f()

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
        type: InputSignal,
        keys: List<Key> = emptyList(),
        buttons: List<GameButton> = emptyList(),
        axes: List<GameAxis> = emptyList()
    ) {
        keyBindings[type] = keys.toList()
        keys.forEach {
            keyToType[it] = type
        }
        buttonBindings[type] = buttons.toList()
        buttons.forEach {
            buttonToType[it] = type
        }
        axisBindings[type] = axes.toList()
        axes.forEach {
            axisToType[it] = type
        }
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

    val isTouching get() = input.isTouching
    val justTouched get() = input.justTouched

    /**
     * Checks to see if the [InputSignal] is currently down for all inputs. This does not trigger for [GameAxis].
     * @return `true` if down; `false` otherwise
     */
    fun down(type: InputSignal): Boolean {
        return if (mode == InputMode.GAMEPAD) {
            getGamepadButtonEvent(type) { input.isGamepadButtonPressed(it) }
        } else {
            getKeyEvent(type) { input.isKeyPressed(it) }
        }
    }

    /**
     * Checks to see if the [InputSignal] is just pressed for all inputs. This does not trigger for [GameAxis].
     * @return `true` if just pressed; `false` otherwise
     */
    fun pressed(type: InputSignal): Boolean {
        return if (mode == InputMode.GAMEPAD) {
            getGamepadButtonEvent(type) { input.isGamepadButtonJustPressed(it) }
        } else {
            getKeyEvent(type) { input.isKeyJustPressed(it) }
        }
    }

    /**
     * Checks to see if the [InputSignal] is just released for all inputs. This does not trigger for [GameAxis].
     * @return `true` if just released; `false` otherwise
     */
    fun released(type: InputSignal): Boolean {
        return if (mode == InputMode.GAMEPAD) {
            getGamepadButtonEvent(type) { input.isGamepadButtonJustReleased(it) }
        } else {
            getKeyEvent(type) { input.isKeyJustPressed(it) }
        }
    }

    override fun charTyped(character: Char): Boolean {
        mode = InputMode.KEYBOARD
        return false
    }

    override fun keyDown(key: Key): Boolean {
        processor ?: return false
        mode = InputMode.KEYBOARD

        keyToType[key]?.let {
            var handled = processor.onActionDown(it)
            if (handled) return true
            if (axisBindings.containsKey(it)) {
                val axis = axis(it)
                handled = processor.onAxisChanged(it, axis)
                if (handled) return true
            }
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        return false
    }

    override fun keyRepeat(key: Key): Boolean {
        mode = InputMode.KEYBOARD
        processor ?: return false

        keyToType[key]?.let {
            var handled = processor.onActionRepeat(it)
            if (handled) return true
            if (axisBindings.containsKey(it)) {
                val axis = axis(it)
                handled = processor.onAxisChanged(it, axis)
                if (handled) return true
            }
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        return false
    }

    override fun keyUp(key: Key): Boolean {
        mode = InputMode.KEYBOARD
        processor ?: return false

        keyToType[key]?.let {
            var handled = processor.onActionUp(it)
            if (handled) return true
            if (axisBindings.containsKey(it)) {
                val axis = axis(it)
                handled = processor.onAxisChanged(it, axis)
                if (handled) return true
            }
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        return false
    }

    override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        mode = InputMode.GAMEPAD
        processor ?: return false

        buttonToType[button]?.let {
            var handled = processor.onActionDown(it)
            if (handled) return true
            if (axisBindings.containsKey(it)) {
                val axis = axis(it)
                handled = processor.onAxisChanged(it, axis)
                if (handled) return true
            }
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        return false
    }

    override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
        mode = InputMode.GAMEPAD
        processor ?: return false

        buttonToType[button]?.let {
            var handled = processor.onActionUp(it)
            if (handled) return true
            if (axisBindings.containsKey(it)) {
                val axis = axis(it)
                handled = processor.onAxisChanged(it, axis)
                if (handled) return true
            }
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        return false
    }

    override fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int): Boolean {
        mode = InputMode.GAMEPAD
        processor ?: return false

        var gameAxis = if (stick == GameStick.LEFT) GameAxis.LX else GameAxis.RX
        axisToType[gameAxis]?.let {
            val axis = axis(it)
            var handled = processor.onAxisChanged(it, axis)
            if (handled) return true
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                handled = processor.onVectorChanged(it, vector.x, vector.y)
                if (handled) return true
            }
        }
        gameAxis = if (stick == GameStick.LEFT) GameAxis.LY else GameAxis.RY
        axisToType[gameAxis]?.let {
            val axis = axis(it)
            var hanndled = processor.onAxisChanged(it, axis)
            if (hanndled) return true
            if (vectors.containsKey(it)) {
                val vector = vector(it)
                hanndled = processor.onVectorChanged(it, vector.x, vector.y)
                if (hanndled) return true
            }
        }
        return false
    }

    override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        mode = InputMode.GAMEPAD
        processor ?: return false

        buttonToType[button]?.let {
            return processor.onActionChange(it, pressure)
        }
        return false
    }

    private inline fun getGamepadButtonEvent(
        type: InputSignal,
        predicate: (button: GameButton) -> Boolean
    ): Boolean {
        if (input.connectedGamepads.isNotEmpty()) {
            input.gamepads.fastForEach {
                buttonBindings[type]?.fastForEach {
                    if (predicate(it)) {
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

    private inline fun getButtonAxisStrength(
        type: InputSignal, positive: Boolean, predicate: (strength: Float, isAxis: Boolean) -> Boolean
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
                            val result = -gamepad[it]
                            if (result > 0f && positive) {
                                -result
                            } else if (result < 0f && !positive) {
                                result
                            } else {
                                0f
                            }
                        } else {
                            val result = gamepad[it]
                            if (result > 0f && positive) {
                                result
                            } else if (result < 0f && !positive) {
                                -result
                            } else {
                                0f
                            }
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

    private fun strengthAxis(positive: InputSignal, negative: InputSignal, deadZone: Float): Float {
        return if (mode == InputMode.KEYBOARD) {
            strength(positive, deadZone) - strength(negative, deadZone)
        } else {
            val predicate = { strength: Float, isAxis: Boolean ->
                (isAxis && abs(strength) >= deadZone) || !isAxis && strength != 0f
            }
            getButtonAxisStrength(positive, true, predicate) - getButtonAxisStrength(positive, false, predicate)
        }
    }

    /**
     * Returns the strength of this [InputSignal]. [GameButton] and [Key] will return as either `-1`, `0`, or `1`.
     * A [GameAxis] will return anything between `-1` to `1`.
     * @param type the [InputSignal] strength to check
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return a value between `-1` to `1`
     * @see Key
     * @see GameButton
     * @see GameAxis
     * @see axisDeadZone
     */
    fun strength(type: InputSignal, deadZone: Float = axisDeadZone): Float {
        return if (mode == InputMode.KEYBOARD) {
            getKeyStrength(type) { input.isKeyPressed(it) }
        } else {
            getButtonStrength(type) { strength, isAxis ->
                (isAxis && abs(strength) >= deadZone) || !isAxis && strength != 0f
            }
        }
    }

    /**
     * Returns the strength of this [InputSignal] as an axis. This will take the positive axis and subtract it by
     * the negative axis. This is the same as doing `strength(positive) - strength(negative)`.
     * Requires an axis to have been added with the specified [type].
     *
     * **Note:** This does take into account that a single [GameAxis] can return negative and positive values
     * which is no different than just using [strength] for the [GameAxis].
     * @param type the [InputSignal] strength to check
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return a value between `-1` to `1`
     * @see addAxis
     * @see strength
     * @see Key
     * @see GameButton
     * @see GameAxis
     * @see axisDeadZone
     */
    fun axis(type: InputSignal, deadZone: Float = axisDeadZone): Float {
        return axes[type]?.let {
            strengthAxis(it.positive, it.negative, deadZone)
        } ?: 0f
    }

    /**
     * Returns the strength of this [InputSignal] as a vector. This will take the positive **X** and **Y** axes and subtract it by
     * the negative **X** and **Y** axes. Requires a vector to have been added with the specified [type].
     *
     * **Note:** This is the same as using [axis] for both **X** and **Y** axes and setting them to a vector.
     * @param type the [InputSignal] strength to check
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return a [Vec2f] with [Vec2f.x] and [Vec2f.y] set with values between `-1` to `1`
     * @see addVector
     * @see axis
     * @see Key
     * @see GameButton
     * @see GameAxis
     * @see axisDeadZone
     */
    fun vector(type: InputSignal, deadZone: Float = axisDeadZone): Vec2f {
        return vectors[type]?.let {
            tempVec2f.set(
                strengthAxis(it.positiveX, it.negativeX, deadZone),
                strengthAxis(it.positiveY, it.negativeY, deadZone)
            )
        } ?: tempVec2f.set(0f, 0f)
    }

    /**
     * Takes the absolute value of the [strength] of this [InputSignal]
     * @param type the input signal
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return the calculated distance
     */
    fun dist(type: InputSignal, deadZone: Float = axisDeadZone): Float = abs(strength(type, deadZone))

    /**
     * Calculates the distance between each axes in this vector [InputSignal] and returns the highest distance.
     * @param vector the vector input signal
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return the highest distance calculated between both axes in the vector
     * @see dist
     */
    fun distV(vector: InputSignal, deadZone: Float = axisDeadZone): Float =
        vectors[vector]?.let {
            val vec = vector(vector, deadZone)
            max(abs(vec.x), abs(vec.y))
        } ?: 0f

    /**
     * Calculates the distance between each axes [InputSignal] and returns the highest distance.
     * @return the highest distance calculated between both axes
     * @see dist
     */
    fun dist(xAxis: InputSignal, yAxis: InputSignal, deadZone: Float = axisDeadZone): Float =
        max(abs(axis(xAxis, deadZone)), abs(axis(yAxis, deadZone)))

    /**
     * Calculates the angle of a vector [InputSignal]. Requires a vector to have been added with the specified [vector].
     * @param vector the input signal
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return the angle between both axes in the vector
     * @see addVector
     */
    fun angle(vector: InputSignal, deadZone: Float = axisDeadZone): Float =
        vectors[vector]?.let {
            val vec = vector(vector, deadZone)
            atan2(vec.y, vec.x)
        } ?: 0f

    /**
     * Calculates the angle between both [InputSignal] axes. Requires the axes to have been added with the specified
     * [axis] and [yAxis].
     * @param xAxis the **X** axis input signal
     * @param yAxis the **Y** axis input signal
     * @param deadZone the threshold a [GameAxis] needs to surpass in order to return a value other than `0`.
     * @return the angle between both axes in the vector
     * @see addVector
     */
    fun angle(xAxis: InputSignal, yAxis: InputSignal, deadZone: Float = axisDeadZone) =
        atan2(axis(yAxis, deadZone), axis(xAxis, deadZone))


}

private data class InputAxis<T>(val positive: T, val negative: T)

private data class InputVector<T>(val positiveX: T, val positiveY: T, val negativeX: T, val negativeY: T)