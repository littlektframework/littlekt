package com.lehaine.littlekt.graphics.gl

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlBuffer(val address: Int) {
    val bufferId = nextBufferId++

    companion object {
        private var nextBufferId = 1L
    }
}