package com.lehaine.littlekt.graphics.internal

import com.lehaine.littlekt.AssetProvider
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
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

    val assetProvider = AssetProvider(context)

    lateinit var atlas: TextureAtlas
    lateinit var white: TextureSlice
    lateinit var defaultFont: BitmapFont

    suspend fun load() {
        atlas = context.resourcesVfs["default_tiles.json"].readAtlas()
        defaultFont = context.resourcesVfs["barlow_condensed_medium_regular_17.fnt"].readBitmapFont()
        white = atlas.getByPrefix("pixel_white").slice
    }

    init {
        context.onRender {
            assetProvider.update()
        }
    }
}
