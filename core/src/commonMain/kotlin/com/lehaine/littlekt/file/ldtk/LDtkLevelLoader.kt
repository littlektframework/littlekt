package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileset

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevelLoader(private val map: LDtkTileMap) {
    private val project get() = map.json

    private val assetCache = mutableMapOf<VfsFile, Texture>()
    suspend fun loadLevel(root: VfsFile, levelDefinition: LevelDefinition, map: LDtkTileMap): LDtkLevel {
        val loadedLevelDefinition: LevelDefinition = root[levelDefinition.externalRelPath
            ?: error("Unable to load ${levelDefinition.externalRelPath}")].decodeFromString()

        loadedLevelDefinition.layerInstances?.forEach { layerInstance ->
            project.defs.tilesets.find { it.uid == layerInstance.tilesetDefUid }?.let {
                map.tilesets.getOrPut(it.uid) { loadTileset(root, it) }
            }
        }
        val bgImage = loadedLevelDefinition.bgRelPath?.let {
            val file = root[it]
            assetCache.getOrPut(file) { file.readTexture() }
        }
        return LDtkLevel(project, map.tilesets, loadedLevelDefinition, bgImage).also { map.levels += it }
    }

    private suspend fun loadTileset(vfs: VfsFile, tilesetDefinition: TilesetDefinition) =
        LDtkTileset(
            tilesetDefinition,
            vfs[tilesetDefinition.relPath].readPixmap()
                .sliceWithBorder(vfs.vfs.context, tilesetDefinition.tileGridSize, tilesetDefinition.tileGridSize, 4)
        )
}