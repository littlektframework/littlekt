package com.littlekt.resources

import com.littlekt.graphics.Texture
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

    /**
     * A [Texture] that is a 1x1 white pixel. This is NOT part of the atlas and is a standalone
     * texture.
     */
    val textureWhite: Texture
        get() = InternalResources.INSTANCE.textureWhite

    /**
     * A [Texture] that is a 1x1 "normal" map pixel. This is NOT part of the atlas and is a
     * standalone texture.
     */
    val textureNormal: Texture
        get() = InternalResources.INSTANCE.textureNormal

    /** A 1x1 transparent pixel [TextureSlice]. */
    val transparent: TextureSlice
        get() = InternalResources.INSTANCE.transparent

    /**
     * The default [TextureAtlas] that contains all the default textures used across **LittleKt**.
     */
    val atlas: TextureAtlas
        get() = InternalResources.INSTANCE.atlas
}
