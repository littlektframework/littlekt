package com.lehaine.littlekt.input.internal

import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
internal class InternalInputEvent(
    var key: Key = Key.ANY_KEY,
    var typedChar: Char = Char.MIN_VALUE,
    var pointer: Pointer = Pointer.POINTER1,
    var queueTime: Long = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var type: InternalInputEventType = InternalInputEventType.KEY_DOWN
) {
    val isPointerEvent: Boolean
        get() = type == InternalInputEventType.TOUCH_DOWN
                || type == InternalInputEventType.TOUCH_UP
                || type == InternalInputEventType.TOUCH_DRAGGED
}