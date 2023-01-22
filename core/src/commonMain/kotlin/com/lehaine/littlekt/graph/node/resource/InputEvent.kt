package com.lehaine.littlekt.graph.node.resource

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

    /**
     * The x-coord of the root scene canvas. Changing this will also set [canvasX] automatically.
     */
    var sceneX: Float = 0f
        set(value) {
            field = value
            canvasX = value
        }

    /**
     * The y-coord of the root scene canvas. Changing this will also set [canvasY] automatically.
     */
    var sceneY: Float = 0f
        set(value) {
            field = value
            canvasY = value
        }

    /**
     * The x-coord of the current canvas. This may be equal to [sceneX] if there are no nested canvases.
     */
    var canvasX: Float = 0f

    /**
     * The y-coord of the current canvas. This may be equal to [sceneY] if there are no nested canvases.
     */
    var canvasY: Float = 0f

    /**
     * The x-coord of the current node in its local coordinates. (i.e a value of `0` will be the left edge)
     */
    var localX: Float = 0f

    /**
     * The y-coord of the current node in its local coordinates. (i.e a value of `0` will be the top edge)
     */
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
        canvasX = 0f
        canvasY = 0f
        localX = 0f
        localY = 0f
        scrollAmountX = 0f
        scrollAmountY = 0f
        inputType = null
    }

    override fun toString(): String {
        return "InputEvent(type=$type, pointer=$pointer, button=$button, key=$key, inputType=$inputType, char=$char, sceneX=$sceneX, sceneY=$sceneY, canvasX=$canvasX, canvasY=$canvasY, localX=$localX, localY=$localY, scrollAmountX=$scrollAmountX, scrollAmountY=$scrollAmountY)"
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