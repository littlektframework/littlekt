package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(val root: VfsFile, val project: ProjectJson) {
    val levelLoader = LDtkLevelLoader(project)

    suspend fun loadMap(loadAllLevels: Boolean, levelIdx: Int = 0): LDtkTileMap {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()
        when {
            loadAllLevels -> {
                project.levelDefinitions.forEach {
                    levels += if (project.externalLevels) {
                        levelLoader.loadLevel(
                            parent,
                            it.externalRelPath ?: error("Unable to load external level: ${it.identifier}")
                        )
                    } else {
                        levelLoader.loadLevel(parent, it)
                    }

                }
            }
            else -> {
                val level = project.levelDefinitions[levelIdx]
                levels += if (project.externalLevels) {
                    val path = level.externalRelPath
                    levelLoader.loadLevel(
                        parent,
                        path ?: error("Unable to load external level: ${level.identifier}")
                    )
                } else {
                    levelLoader.loadLevel(parent, level)
                }
            }
        }
        return LDtkTileMap(project.worldLayout, project.bgColor, levels, levelLoader.tilesets)
    }

    suspend fun loadLevel(levelIdx: Int): LDtkLevel {
        val parent = root.parent
        val level = project.levelDefinitions[levelIdx]
        return if (project.externalLevels) {
            val path = level.externalRelPath
            levelLoader.loadLevel(
                parent,
                path ?: error("Unable to load external level: ${level.identifier}")
            )
        } else {
            levelLoader.loadLevel(parent, level)
        }
    }
}