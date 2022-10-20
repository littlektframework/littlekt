package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.internal.InternalResources

/**
 * An object that contains a bunch of common texture slices that can be used to prevent texture switches.
 */
object Textures {

    /**
     * A 1x1 white pixel [TextureSlice].
     */
    val white: TextureSlice get() = InternalResources.INSTANCE.white

    /**
     * A 1x1 transparent pixel [TextureSlice].
     */
    val transparent: TextureSlice get() = InternalResources.INSTANCE.transparent

    /**
     * The default [TextureAtlas] that contains all the default textures used across **LittleKt**.
     */
    val atlas: TextureAtlas get() = InternalResources.INSTANCE.atlas
}

/**
 * An object that contains a bunch of common fonts that are used across **LittleKt**.
 */
object Fonts {

    /**
     * The name of the default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 17px`
     */
    val default get() = InternalResources.INSTANCE.defaultFont

}