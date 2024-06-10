package com.littlekt.input

/**
 * @author Colton Daily
 * @date 2/22/2022
 */
interface InputMapProcessor<InputSignal> : InputProcessor {
    fun onActionDown(inputType: InputSignal): Boolean = false

    fun onActionUp(inputType: InputSignal): Boolean = false

    fun onActionRepeat(inputType: InputSignal): Boolean = false

    fun onActionChange(inputType: InputSignal, pressure: Float): Boolean = false
}
