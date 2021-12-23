package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.WorldLayout
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.tilemap.TileMap

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkTileMap(
    val worldLayout: WorldLayout,
    val backgroundColor: String,
    val levels: List<LDtkLevel>,
    val tilesets: Map<Int, LDtkTileset>
) : TileMap() {
    val levelsMap: Map<String, List<LDtkLevel>> = levels.groupBy { it.identifier }

    override fun render(batch: SpriteBatch, camera: Camera, x: Float, y: Float) {
//       super.render(batch, camera, viewport)
        viewBounds.x = 0f
        viewBounds.y = 0f
        viewBounds.width = 960f
        viewBounds.height = 540f
        levels.forEach { it.render(batch, viewBounds, it.worldX + x, it.worldY + y) }
    }

    operator fun get(level: String) = levelsMap[level]

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, backgroundColor='$backgroundColor')"
    }
}