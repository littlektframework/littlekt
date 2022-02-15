package com.lehaine.littlekt.graphics.gl

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
actual class GlFrameBuffer(val reference: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlFrameBuffer

        if (reference != other.reference) return false

        return true
    }

    override fun hashCode(): Int {
        return reference
    }

    override fun toString(): String {
        return "GlFrameBuffer(reference=$reference)"
    }

}