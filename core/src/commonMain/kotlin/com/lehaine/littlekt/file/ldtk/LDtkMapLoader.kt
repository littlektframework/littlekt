package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEnum
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEnumValue
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(
    private val root: VfsFile,
    private val project: ProjectJson,
    tilesetBorder: Int = 2,
) : Disposable {
    private val levelLoader = LDtkLevelLoader(project, tilesetBorder)
    private val enums = project.defs.enums.associateBy(keySelector = { it.identifier }) { enum ->
        val values =
            enum.values.associateBy(keySelector = { it.id }) {
                LDtkEnumValue(
                    it.id,
                    Color.fromHex(it.color.toString(16))
                )
            }
        LDtkEnum(enum.identifier, values)
    }
    private val entityDefinitions = project.defs.entities.associateBy { it.identifier }

    suspend fun loadMap(loadAllLevels: Boolean, levelIdx: Int = 0): LDtkWorld {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()

        when {
            loadAllLevels -> {
                project.levelDefinitions.forEach {
                    levels += if (project.externalLevels) {
                        levelLoader.loadLevel(
                            parent,
                            it.externalRelPath ?: error("Unable to load external level: ${it.identifier}"),
                            enums
                        )
                    } else {
                        levelLoader.loadLevel(parent, it, enums)
                    }

                }
            }
            else -> {
                val level = project.levelDefinitions[levelIdx]
                levels += if (project.externalLevels) {
                    val path = level.externalRelPath
                    levelLoader.loadLevel(
                        parent,
                        path ?: error("Unable to load external level: ${level.identifier}"),
                        enums
                    )
                } else {
                    levelLoader.loadLevel(parent, level, enums)
                }
            }
        }

        return LDtkWorld(project.worldLayout, project.bgColor, levels, levelLoader.tilesets, enums, entityDefinitions)
    }

    suspend fun loadLevel(levelIdx: Int): LDtkLevel {
        val parent = root.parent
        val level = project.levelDefinitions[levelIdx]

        return if (project.externalLevels) {
            val path = level.externalRelPath
            levelLoader.loadLevel(
                parent,
                path ?: error("Unable to load external level: ${level.identifier}"),
                enums
            )
        } else {
            levelLoader.loadLevel(parent, level, enums)
        }
    }

    override fun dispose() {
        levelLoader.dispose()
    }
}