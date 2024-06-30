package com.littlekt.resources

import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.resources.internal.InternalResources

/**
 * An object that contains a bunch of common texture slices that can be used to prevent texture
 * switches.
 */
object Textures {

    /** A 1x1 white pixel [TextureSlice]. */
    val white: TextureSlice
        get() = InternalResources.INSTANCE.white

    /** A 1x1 transparent pixel [TextureSlice]. */
    val transparent: TextureSlice
        get() = InternalResources.INSTANCE.transparent

    /**
     * The default [TextureAtlas] that contains all the default textures used across **LittleKt**.
     */
    val atlas: TextureAtlas
        get() = InternalResources.INSTANCE.atlas
}
