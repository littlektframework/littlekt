package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileset

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(val project: ProjectJson) {

    suspend fun loadMap(vfs: Vfs, loadAllLevels: Boolean, loadLevelIdx: Int = 0): LDtkTileMap {
        val map = LDtkTileMap(project)
        val levelLoader = LDtkLevelLoader(map)
        if (loadAllLevels && project.externalLevels) {
            project.levelDefinitions.forEach { levelDefinition ->
                levelLoader.loadLevel(vfs, levelDefinition, map)
            }
        } else if (project.externalLevels) {
            levelLoader.loadLevel(vfs, project.levelDefinitions[loadLevelIdx], map)
        } else {
            project.defs.tilesets.forEach {
                loadTileset(vfs, it)
            }
        }
        return map
    }

    private suspend fun loadTileset(vfs: Vfs, tilesetDefinition: TilesetDefinition) =
        LDtkTileset(
            tilesetDefinition, vfs[tilesetDefinition.relPath].readTexture()
        )
}