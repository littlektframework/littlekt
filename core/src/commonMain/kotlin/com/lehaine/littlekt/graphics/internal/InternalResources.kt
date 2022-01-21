package com.lehaine.littlekt.graphics.internal

import com.lehaine.littlekt.AssetProvider
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.util.internal.SingletonBase

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
internal class InternalResources private constructor(private val context: Context) {
    internal companion object : SingletonBase<InternalResources, Context>(::InternalResources)

    private val assetProvider = AssetProvider(context)

    val atlas: TextureAtlas by assetProvider.load(context.resourcesVfs["default_tiles.json"])
    val white: TextureSlice by assetProvider.prepare { atlas.getByPrefix("pixel_white").slice }
    val defaultFont: BitmapFont by assetProvider.load(context.resourcesVfs["barlow_condensed_medium_regular_17.fnt"])

    init {
        context.onRender {
            assetProvider.update()
        }
    }
}
