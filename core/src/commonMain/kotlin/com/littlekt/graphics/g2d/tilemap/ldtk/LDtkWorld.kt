package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.Releasable
import com.littlekt.file.ldtk.LDtkEntityDefinition
import com.littlekt.file.ldtk.LDtkWorldLayout
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.tilemap.TileMap
import com.littlekt.util.calculateViewBounds
import com.littlekt.util.datastructure.fastForEach

/**
 * The world info of LDtk.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkWorld(
    val worldLayout: LDtkWorldLayout,
    val backgroundColor: Color,
    val defaultLevelBackgroundColor: Color,
    val levels: List<LDtkLevel>,
    val tilesets: Map<Int, LDtkTileset>,
    val enums: Map<String, LDtkEnum>,
    val entities: Map<String, LDtkEntityDefinition>
) : TileMap(), Releasable {
    val levelsMap: Map<String, LDtkLevel> by lazy { levels.associateBy { it.identifier } }
    val maxWidth: Int = levels.maxOf { it.worldX + it.pxWidth }
    val maxHeight: Int = levels.maxOf { it.worldY + it.pxHeight }

    internal var onRelease = {}

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        levels.fastForEach {
            it.addToCache(cache, it.worldX * scale + x, it.worldY * scale + y, scale)
        }
    }

    override fun removeFromCache(cache: SpriteCache) {
        levels.fastForEach { it.removeFromCache(cache) }
    }

    override fun render(batch: Batch, camera: Camera, x: Float, y: Float, scale: Float) {
        viewBounds.calculateViewBounds(camera)
        levels.forEach {
            it.render(batch, viewBounds, it.worldX * scale + x, it.worldY * scale + y, scale)
        }
    }

    operator fun get(level: String) =
        levelsMap[level] ?: error("Level: '$level' does not exist in this map!")

    override fun release() {
        onRelease()
    }

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, backgroundColor='$backgroundColor', enums=$enums)"
    }
}
