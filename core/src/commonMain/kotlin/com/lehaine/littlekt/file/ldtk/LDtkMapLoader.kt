package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileset

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(val root: VfsFile) {

    suspend fun loadMap(loadAllLevels: Boolean, loadLevelIdx: Int = 0): LDtkTileMap {
        val project = root.decodeFromString<ProjectJson>()
        val parent = root.parent
        val map = LDtkTileMap(project)
        val levelLoader = LDtkLevelLoader(map)
        if (loadAllLevels && project.externalLevels) {
            project.levelDefinitions.forEach { levelDefinition ->
                levelLoader.loadLevel(parent, levelDefinition, map)
            }
        } else if (project.externalLevels) {
            levelLoader.loadLevel(parent, project.levelDefinitions[loadLevelIdx], map)
        } else {
            project.defs.tilesets.forEach {
                map.tilesets.getOrPut(it.uid) { loadTileset(root, it) }
            }
        }
        return map
    }

    private suspend fun loadTileset(vfs: VfsFile, tilesetDefinition: TilesetDefinition) =
        LDtkTileset(
            tilesetDefinition, vfs[tilesetDefinition.relPath].readTexture()
                .sliceWithBorder(vfs.vfs.context, tilesetDefinition.tileGridSize, tilesetDefinition.tileGridSize, 4)
        )
}