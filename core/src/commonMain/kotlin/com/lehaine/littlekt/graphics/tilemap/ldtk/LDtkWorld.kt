package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.ldtk.LDtkEntityDefinition
import com.lehaine.littlekt.file.ldtk.LDtkWorldLayout
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.tilemap.TileMap
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkWorld(
    val worldLayout: LDtkWorldLayout,
    val backgroundColor: String,
    val levels: List<LDtkLevel>,
    val tilesets: Map<Int, LDtkTileset>,
    val enums: Map<String, LDtkEnum>,
    val entities: Map<String, LDtkEntityDefinition>
) : TileMap(), Disposable {
    val levelsMap: Map<String, LDtkLevel> by lazy { levels.associateBy { it.identifier } }

    internal var onDispose = {}

    override fun render(batch: Batch, camera: Camera, x: Float, y: Float) {
        viewBounds.calculateViewBounds(camera)
        levels.forEach { it.render(batch, viewBounds, it.worldX + x, it.worldY + y) }
    }

    operator fun get(level: String) = levelsMap[level] ?: error("Level: '$level' does not exist in this map!")

    override fun dispose() {
        onDispose()
    }

    override fun toString(): String {
        return "LDtkMap(levels=$levels, tilesets=$tilesets, worldLayout=$worldLayout, backgroundColor='$backgroundColor', enums=$enums)"
    }
}