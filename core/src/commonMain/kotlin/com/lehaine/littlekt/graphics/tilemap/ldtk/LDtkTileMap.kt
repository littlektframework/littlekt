package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.ProjectJson
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Viewport
import com.lehaine.littlekt.graphics.tilemap.TileMap

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkTileMap(val json: ProjectJson) : TileMap() {
    val levels = mutableListOf<LDtkLevel>()
    val tilesets = mutableMapOf<Int, LDtkTileset>()

    val worldLayout = json.worldLayout
    val bgColorHex = json.bgColor

    override fun render(batch: SpriteBatch, camera: Camera, viewport: Viewport, renderWithOffsets: Boolean) {
//       super.render(batch, camera, viewport)
        viewBounds.x = 0f
        viewBounds.y = 0f
        viewBounds.width = 960f
        viewBounds.height = 540f
        levels.forEach { it.render(batch, viewBounds, renderWithOffsets) }
    }

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, bgColorHex='$bgColorHex')"
    }
}