package com.lehaine.littlekt.input

/**
 * @author Colton Daily
 * @date 2/22/2022
 */
interface InputMapProcessor<InputSignal> {
    fun onActionDown(type: InputSignal): Boolean = false
    fun onActionUp(type: InputSignal): Boolean = false
    fun onActionRepeat(type: InputSignal): Boolean = false
    fun onActionChange(type: InputSignal, pressure: Float): Boolean = false
    fun onAxisChanged(type: InputSignal, axis: Float): Boolean = false
    fun onVectorChanged(type: InputSignal, xAxis: Float, yAxis: Float): Boolean = false
}