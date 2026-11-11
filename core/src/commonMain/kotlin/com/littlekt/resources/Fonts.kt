package com.littlekt.resources

import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.resources.internal.InternalResources

/** An object that contains a bunch of common fonts that are used across **LittleKt**. */
object Fonts {

    /**
     * The name of the default [BitmapFont] used across **LittleKt**.
     *
     * **Note**: you must still load the [BitmapFont]! This is just the NAME.
     *
     * Font type: `Barlow Condensed medium 17px`
     */
    val default: BitmapFont
        get() = InternalResources.INSTANCE.defaultFont

    /**
     * The [default] font without its black outline. Better suited for UI and light backgrounds.
     *
     * Font type: `Barlow Condensed medium 17px`
     */
    val defaultNoOutline: BitmapFont
        get() = InternalResources.INSTANCE.defaultFontNoOutline
}
