package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.ldtk.*

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevelLoader(private val project: ProjectJson) {

    val assetCache = mutableMapOf<VfsFile, Texture>()
    val tilesets = mutableMapOf<Int, LDtkTileset>()

    suspend fun loadLevel(root: VfsFile, externalRelPath: String): LDtkLevel {
        val levelDef: LevelDefinition = root[externalRelPath].decodeFromString()
        return loadLevel(root, levelDef)
    }

    suspend fun loadLevel(root: VfsFile, levelDef: LevelDefinition): LDtkLevel {
        levelDef.layerInstances?.forEach { layerInstance ->
            project.defs.tilesets.find { it.uid == layerInstance.tilesetDefUid }?.let {
                tilesets.getOrPut(it.uid) { loadTileset(root, it) }
            }
        }
        val bgImage = levelDef.bgRelPath?.let {
            val file = root[it]
            assetCache.getOrPut(file) { file.readTexture() }
        }
        val entities: MutableList<LDtkEntity> = mutableListOf()
        return LDtkLevel(
            uid = levelDef.uid,
            identifier = levelDef.identifier,
            pxWidth = levelDef.pxWid,
            pxHeight = levelDef.pxHei,
            worldX = levelDef.worldX,
            worldY = levelDef.worldY,
            neighbors = levelDef.neighbours?.map {
                LDtkLevel.Neighbor(
                    it.levelUid,
                    LDtkLevel.NeighborDirection.fromDir(it.dir)
                )
            }
                ?: listOf(),
            layers = levelDef.layerInstances?.map { instantiateLayer(it, entities, tilesets) } ?: listOf(),
            entities = entities,
            backgroundColor = levelDef.bgColor,
            levelBackgroundPos = levelDef.bgPos,
            bgImageTexture = bgImage
        )
    }

    private fun getLayerDef(uid: Int?, identifier: String? = ""): LayerDefinition? {
        if (uid == null && identifier == null) {
            return null
        }
        return project.defs.layers.find { it.uid == uid || it.identifier == identifier }
    }

    private fun instantiateLayer(
        json: LayerInstance,
        entities: MutableList<LDtkEntity>,
        tilesets: Map<Int, LDtkTileset>
    ): LDtkLayer {
        return when (json.type) { //IntGrid, Entities, Tiles or AutoLayer
            "IntGrid" -> {
                val intGridValueInfo = getLayerDef(json.layerDefUid)?.intGridValues?.map {
                    LDtkIntGridLayer.ValueInfo(it.identifier, it.value)
                } ?: listOf()
                val intGrid = mutableMapOf<Int, Int>().apply {
                    if (json.intGridCSV != null) {
                        json.intGridCSV.forEachIndexed { index, i ->
                            put(index, i)
                        }
                    } else {
                        json.intGrid?.forEach {
                            put(it.coordID, it.v)
                        }
                    }
                }

                if (getLayerDef(json.layerDefUid)?.autoTilesetDefUid == null) {
                    LDtkIntGridLayer(
                        intGridValueInfo = intGridValueInfo,
                        intGrid = intGrid,
                        identifier = json.identifier,
                        type = LayerType.valueOf(json.type),
                        cellSize = json.gridSize,
                        gridWidth = json.cWid,
                        gridHeight = json.cHei,
                        pxTotalOffsetX = json.pxTotalOffsetX,
                        pxTotalOffsetY = json.pxTotalOffsetY,
                        opacity = json.opacity
                    )
                } else {
                    LDtkIntGridAutoLayer(
                        tileset = tilesets[json.tilesetDefUid]
                            ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"),
                        autoTiles = json.autoLayerTiles.map {
                            LDtkAutoLayer.AutoTile(it.t, it.f, it.px[0], it.px[1])
                        },
                        intGridValueInfo = intGridValueInfo,
                        intGrid = intGrid,
                        identifier = json.identifier,
                        type = LayerType.valueOf(json.type),
                        cellSize = json.gridSize,
                        gridWidth = json.cWid,
                        gridHeight = json.cHei,
                        pxTotalOffsetX = json.pxTotalOffsetX,
                        pxTotalOffsetY = json.pxTotalOffsetY,
                        opacity = json.opacity
                    )
                }
            }
            "Entities" -> {
                json.entityInstances.mapTo(entities) {
                    LDtkEntity(
                        identifier = it.identifier,
                        cx = it.grid[0],
                        cy = it.grid[1],
                        x = it.px[0].toFloat(),
                        y = it.px[1].toFloat(),
                        pivotX = it.pivot[0],
                        pivotY = it.pivot[1],
                        width = it.width,
                        height = it.height,
                        tileInfo = if (it.tile == null) {
                            null
                        } else {
                            LDtkEntity.TileInfo(
                                tilesetUid = it.tile.tilesetUid,
                                x = it.tile.srcRect[0],
                                y = it.tile.srcRect[1],
                                w = it.tile.srcRect[2],
                                h = it.tile.srcRect[3]
                            )
                        }
                    )
                }
                LDtkEntityLayer(
                    entities = entities,
                    identifier = json.identifier,
                    type = LayerType.valueOf(json.type),
                    cellSize = json.gridSize,
                    gridWidth = json.cWid,
                    gridHeight = json.cHei,
                    pxTotalOffsetX = json.pxTotalOffsetX,
                    pxTotalOffsetY = json.pxTotalOffsetY,
                    opacity = json.opacity
                )
            }
            "Tiles" -> {
                val tiles = mutableMapOf<Int, MutableList<LDtkTilesLayer.TileInfo>>().apply {
                    json.gridTiles.forEach {
                        getOrPut(it.d[0]) {
                            mutableListOf()
                        }.add(LDtkTilesLayer.TileInfo(it.t, it.f))
                    }
                }
                LDtkTilesLayer(
                    tileset = tilesets[json.tilesetDefUid]
                        ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"),
                    tiles = tiles,
                    identifier = json.identifier,
                    type = LayerType.valueOf(json.type),
                    cellSize = json.gridSize,
                    gridWidth = json.cWid,
                    gridHeight = json.cHei,
                    pxTotalOffsetX = json.pxTotalOffsetX,
                    pxTotalOffsetY = json.pxTotalOffsetY,
                    opacity = json.opacity
                )
            }
            "AutoLayer" -> {
                LDtkAutoLayer(
                    tileset = tilesets[json.tilesetDefUid]
                        ?: error("Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"),
                    autoTiles = json.autoLayerTiles.map {
                        LDtkAutoLayer.AutoTile(it.t, it.f, it.px[0], it.px[1])
                    },
                    identifier = json.identifier,
                    type = LayerType.valueOf(json.type),
                    cellSize = json.gridSize,
                    gridWidth = json.cWid,
                    gridHeight = json.cHei,
                    pxTotalOffsetX = json.pxTotalOffsetX,
                    pxTotalOffsetY = json.pxTotalOffsetY,
                    opacity = json.opacity
                )
            }
            else -> error("Unable to instantiate layer for level ${json.identifier}")
        }
    }

    suspend fun loadTileset(vfs: VfsFile, tilesetDef: TilesetDefinition) =
        LDtkTileset(
            identifier = tilesetDef.identifier,
            cellSize = tilesetDef.tileGridSize,
            pxWidth = tilesetDef.pxWid,
            pxHeight = tilesetDef.pxHei,
            tiles = vfs[tilesetDef.relPath].readPixmap()
                .sliceWithBorder(vfs.vfs.context, tilesetDef.tileGridSize, tilesetDef.tileGridSize, 4)
        )
}