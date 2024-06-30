package com.littlekt.graph.node.ui

import com.littlekt.graph.util.signal1v
import com.littlekt.math.clamp
import com.littlekt.math.ife
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 2/6/2022
 */
abstract class Range : Control() {

    /** Signal that is emitted when [value] is changed. */
    val onValueChanged = signal1v<Float>()

    private var _min = 0f

    /** The minimum value that [value] can be. Ranged is clamped if less than [min]. */
    var min: Float
        get() = _min
        set(value) {
            if (_min == value) return
            _min = value
            updateValue(_value)
            validateValues()
        }

    private var _max = 100f

    /** The maximum value that [value] can be. Ranged is clamped if greater than [max]. */
    var max: Float
        get() = _max
        set(value) {
            if (_max == value) return
            _max = value
            updateValue(_value)
            validateValues()
        }

    /** If greater than 0, [value] will always be rounded to a multiple of [step]. */
    var step: Float = 1f

    private var _page: Float = 0f

    /**
     * The page size. Used for [ScrollBar]. The ScrollBar's length is its size multiplied by [page]
     * over the different between [min] and [max].
     */
    var page: Float
        get() = _page
        set(value) {
            if (_page == value) return
            _page = value
            updateValue(_value)
            validateValues()
        }

    /** If `true`, [value] will always be rounded to the nearest integer. */
    var rounded: Boolean = false

    /** If `true`, [value] may be greater than [max]. */
    var allowGreater: Boolean = false

    /** If `true`, [value] may be less than [min]. */
    var allowLesser: Boolean = false

    private var _value = 0f

    /** The range's current value. */
    var value: Float
        get() = _value
        set(value) {
            updateValue(value)
        }

    /** The ratio of [value] mapped between 0 and 1. */
    var ratio: Float
        get() {
            // avoid division by zero
            if (max ife min) return 1f
            val result = value.clamp(min, max)
            return ((result - min) / max - min).clamp(0f, 1f)
        }
        set(value) {
            var v: Float
            val percent = (max - min) * value
            v =
                if (step > 0f) {
                    val steps = (percent / step).roundToInt().toFloat()
                    steps * step + min
                } else {
                    percent + min
                }
            v = v.clamp(min, max)
            updateValue(v)
        }

    private fun updateValue(value: Float) {
        var newValue = value
        if (step > 0) {
            newValue = (value / step).roundToInt() * step
        }
        if (rounded) {
            newValue = newValue.roundToInt().toFloat()
        }
        if (!allowGreater && newValue > max - page) {
            newValue = max - page
        }
        if (!allowLesser && newValue < min) {
            newValue = min
        }
        if (newValue == _value) return
        _value = newValue
        onValueChanged.emit(newValue)
    }

    private fun validateValues() {
        _max = max(_max, _min)
        _page = _page.clamp(0f, _max - _min)
    }

    override fun onDestroy() {
        super.onDestroy()
        onValueChanged.clear()
    }
}
