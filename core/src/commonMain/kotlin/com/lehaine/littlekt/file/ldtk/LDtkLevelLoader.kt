package com.lehaine.littlekt.file.ldtk

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.ldtk.*
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevelLoader(private val project: ProjectJson) : Disposable {

    var sliceBorder = 2
    val assetCache = mutableMapOf<VfsFile, Texture>()
    val tilesets = mutableMapOf<Int, LDtkTileset>()

    suspend fun loadLevel(root: VfsFile, externalRelPath: String, enums: Map<String, LDtkEnum>): LDtkLevel {
        val levelDef: LevelDefinition = root[externalRelPath].decodeFromString()
        return loadLevel(root, levelDef, enums)
    }

    suspend fun loadLevel(root: VfsFile, levelDef: LevelDefinition, enums: Map<String, LDtkEnum>): LDtkLevel {
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
            layers = levelDef.layerInstances?.map { instantiateLayer(it, entities, tilesets, enums) } ?: listOf(),
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
        tilesets: Map<Int, LDtkTileset>,
        enums: Map<String, LDtkEnum>
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
                json.entityInstances.mapTo(entities) { entity ->
                    val fields = mutableMapOf<String, LDtkField<*>>()
                    val entityDef = project.defs.entities.first { it.uid == entity.defUid }
                    entity.fieldInstances.forEach { field ->
                        val isArray = ARRAY_REGEX.matches(field.type)
                        val type = if (isArray) {
                            val match = ARRAY_REGEX.find(field.type) ?: error("Unable to find Array Field Type!")
                            match.groupValues[1]
                        } else {
                            field.type
                        }
                        val fieldDef = entityDef.fieldDefs.first { it.uid == field.defUid }
                        val canBeNull = fieldDef.canBeNull
                        if (isArray) {
                            when (type) {
                                "Int" -> {
                                    val values = field.value?.stringList?.map {
                                        LDtkValueField(it.toInt())
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Float" -> {
                                    val values = field.value?.stringList?.map {
                                        LDtkValueField(it.toFloat())
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Bool" -> {
                                    val values = field.value?.stringList?.map {
                                        LDtkValueField(it.toBoolean())
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "String" -> {
                                    val values = field.value?.stringList?.map {
                                        LDtkValueField(it)
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Color" -> {
                                    val values = field.value?.stringList?.map {
                                        LDtkValueField(Color.fromHex(it.toInt().toString(16)))
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Point" -> {
                                    val values = field.value?.stringMapList?.map {
                                        LDtkValueField(
                                            Point(
                                                it["cx"]?.toInt()
                                                    ?: error("Unable to find 'cx' value when creating Point LDtkField!"),
                                                it["cy"]?.toInt()
                                                    ?: error("Unable to find 'cy' value when creating Point LDtkField!")
                                            )
                                        )
                                    } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "FilePath" -> {
                                    TODO("FilePath not yet implemented!")
                                }
                                else -> {
                                    when {
                                        "LocalEnum." in type -> {
                                            val enumName = type.substring(type.indexOf(".") + 1)
                                            val values = field.value?.stringList?.map {
                                                LDtkValueField(enums[enumName]?.get(it))
                                            } ?: emptyList()
                                            fields[field.identifier] = LDtkArrayField(values)
                                        }
                                        "ExternEnum." in type -> {
                                            logger.warn { "ExternEnums are not supported! ($type)" }
                                        }
                                        else -> {
                                            logger.warn { "Unsupported field type: $type" }
                                        }
                                    }
                                }
                            }
                        } else {
                            when (type) {
                                "Int" -> {
                                    fields[field.identifier] = if (canBeNull) {
                                        LDtkValueField(field.value?.content?.toInt())
                                    } else {
                                        LDtkValueField(field.value!!.content!!.toInt())
                                    }
                                }
                                "Float" -> {
                                    fields[field.identifier] = if (canBeNull) {
                                        LDtkValueField(field.value?.content?.toFloat())
                                    } else {
                                        LDtkValueField(field.value!!.content!!.toFloat())
                                    }
                                }
                                "Bool" -> {
                                    fields[field.identifier] = if (canBeNull) {
                                        LDtkValueField(field.value?.content?.toBoolean())
                                    } else {
                                        LDtkValueField(field.value!!.content!!.toBoolean())
                                    }
                                }
                                "String" -> {
                                    fields[field.identifier] = if (canBeNull) {
                                        LDtkValueField(field.value?.content)
                                    } else {
                                        LDtkValueField(field.value!!.content!!)
                                    }
                                }
                                "Color" -> {
                                    fields[field.identifier] =
                                        LDtkValueField(
                                            Color.fromHex(
                                                field.value?.content?.toInt()?.toString(16) ?: "#000000"
                                            )
                                        )
                                }
                                "Point" -> {
                                    fields[field.identifier] = if (canBeNull) {
                                        LDtkValueField(field.value?.stringMap?.let {
                                            Point(
                                                it["cx"]?.toInt()
                                                    ?: error("Unable to find 'cx' value when creating Point LDtkField!"),
                                                it["cy"]?.toInt()
                                                    ?: error("Unable to find 'cy' value when creating Point LDtkField!")
                                            )
                                        })
                                    } else {
                                        LDtkValueField(field.value!!.stringMap!!.let {
                                            Point(
                                                it["cx"]?.toInt()
                                                    ?: error("Unable to find 'cx' value when creating Point LDtkField!"),
                                                it["cy"]?.toInt()
                                                    ?: error("Unable to find 'cy' value when creating Point LDtkField!")
                                            )
                                        })
                                    }
                                }
                                "FilePath" -> {
                                    TODO("FilePath not yet implemented!")
                                }
                                else -> {
                                    when {
                                        "LocalEnum." in type -> {
                                            val enumName = type.substring(type.indexOf(".") + 1)
                                            fields[field.identifier] = if (canBeNull) {
                                                LDtkValueField(enums[enumName]?.get(field.value?.content))
                                            } else {
                                                LDtkValueField(enums[enumName]!![field.value?.content]!!)
                                            }
                                        }
                                        "ExternEnum." in type -> {
                                            logger.warn { "ExternEnums are not supported! ($type)" }
                                        }
                                        else -> {
                                            logger.warn { "Unsupported field type: $type" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LDtkEntity(
                        identifier = entity.identifier,
                        cx = entity.grid[0],
                        cy = entity.grid[1],
                        x = entity.px[0].toFloat(),
                        y = entity.px[1].toFloat(),
                        pivotX = entity.pivot[0],
                        pivotY = entity.pivot[1],
                        width = entity.width,
                        height = entity.height,
                        tileInfo = if (entity.tile == null) {
                            null
                        } else {
                            LDtkEntity.TileInfo(
                                tilesetUid = entity.tile.tilesetUid,
                                x = entity.tile.srcRect[0],
                                y = entity.tile.srcRect[1],
                                w = entity.tile.srcRect[2],
                                h = entity.tile.srcRect[3]
                            )
                        },
                        fields
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
                .sliceWithBorder(vfs.vfs.context, tilesetDef.tileGridSize, tilesetDef.tileGridSize, sliceBorder)
        )

    override fun dispose() {
        assetCache.values.forEach { it.dispose() }
    }

    companion object {
        private val ARRAY_REGEX = "Array<(.*)>".toRegex()
        private val logger = Logger<LDtkLevelLoader>()
    }
}