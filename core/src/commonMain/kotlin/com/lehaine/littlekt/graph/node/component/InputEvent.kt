package com.lehaine.littlekt.graph.node.component

import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class InputEvent<InputSignal> : Event() {
    var type: Type = Type.NONE
    var pointer: Pointer = Pointer.POINTER1
    var button: Int = 0
    var key: Key = Key.ANY_KEY
    var inputType: InputSignal? = null
    var char: Char = Char.MIN_VALUE
    var sceneX: Float = 0f
    var sceneY: Float = 0f
    var localX: Float = 0f
    var localY: Float = 0f
    var scrollAmountX: Float = 0f
    var scrollAmountY: Float = 0f

    override fun reset() {
        super.reset()
        button = -1
        key = Key.ANY_KEY
        char = Char.MIN_VALUE
        sceneX = 0f
        sceneY = 0f
        localX = 0f
        localY = 0f
        scrollAmountX = 0f
        scrollAmountY = 0f
        inputType = null
    }

    override fun toString(): String {
        return "InputEvent(type=$type, pointer=$pointer, button=$button, keyCode=$key, sceneX=$sceneX, sceneY=$sceneY, scrollAmountX=$scrollAmountX, scrollAmountY=$scrollAmountY)"
    }

    enum class Type {
        NONE,

        /**
         * A new touch for a pointer on the scene was detected
         */
        TOUCH_DOWN,

        /**
         * A pointer has stopped touching the scene.
         */
        TOUCH_UP,

        /**
         * A pointer that is touching the scene has moved.
         */
        TOUCH_DRAGGED,

        /**
         * The mouse pointer has moved (without a mouse button being active).
         */
        MOUSE_HOVER,

        /**
         * The mouse pointer or an active touch have entered (i.e., [hit][Cotnrol.hit]) a Control.
         */
        MOUSE_ENTER,

        /**
         * The mouse pointer or an active touch have exited a Control.
         */
        MOUSE_EXIT,

        /**
         * The mouse scroll wheel has changed.
         */
        SCROLLED,

        /**
         * A keyboard key has been pressed.
         */
        KEY_DOWN,

        /**
         * A keyboard key has been released.
         */
        KEY_UP,

        /**
         * A keyboard key has been pressed and not released.
         */
        KEY_REPEAT,

        /**
         * A keyboard character has been typed.
         */
        CHAR_TYPED,

        /**
         * The input type action that has been pressed.
         */
        ACTION_DOWN,

        /**
         * The input type action that has been pressed and not released.
         */
        ACTION_REPEAT,

        /**
         * The input type action that has been released.
         */
        ACTION_UP
    }
}