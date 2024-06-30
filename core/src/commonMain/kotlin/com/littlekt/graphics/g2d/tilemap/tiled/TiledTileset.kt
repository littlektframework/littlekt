package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.tilemap.TileSet
import kotlin.time.Duration

/**
 * A tileset used in a Tiled layer.
 *
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledTileset(val tileWidth: Int, val tileHeight: Int, val tiles: List<Tile>) : TileSet {

    data class Tile(
        val slice: TextureSlice,
        val id: Int,
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int,
        val frames: List<AnimatedTile>,
        val properties: Map<String, TiledMap.Property>
    )

    data class AnimatedTile(
        val slice: TextureSlice,
        val id: Int,
        val duration: Duration,
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int,
    )
}
