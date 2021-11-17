package com.lehaine.littlekt.graphics.render

import com.lehaine.littlekt.Percent

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
data class Color(
    var red: Percent = 0.0f,
    var green: Percent = 0.0f,
    var blue: Percent = 0.0f,
    var alpha: Percent = 1.0f
) {
    var r: Percent
        get() = red
        set(value) {
            red = value
        }
    var g: Percent
        get() = green
        set(value) {
            green = value
        }
    var b: Percent
        get() = blue
        set(value) {
            blue = value
        }
    var a: Percent
        get() = alpha
        set(value) {
            alpha = value
        }

    companion object {
        val CLEAR get() = Color(0f, 0f, 0f, 0f)
        val WHITE get() = Color(1f, 1f, 1f, 1f)
    }
}
