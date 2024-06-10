package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.tilemap.TileSet
import com.littlekt.resources.Textures
import kotlin.math.ceil

/**
 * A tileset used in LDtk.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkTileset(
    val identifier: String,
    val uid: Int,
    val cellSize: Int,
    val pxWidth: Int,
    val pxHeight: Int,
    val tiles: List<TextureSlice>,
) : TileSet {

    /** The width of the tilemap/layer in cells */
    val gridWidth
        get() = ceil((pxWidth / cellSize).toFloat()).toInt()

    /** Get the X pixel coordinate (in the atlas image) from a specified tile ID */
    fun getAtlasX(tileId: Int): Int {
        return (tileId - (tileId / gridWidth) * gridWidth)
    }

    /** Get the Y pixel coordinate (in the atlas image) from a specified tile ID */
    fun getAtlasY(tileId: Int): Int {
        return tileId / gridWidth
    }

    private val temp: LDtkTile = LDtkTile(Textures.white)

    internal fun getLDtkTile(tileId: Int, flipBits: Int = 0): LDtkTile? {
        if (tileId < 0) {
            return null
        }

        if (tileId >= tiles.size) {
            return null
        }
        val region = tiles[tileId]
        temp.slice = region
        return when (flipBits) {
            0 ->
                temp.apply {
                    flipX = false
                    flipY = false
                }
            1 ->
                temp.apply {
                    flipX = true
                    flipY = false
                }
            2 ->
                temp.apply {
                    flipX = false
                    flipY = true
                }
            3 ->
                temp.apply {
                    flipX = true
                    flipY = true
                }
            else -> error("Unsupported flipBits value")
        }
    }

    override fun toString(): String {
        return "LDtkTileset(identifier='$identifier', cellSize=$cellSize, pxWidth=$pxWidth, pxHeight=$pxHeight, gridWidth=$gridWidth, tiles=${tiles})"
    }

    internal data class LDtkTile(
        var slice: TextureSlice,
        var flipX: Boolean = false,
        var flipY: Boolean = false
    )
}
