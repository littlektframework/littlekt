package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.ProjectJson
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Viewport
import com.lehaine.littlekt.graphics.tilemap.TileMap
import com.lehaine.littlekt.math.Rect
import kotlin.math.abs

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkTileMap(val json: ProjectJson) : TileMap {
    val levels = mutableListOf<LDtkLevel>()
    val tilesets = mutableMapOf<Int, LDtkTileset>()

    val worldLayout = json.worldLayout
    val bgColorHex = json.bgColor

    private val viewBounds = Rect()

    override fun render(batch: SpriteBatch, camera: Camera, viewport: Viewport) {
        val width = viewport.width * camera.zoom
        val height = viewport.height * camera.zoom
        val w = width * abs(camera.up.y) + height * abs(camera.up.x)
        val h = height * abs(camera.up.y) + width * abs(camera.up.x)
        viewBounds.x = camera.position.x - w / 2
        viewBounds.y = camera.position.y - h / 2
        viewBounds.width = w
        viewBounds.height = h

        levels.forEach { it.render(batch, viewBounds) }
    }

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, bgColorHex='$bgColorHex')"
    }
}