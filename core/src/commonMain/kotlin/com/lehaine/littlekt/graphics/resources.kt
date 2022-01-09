package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.internal.InternalResources

/**
 * An object that contains a bunch of common texture slices that can be used to prevent texture switches.
 */
object Textures {
    /**
     * A very small [Texture] that contains a bunch of different colors. This texture is already sliced.
     * @see white
     * @see black
     * @see red
     * @see green
     * @see blue
     * @see transparent
     */
    val default get() = InternalResources.INSTANCE.defaultTexture

    /**
     * A 1x1 white pixel [TextureSlice].
     */
    val white: TextureSlice get() = InternalResources.INSTANCE.white

    /**
     * A 1x1 black pixel [TextureSlice].
     */
    val black: TextureSlice get() = InternalResources.INSTANCE.black

    /**
     * A 1x1 red pixel [TextureSlice].
     */
    val red: TextureSlice get() = InternalResources.INSTANCE.red

    /**
     * A 1x1 green pixel [TextureSlice].
     */
    val green: TextureSlice get() = InternalResources.INSTANCE.green

    /**
     * A 1x1 blue pixel [TextureSlice].
     */
    val blue: TextureSlice get() = InternalResources.INSTANCE.blue

    /**
     * A 1x1 transparent pixel [TextureSlice].
     */
    val transparent: TextureSlice get() = InternalResources.INSTANCE.transparent
}

/**
 * An object that contains a bunch of common fonts that are used across `LittleKt.
 */
object Fonts {
    /**
     * The name of the the tiny version of the default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 9px`
     */
    val tiny get() = InternalResources.INSTANCE.defaultFontTiny

    /**
     * The name of the small version of the  default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 11px`
     */
    val small get() = InternalResources.INSTANCE.defaultFontSmall

    /**
     * The name of the default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 17px`
     */
    val default get() = InternalResources.INSTANCE.defaultFont

    /**
     * The name of the large version of the default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 32px`
     */
    val large get() = InternalResources.INSTANCE.defaultFontLarge
}