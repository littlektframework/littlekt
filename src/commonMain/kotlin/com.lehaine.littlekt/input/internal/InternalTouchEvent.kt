package com.lehaine.littlekt.input.internal

import com.lehaine.littlekt.input.KeyCode
import com.lehaine.littlekt.input.TouchSignal
import com.lehaine.littlekt.math.Vector2

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class InternalTouchEvent(
    /**
     * Key code is not null if the event is about a key change
     */
    var keycode: KeyCode? = null,
    /**
     * If the keycode is not null; it's a touch event
     */
    var touchSignal: TouchSignal = TouchSignal.TOUCH1,
    var position: Vector2 = Vector2(0f, 0f),
    var direction: InternalTouchEventDirection = InternalTouchEventDirection.DOWN
) {
    val isTouchEvent: Boolean
        get() = keycode == null
}