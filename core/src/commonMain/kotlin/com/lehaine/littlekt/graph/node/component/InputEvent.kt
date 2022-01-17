package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class InputEvent : Event() {
    var type: Type = Type.NONE
    var pointer: Pointer = Pointer.POINTER1
    var button: Int = 0
    var keyCode: Int = 0
    var sceneX: Float = 0f
    var sceneY: Float = 0f
    var scrollAmountX: Int = 0
    var scrollAmountY: Int = 0

    override fun reset() {
        super.reset()
        button = -1
    }

    override fun toString(): String {
        return "InputEvent(type=$type, pointer=$pointer, button=$button, keyCode=$keyCode, sceneX=$sceneX, sceneY=$sceneY, scrollAmountX=$scrollAmountX, scrollAmountY=$scrollAmountY)"
    }

    enum class Type {
        NONE,

        /** A new touch for a pointer on the scene was detected  */
        TOUCH_DOWN,

        /** A pointer has stopped touching the scene.  */
        TOUCH_UP,

        /** A pointer that is touching the scene has moved.  */
        TOUCH_DRAGGED,

        /** The mouse pointer has moved (without a mouse button being active).  */
        MOUSE_HOVER,

        /** The mouse pointer or an active touch have entered (i.e., [hit][Cotnrol.hit]) a Control.  */
        MOUSE_ENTER,

        /** The mouse pointer or an active touch have exited a Control.  */
        MOUSE_EXIT,

        /** The mouse scroll wheel has changed.  */
        SCROLLED,

        /** A keyboard key has been pressed.  */
        KEY_DOWN,

        /** A keyboard key has been released.  */
        KEY_UP,

        /** A keyboard key has been pressed and released.  */
        KEY_TYPED
    }
}