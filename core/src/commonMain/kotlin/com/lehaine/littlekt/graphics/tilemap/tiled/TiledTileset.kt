package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.tilemap.TileSet

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledTileset(
    val tileWidth: Int,
    val tileHeight: Int,
    val tiles: List<TextureSlice>
) : TileSet {
}