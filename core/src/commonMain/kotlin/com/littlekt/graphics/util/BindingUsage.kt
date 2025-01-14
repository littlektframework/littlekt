package com.littlekt.graphics.util

import kotlin.jvm.JvmInline

/**
 * A value class to mark usages of BindGroupLayouts, mainly in shaders. [usage] values in the
 * `5000-5999` range should be considered invalid as they are used internally.
 *
 * @author Colton Daily
 * @date 1/14/2025
 */
@JvmInline
value class BindingUsage(val usage: Int) {
    companion object {
        val CAMERA = BindingUsage(5000)
        val TEXTURE = BindingUsage(5001)
        val MODEL = BindingUsage(5002)
        val MATERIAL = BindingUsage(5003)
        val CLUSTER_BOUNDS = BindingUsage(5010)
        val CLUSTER_LIGHTS = BindingUsage(5011)
    }
}
