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


    /**
     * The width of the combined atlas that is generated for gpu font cache.
     */
    val atlasWidth: Int get() = InternalResources.INSTANCE.atlasHeight

    /**
     * The height of the combined atlas the is generated for gpu font cache. The bezier and the grid will each share
     * half of this total. The grid will take the bottom half while the beziers will take the top.
     */
    val atlasHeight: Int get() = InternalResources.INSTANCE.atlasHeight

    /**
     * Resize the internal texture that is used by a gpu font cache to the new size and copies over any data from the previous texture to
     * the newly generated texture.
     *
     * **Warning**: This will dispose of the previous texture and generate a new one!
     * @see [com.lehaine.littlekt.graphics.font.GpuFontCache]
     */
    fun setGpuFontAtlasSize(width: Int, height: Int) = InternalResources.INSTANCE.setGpuFontAtlasSize(width, height)
}