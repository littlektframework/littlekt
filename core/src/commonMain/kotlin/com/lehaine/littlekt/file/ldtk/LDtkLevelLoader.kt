package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileset

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevelLoader(private val map: LDtkTileMap) {
    private val project get() = map.json

    suspend fun loadLevel(vfs: Vfs, levelDefinition: LevelDefinition, map: LDtkTileMap): LDtkLevel {
        val loadedLevelDefinition: LevelDefinition = vfs[levelDefinition.externalRelPath
            ?: error("Unable to load ${levelDefinition.externalRelPath}")].decodeFromString()

        loadedLevelDefinition.layerInstances?.forEach { layerInstance ->
            project.defs.tilesets.find { it.uid == layerInstance.tilesetDefUid }?.let {
                loadTileset(vfs, it)
            }
        }
        val bgImage = loadedLevelDefinition.bgRelPath?.let { vfs[it].readTexture() }
        return LDtkLevel(project, map.tilesets, loadedLevelDefinition, bgImage).also { map.levels += it }
    }

    private suspend fun loadTileset(vfs: Vfs, tilesetDefinition: TilesetDefinition) =
        LDtkTileset(
            tilesetDefinition, vfs[tilesetDefinition.relPath].readTexture()
        )
}