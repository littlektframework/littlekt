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
value class BindingUsage(val usage: String) {
    companion object {
        val CAMERA = BindingUsage("Camera")
        val TEXTURE = BindingUsage("Texture")
        val MODEL = BindingUsage("Model")
        val MATERIAL = BindingUsage("Material")
        val CLUSTER_BOUNDS = BindingUsage("Cluster Bounds")
        val CLUSTER_LIGHTS = BindingUsage("Cluster Lights")
    }
}
