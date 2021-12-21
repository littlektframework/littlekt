package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.TilesetDefinition
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.gl.DataType
import com.lehaine.littlekt.graphics.gl.GLTextureData
import com.lehaine.littlekt.graphics.gl.TextureFormat
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.graphics.tilemap.TileSet
import kotlin.math.ceil

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkTileset(json: TilesetDefinition, val texture: Texture) : TileSet {
    val identifier = json.identifier
    val cellSize = json.tileGridSize
    val pxWidth = json.pxWid
    val pxHeight = json.pxHei
    val gridWidth get() = ceil((pxWidth / cellSize).toFloat()).toInt()

    val tiles = texture.slice(cellSize, cellSize)

    /**
     * Get the X pixel coordinate (in the atlas image) from a specified tile ID
     */
    fun getAtlasX(tileId: Int): Int {
        return (tileId - (tileId / gridWidth) * gridWidth)
    }

    /**
     * Get the Y pixel coordinate (in the atlas image) from a specified tile ID
     */
    fun getAtlasY(tileId: Int): Int {
        return tileId / gridWidth
    }

    internal data class LDtkTile(var slice: TextureSlice, var flipX: Boolean = false, var flipY: Boolean = false)

    private val temp =
        LDtkTile(
            TextureSlice(
                Texture(
                    GLTextureData(
                        1,
                        1,
                        0,
                        TextureFormat.RGBA,
                        TextureFormat.RGBA,
                        DataType.UNSIGNED_BYTE
                    )
                )
            ), flipX = false, flipY = false
        )

    internal fun getLDtkTile(tileId: Int, flipBits: Int = 0): LDtkTile? {
        if (tileId < 0) {
            return null
        }

        val tx = getAtlasX(tileId)
        val ty = getAtlasY(tileId)
        if (ty >= tiles.size) {
            return null
        }
        if (tx >= tiles[ty].size) {
            return null
        }
        val region = tiles[ty][tx]
        temp.slice = region
        return when (flipBits) {
            0 -> temp
            1 -> temp.apply {
                flipX = true
            }
            2 -> temp.apply {
                flipX = false
                flipY = true
            }
            3 -> temp.apply {
                flipX = true
                flipY = true
            }
            else -> error("Unsupported flipBits value")
        }
    }

    override fun toString(): String {
        return "LDtkTileset(texture=$texture, identifier='$identifier', cellSize=$cellSize, pxWidth=$pxWidth, pxHeight=$pxHeight, gridWidth=$gridWidth, tiles=${tiles?.contentToString()})"
    }

}