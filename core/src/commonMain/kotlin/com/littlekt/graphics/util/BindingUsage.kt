package com.littlekt.graphics.util

import kotlin.jvm.JvmInline

/**
 * @author Colton Daily
 * @date 1/14/2025
 */
@JvmInline
value class BindingUsage(val usage: Int) {
    companion object {
        val CAMERA = BindingUsage(0)
        val TEXTURE = BindingUsage(1)
    }
}
