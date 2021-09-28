package com.lehaine.littlekt.render

import com.lehaine.littlekt.Percent

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
data class Color(
    var red: Percent = 0.0,
    var green: Percent = 0.0,
    var blue: Percent = 0.0,
    var alpha: Percent = 1.0
)