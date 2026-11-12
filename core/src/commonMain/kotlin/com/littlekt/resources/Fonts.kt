package com.littlekt.resources

import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.RasterFont
import com.littlekt.graphics.g2d.font.TtfFont
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

    /**
     * The default TrueType font. Use [TtfFont.raster] to draw it sharp at any pixel size.
     *
     * Font type: `Barlow Condensed medium`, licensed under the SIL Open Font License 1.1
     */
    val defaultTtf: TtfFont
        get() = InternalResources.INSTANCE.defaultTtfFont

    /**
     * The [defaultTtf] font rasterized at 17px, the size of [default]. The default scene graph
     * theme uses this.
     */
    val defaultRaster: RasterFont
        get() = InternalResources.INSTANCE.defaultRasterFont
}
