package com.littlekt.resources

import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.resources.internal.InternalResources
import com.littlekt.resources.internal.roundedRectCorner as internalRoundedRectCorner

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

    /**
     * A white anti-aliased rounded rect [TextureSlice] meant to be drawn as a [NinePatch] with
     * [roundedRectCorner] on each side. When [border] is greater than `0`, only the border is
     * drawn. Common sizes are part of the [atlas]. Other sizes create a standalone texture once.
     *
     * @param radius the corner radius in pixels
     * @param border the border width in pixels or `0` to fill
     */
    fun roundedRect(radius: Int, border: Int = 0): TextureSlice =
        InternalResources.INSTANCE.roundedRect(radius, border)

    /** The size of each side of the [NinePatch] used to draw a [roundedRect]. */
    fun roundedRectCorner(radius: Int, border: Int = 0): Int =
        internalRoundedRectCorner(radius, border)
}
