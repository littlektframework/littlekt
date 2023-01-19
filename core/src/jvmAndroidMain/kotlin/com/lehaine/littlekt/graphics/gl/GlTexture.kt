package com.lehaine.littlekt.graphics.gl

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlTexture(val reference: Int) {
    override fun toString(): String {
        return "GlTexture(reference=$reference)"
    }
}