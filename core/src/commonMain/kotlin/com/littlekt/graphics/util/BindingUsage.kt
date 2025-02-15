package com.littlekt.graphics.util

import kotlin.jvm.JvmInline

/**
 * A value class to mark usages of BindGroupLayouts, mainly in shaders. Internally, it uses a set of
 * strings to do "bitwise" operations on but in reality it only stores strings. So while we can do
 * `|` or `&` operations limit the use as it allocates a new set each time.
 *
 * @author Colton Daily
 * @date 1/14/2025
 */
@JvmInline
value class BindingUsage(val usage: Set<String>) {
    constructor(usage: String) : this(setOf(usage))

    infix fun or(other: BindingUsage): BindingUsage = BindingUsage(usage + other.usage)

    infix fun and(other: BindingUsage): BindingUsage = BindingUsage(usage - other.usage)

    companion object {
        val CAMERA = BindingUsage("Camera")
        val TEXTURE = BindingUsage("Texture")
        val MODEL = BindingUsage("Model")
        val MATERIAL = BindingUsage("Material")
        val SKIN = BindingUsage("Skin")
        val CLUSTER_BOUNDS = BindingUsage("Cluster Bounds")
        val CLUSTER_LIGHTS = BindingUsage("Cluster Lights")
        val LIGHT = BindingUsage("Light")
        val SHADOW = BindingUsage("Shadow")
    }
}
