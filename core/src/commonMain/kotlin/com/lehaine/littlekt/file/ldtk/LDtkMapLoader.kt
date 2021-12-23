package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(val root: VfsFile) {

    suspend fun loadMap(loadAllLevels: Boolean, loadLevelIdx: Int = 0): LDtkTileMap {
        val project = root.decodeFromString<ProjectJson>()
        val parent = root.parent
        val levelLoader = LDtkLevelLoader(project)
        val levels = mutableListOf<LDtkLevel>()
        if (loadAllLevels && project.externalLevels) {
            project.levelDefinitions.forEach { levelDefinition ->
                levels += levelLoader.loadLevel(parent, levelDefinition)
            }
        } else if (project.externalLevels) {
            levels += levelLoader.loadLevel(parent, project.levelDefinitions[loadLevelIdx])
        } else {
            project.defs.tilesets.forEach {
                levelLoader.loadTileset(parent, it)
            }
        }
        return LDtkTileMap(project.worldLayout, project.bgColor, levels, levelLoader.tilesets)
    }
}