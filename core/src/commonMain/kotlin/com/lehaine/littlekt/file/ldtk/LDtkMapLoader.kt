package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureAtlas
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
    private val mapData: LDtkMapData,
    atlas: TextureAtlas? = null,
    tilesetBorder: Int = 2,
) : Disposable {
    private val levelLoader = LDtkLevelLoader(mapData, atlas, tilesetBorder)
    private val enums = mapData.defs.enums.associateBy(keySelector = { it.identifier }) { enum ->
        val values =
            enum.values.associateBy(keySelector = { it.id }) {
                LDtkEnumValue(
                    it.id,
                    Color.fromHex(it.color.toString(16))
                )
            }
        LDtkEnum(enum.identifier, values)
    }
    private val entityDefinitions = mapData.defs.entities.associateBy { it.identifier }

    suspend fun loadMap(loadAllLevels: Boolean, levelIdx: Int = 0): LDtkWorld {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()

        when {
            loadAllLevels -> {
                mapData.levelDefinitions.forEach {
                    levels += if (mapData.externalLevels) {
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
                val level = mapData.levelDefinitions[levelIdx]
                levels += if (mapData.externalLevels) {
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

        return LDtkWorld(mapData.worldLayout, mapData.bgColor, levels, levelLoader.tilesets, enums, entityDefinitions)
    }

    suspend fun loadLevel(levelIdx: Int): LDtkLevel {
        val parent = root.parent
        val level = mapData.levelDefinitions[levelIdx]

        return if (mapData.externalLevels) {
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