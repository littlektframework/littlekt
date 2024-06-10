package com.littlekt.file.ldtk

import com.littlekt.Releasable
import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkEnum
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkEnumValue
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkLevel
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkWorld

/**
 * A loader for reading and parsing LDtk files into typed tilemaps.
 *
 * @see LDtkWorld
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapLoader(
    private val root: VfsFile,
    private val mapData: LDtkMapData,
    atlas: TextureAtlas? = null,
    tilesetBorder: Int = 2,
) : Releasable {
    private val levelLoader = LDtkLevelLoader(mapData, atlas, tilesetBorder)
    private val enums =
        mapData.defs.enums.associateBy(keySelector = { it.identifier }) { enum ->
            val values =
                enum.values.associateBy(keySelector = { it.id }) {
                    LDtkEnumValue(it.id, Color.fromHex(it.color.toString(16)))
                }
            LDtkEnum(enum.identifier, values)
        }
    private val entityDefinitions = mapData.defs.entities.associateBy { it.identifier }

    /**
     * Load the LDtk tilemap into a [LDtkWorld].
     *
     * @param loadAllLevels load all levels and assets if true; otherwise load only the [levelIdx].
     * @param levelIdx the index of the level to load if [loadAllLevels] is false.
     * @return the loaded [LDtkWorld]
     */
    suspend fun loadMap(loadAllLevels: Boolean, levelIdx: Int = 0): LDtkWorld {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()

        when {
            loadAllLevels -> {
                mapData.levelDefinitions.forEach {
                    levels +=
                        if (mapData.externalLevels) {
                            levelLoader.loadLevel(
                                parent,
                                it.externalRelPath
                                    ?: error("Unable to load external level: ${it.identifier}"),
                                enums
                            )
                        } else {
                            levelLoader.loadLevel(parent, it, enums)
                        }
                }
            }
            else -> {
                val level = mapData.levelDefinitions[levelIdx]
                levels +=
                    if (mapData.externalLevels) {
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

        return LDtkWorld(
            mapData.worldLayout ?: error("World Layout is not set."),
            Color.fromHex(mapData.bgColor),
            Color.fromHex(mapData.defaultLevelBgColor),
            levels,
            levelLoader.tilesets,
            enums,
            entityDefinitions
        )
    }

    /**
     * Load a specific level from LDtk.
     *
     * @param levelIdx the index of the level to load
     * @return the loaded [LDtkLevel]
     */
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

    override fun release() {
        levelLoader.release()
    }
}
