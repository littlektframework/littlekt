package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.internal.InternalResources

/**
 * An object that contains a bunch of common texture slices that can be used to prevent texture switches.
 */
object Textures {
    val default get() = InternalResources.INSTANCE.default

    val white: TextureSlice get() = InternalResources.INSTANCE.white
    val black: TextureSlice get() = InternalResources.INSTANCE.black
    val red: TextureSlice get() = InternalResources.INSTANCE.red
    val green: TextureSlice get() = InternalResources.INSTANCE.green
    val blue: TextureSlice get() = InternalResources.INSTANCE.blue
    val transparent: TextureSlice get() = InternalResources.INSTANCE.transparent
}