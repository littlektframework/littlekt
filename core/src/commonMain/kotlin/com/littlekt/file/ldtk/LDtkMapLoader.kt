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
     * Load the LDtk tilemap into a [LDtkWorld] and load the specified level index..
     *
     * @param levelIdx the index of the level to load.
     * @param translateMapHeight if true, the loader will translate each levels 'y' coordinate by
     *   the height of the entire map. Set this to false, if loading individual levels and rendering
     *   them only one at a time in order to have the level base off of the 0,0 coordinate.
     * @return the loaded [LDtkWorld]
     * @see loadLevel
     */
    suspend fun loadMap(levelIdx: Int, translateMapHeight: Boolean = true): LDtkWorld {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()
        val level = mapData.levelDefinitions[levelIdx]
        val maxHeight =
            if (translateMapHeight) mapData.levelDefinitions.maxOf { it.worldY + it.pxHei }
            else level.worldY + level.pxHei
        levels +=
            if (mapData.externalLevels) {
                val path = level.externalRelPath
                levelLoader.loadLevel(
                    parent,
                    path ?: error("Unable to load external level: ${level.identifier}"),
                    enums,
                    maxHeight
                )
            } else {
                levelLoader.loadLevel(parent, level, enums, maxHeight)
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
     * Load the LDtk tilemap into a [LDtkWorld] by loading all levels.
     *
     * @param translateMapHeight if true, the loader will translate each levels 'y' coordinate by
     *   the height of the entire map. Set this to false, if loading individual levels and rendering
     *   them only one at a time in order to have the level base off of the 0,0 coordinate.
     * @return the loaded [LDtkWorld]
     */
    suspend fun loadMap(translateMapHeight: Boolean = true): LDtkWorld {
        val parent = root.parent
        val levels = mutableListOf<LDtkLevel>()
        val mapHeight = mapData.levelDefinitions.maxOf { it.worldY + it.pxHei }
        mapData.levelDefinitions.forEach {
            val maxHeight = if (translateMapHeight) mapHeight else it.worldY + it.pxHei
            levels +=
                if (mapData.externalLevels) {
                    levelLoader.loadLevel(
                        parent,
                        it.externalRelPath
                            ?: error("Unable to load external level: ${it.identifier}"),
                        enums,
                        maxHeight
                    )
                } else {
                    levelLoader.loadLevel(parent, it, enums, maxHeight)
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
     * @param translateMapHeight if true, the loader will translate each levels 'y' coordinate by
     *   the height of the entire map. Set this to false, if loading individual levels and rendering
     *   them only one at a time in order to have the level base off of the 0,0 coordinate.
     * @return the loaded [LDtkLevel]
     */
    suspend fun loadLevel(levelIdx: Int, translateMapHeight: Boolean = true): LDtkLevel {
        val parent = root.parent
        val level = mapData.levelDefinitions[levelIdx]
        val maxHeight =
            if (translateMapHeight) mapData.levelDefinitions.maxOf { it.worldY + it.pxHei }
            else level.worldY + level.pxHei

        return if (mapData.externalLevels) {
            val path = level.externalRelPath
            levelLoader.loadLevel(
                parent,
                path ?: error("Unable to load external level: ${level.identifier}"),
                enums,
                maxHeight
            )
        } else {
            levelLoader.loadLevel(parent, level, enums, maxHeight)
        }
    }

    override fun release() {
        levelLoader.release()
    }
}
