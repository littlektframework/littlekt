package com.littlekt.file.ldtk

import com.littlekt.Releasable
import com.littlekt.file.vfs.*
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.tilemap.ldtk.*
import com.littlekt.graphics.slice
import com.littlekt.graphics.sliceWithBorderToTexture
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.log.Logger
import com.littlekt.math.geom.Point

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
internal class LDtkLevelLoader(
    private val mapData: LDtkMapData,
    private val atlas: TextureAtlas? = null,
    private val sliceBorder: Int = 2,
) : Releasable {
    private val assetCache = mutableMapOf<VfsFile, TextureSlice>()
    internal val tilesets = mutableMapOf<Int, LDtkTileset>()

    suspend fun loadLevel(
        root: VfsFile,
        externalRelPath: String,
        enums: Map<String, LDtkEnum>,
        maxHeight: Int
    ): LDtkLevel {
        val levelDef: LDtkLevelDefinition = root[externalRelPath].decodeFromString()
        return loadLevel(root, levelDef, enums, maxHeight)
    }

    suspend fun loadLevel(
        root: VfsFile,
        levelDef: LDtkLevelDefinition,
        enums: Map<String, LDtkEnum>,
        maxHeight: Int
    ): LDtkLevel {
        levelDef.layerInstances?.forEach { layerInstance ->
            mapData.defs.tilesets
                .find { it.uid == layerInstance.tilesetDefUid }
                ?.let { tilesets.getOrPut(it.uid) { loadTileset(root, it) } }
        }
        val bgImage =
            levelDef.bgRelPath?.let {
                val file = root[it]
                assetCache.getOrPut(file) {
                    atlas?.get(it.pathInfo.baseName)?.slice ?: file.readTexture().slice()
                }
            }
        val entities: MutableList<LDtkEntity> = mutableListOf()
        return LDtkLevel(
            uid = levelDef.uid,
            identifier = levelDef.identifier,
            iid = levelDef.iid,
            pxWidth = levelDef.pxWid,
            pxHeight = levelDef.pxHei,
            worldX = levelDef.worldX,
            // flip it from y-down to y-up coords
            worldY = maxHeight - (levelDef.worldY + levelDef.pxHei),
            neighbors =
                levelDef.neighbours?.map {
                    LDtkLevel.Neighbor(
                        it.levelUid,
                        it.levelIid,
                        LDtkLevel.NeighborDirection.fromDir(it.dir)
                    )
                } ?: listOf(),
            layers =
                levelDef.layerInstances?.map { instantiateLayer(it, entities, tilesets, enums) }
                    ?: listOf(),
            entities = entities,
            backgroundColor = Color.fromHex(levelDef.bgColor),
            levelBackgroundPos = levelDef.bgPos,
            bgImageTexture = bgImage
        )
    }

    private fun getLayerDef(uid: Int?, identifier: String? = ""): LDtkLayerDefinition? {
        if (uid == null && identifier == null) {
            return null
        }
        return mapData.defs.layers.find { it.uid == uid || it.identifier == identifier }
    }

    private fun instantiateLayer(
        json: LDtkLayerInstance,
        entities: MutableList<LDtkEntity>,
        tilesets: Map<Int, LDtkTileset>,
        enums: Map<String, LDtkEnum>,
    ): LDtkLayer {
        return when (json.type) { // IntGrid, Entities, Tiles or AutoLayer
            "IntGrid" -> {
                val intGridValueInfo =
                    getLayerDef(json.layerDefUid)?.intGridValues?.map {
                        LDtkIntGridLayer.ValueInfo(it.identifier, it.color)
                    } ?: listOf()
                val intGrid =
                    mutableMapOf<Int, Int>().apply {
                        if (json.intGridCSV != null) {
                            json.intGridCSV.forEachIndexed { index, i -> put(index, i) }
                        } else {
                            json.intGrid?.forEach { put(it.coordID, it.v) }
                        }
                    }

                val layerDef = getLayerDef(json.layerDefUid)
                if (layerDef?.tilesetDefUid == null && layerDef?.autoTilesetDefUid == null) {
                    LDtkIntGridLayer(
                        intGridValueInfo = intGridValueInfo,
                        intGrid = intGrid,
                        identifier = json.identifier,
                        iid = json.iid,
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
                        tileset =
                            tilesets[json.tilesetDefUid]
                                ?: error(
                                    "Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"
                                ),
                        autoTiles =
                            json.autoLayerTiles.map {
                                LDtkAutoLayer.AutoTile(
                                    tileId = it.t,
                                    flips = it.f,
                                    renderX = it.px[0],
                                    // flip since ldtk is y-down
                                    renderY = (json.cHei - 1) * json.gridSize - it.px[1]
                                )
                            },
                        intGridValueInfo = intGridValueInfo,
                        intGrid = intGrid,
                        identifier = json.identifier,
                        iid = json.iid,
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
                    val entityDef = mapData.defs.entities.first { it.uid == entity.defUid }
                    entity.fieldInstances.forEach { field ->
                        val isArray = ARRAY_REGEX.matches(field.type)
                        val type =
                            if (isArray) {
                                val match =
                                    ARRAY_REGEX.find(field.type)
                                        ?: error("Unable to find Array Field Type!")
                                match.groupValues[1]
                            } else {
                                field.type
                            }
                        val fieldDef = entityDef.fieldDefs.first { it.uid == field.defUid }
                        val canBeNull = fieldDef.canBeNull
                        if (isArray) {
                            when (type) {
                                "Int" -> {
                                    val values =
                                        field.value?.stringList?.map { LDtkValueField(it.toInt()) }
                                            ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Float" -> {
                                    val values =
                                        field.value?.stringList?.map {
                                            LDtkValueField(it.toFloat())
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Bool" -> {
                                    val values =
                                        field.value?.stringList?.map {
                                            LDtkValueField(it.toBoolean())
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "String" -> {
                                    val values =
                                        field.value?.stringList?.map { LDtkValueField(it) }
                                            ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Color" -> {
                                    val values =
                                        field.value?.stringList?.map {
                                            LDtkValueField(Color.fromHex(it))
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "Point" -> {
                                    val values =
                                        field.value?.stringMapList?.map {
                                            LDtkValueField(
                                                Point(
                                                    it["cx"]?.toInt()
                                                        ?: error(
                                                            "Unable to find 'cx' value when creating Point LDtkField!"
                                                        ),
                                                    it["cy"]?.toInt()
                                                        ?: error(
                                                            "Unable to find 'cy' value when creating Point LDtkField!"
                                                        )
                                                )
                                            )
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "FilePath" -> {
                                    logger.warn {
                                        "FilePath fields '${field.identifier}' are currently not supported!"
                                    }
                                }
                                "Tile" -> {
                                    val values =
                                        field.value?.stringMapList?.map {
                                            LDtkValueField(
                                                LDtkTileInfo(
                                                    tilesetUid =
                                                        it["tilesetUid"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'tilesetUid' value when creating LDtkTileInfo LDtkField!"
                                                            ),
                                                    x =
                                                        it["x"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'x' value when creating LDtkTileInfo LDtkField!"
                                                            ),
                                                    y =
                                                        it["y"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'y' value when creating LDtkTileInfo LDtkField!"
                                                            ),
                                                    w =
                                                        it["w"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'w' value when creating LDtkTileInfo LDtkField!"
                                                            ),
                                                    h =
                                                        it["h"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'h' value when creating LDtkTileInfo LDtkField!"
                                                            )
                                                )
                                            )
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                "EntityRef" -> {
                                    val values =
                                        field.value?.stringMapList?.map {
                                            LDtkValueField(
                                                LDtkEntityRef(
                                                    it["entityIid"]
                                                        ?: error(
                                                            "Unable to find 'entityIid' value when creating LDtkEntityRef LDtkField!"
                                                        ),
                                                    it["layerIid"]
                                                        ?: error(
                                                            "Unable to find 'layerIid' value when creating LDtkEntityRef LDtkField!"
                                                        ),
                                                    it["levelIid"]
                                                        ?: error(
                                                            "Unable to find 'levelIid' value when creating LDtkEntityRef LDtkField!"
                                                        ),
                                                    it["worldIid"]
                                                        ?: error(
                                                            "Unable to find 'worldIid' value when creating LDtkEntityRef LDtkField!"
                                                        )
                                                )
                                            )
                                        } ?: emptyList()
                                    fields[field.identifier] = LDtkArrayField(values)
                                }
                                else -> {
                                    when {
                                        "LocalEnum." in type -> {
                                            val enumName = type.substring(type.indexOf(".") + 1)
                                            val values =
                                                field.value?.stringList?.map {
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
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(field.value?.content?.toInt())
                                        } else {
                                            LDtkValueField(field.value!!.content!!.toInt())
                                        }
                                }
                                "Float" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(field.value?.content?.toFloat())
                                        } else {
                                            LDtkValueField(field.value!!.content!!.toFloat())
                                        }
                                }
                                "Bool" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(field.value?.content?.toBoolean())
                                        } else {
                                            LDtkValueField(field.value!!.content!!.toBoolean())
                                        }
                                }
                                "String" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(field.value?.content)
                                        } else {
                                            LDtkValueField(field.value!!.content!!)
                                        }
                                }
                                "Color" -> {
                                    fields[field.identifier] =
                                        LDtkValueField(Color.fromHex(field.value?.content ?: "#000000"))
                                }
                                "Point" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(
                                                field.value?.stringMap?.let {
                                                    Point(
                                                        it["cx"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'cx' value when creating Point LDtkField!"
                                                            ),
                                                        it["cy"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'cy' value when creating Point LDtkField!"
                                                            )
                                                    )
                                                }
                                            )
                                        } else {
                                            LDtkValueField(
                                                field.value!!.stringMap!!.let {
                                                    Point(
                                                        it["cx"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'cx' value when creating Point LDtkField!"
                                                            ),
                                                        it["cy"]?.toInt()
                                                            ?: error(
                                                                "Unable to find 'cy' value when creating Point LDtkField!"
                                                            )
                                                    )
                                                }
                                            )
                                        }
                                }
                                "FilePath" -> {
                                    logger.warn {
                                        "FilePath fields '${field.identifier}' are currently not supported!"
                                    }
                                }
                                "Tile" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(
                                                field.value?.stringMap?.let {
                                                    LDtkTileInfo(
                                                        tilesetUid =
                                                            it["tilesetUid"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'tilesetUid' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        x =
                                                            it["x"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'x' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        y =
                                                            it["y"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'y' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        w =
                                                            it["w"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'w' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        h =
                                                            it["h"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'h' value when creating LDtkTileInfo LDtkField!"
                                                                )
                                                    )
                                                }
                                            )
                                        } else {
                                            LDtkValueField(
                                                field.value!!.stringMap!!.let {
                                                    LDtkTileInfo(
                                                        tilesetUid =
                                                            it["tilesetUid"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'tilesetUid' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        x =
                                                            it["x"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'x' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        y =
                                                            it["y"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'y' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        w =
                                                            it["w"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'w' value when creating LDtkTileInfo LDtkField!"
                                                                ),
                                                        h =
                                                            it["h"]?.toInt()
                                                                ?: error(
                                                                    "Unable to find 'h' value when creating LDtkTileInfo LDtkField!"
                                                                )
                                                    )
                                                }
                                            )
                                        }
                                }
                                "EntityRef" -> {
                                    fields[field.identifier] =
                                        if (canBeNull) {
                                            LDtkValueField(
                                                field.value?.stringMap?.let {
                                                    LDtkEntityRef(
                                                        it["entityIid"]
                                                            ?: error(
                                                                "Unable to find 'entityIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["layerIid"]
                                                            ?: error(
                                                                "Unable to find 'layerIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["levelIid"]
                                                            ?: error(
                                                                "Unable to find 'levelIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["worldIid"]
                                                            ?: error(
                                                                "Unable to find 'worldIid' value when creating LDtkEntityRef LDtkField!"
                                                            )
                                                    )
                                                }
                                            )
                                        } else {
                                            LDtkValueField(
                                                field.value!!.stringMap!!.let {
                                                    LDtkEntityRef(
                                                        it["entityIid"]
                                                            ?: error(
                                                                "Unable to find 'entityIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["layerIid"]
                                                            ?: error(
                                                                "Unable to find 'layerIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["levelIid"]
                                                            ?: error(
                                                                "Unable to find 'levelIid' value when creating LDtkEntityRef LDtkField!"
                                                            ),
                                                        it["worldIid"]
                                                            ?: error(
                                                                "Unable to find 'worldIid' value when creating LDtkEntityRef LDtkField!"
                                                            )
                                                    )
                                                }
                                            )
                                        }
                                }
                                else -> {
                                    when {
                                        "LocalEnum." in type -> {
                                            val enumName = type.substring(type.indexOf(".") + 1)
                                            fields[field.identifier] =
                                                if (canBeNull) {
                                                    LDtkValueField(
                                                        enums[enumName]?.get(field.value?.content)
                                                    )
                                                } else {
                                                    LDtkValueField(
                                                        enums[enumName]!![field.value?.content]!!
                                                    )
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
                        iid = entity.iid,
                        cx = entity.grid[0],
                        cy = entity.grid[1],
                        x = entity.px[0].toFloat(),
                        // flip since ldtk is y-down
                        y = (json.cHei - 1) * json.gridSize - entity.px[1].toFloat(),
                        pivotX = entity.pivot[0],
                        pivotY = entity.pivot[1],
                        width = entity.width,
                        height = entity.height,
                        tileInfo =
                            if (entity.tile == null) {
                                null
                            } else {
                                LDtkTileInfo(
                                    tilesetUid = entity.tile.tilesetUid,
                                    x =
                                        if (entity.tile.srcRect.isNotEmpty()) entity.tile.srcRect[0]
                                        else entity.tile.x,
                                    y =
                                        if (entity.tile.srcRect.isNotEmpty()) entity.tile.srcRect[1]
                                        else entity.tile.y,
                                    w =
                                        if (entity.tile.srcRect.isNotEmpty()) entity.tile.srcRect[2]
                                        else entity.tile.w,
                                    h =
                                        if (entity.tile.srcRect.isNotEmpty()) entity.tile.srcRect[3]
                                        else entity.tile.h
                                )
                            },
                        fields
                    )
                }
                LDtkEntityLayer(
                    entities = entities,
                    identifier = json.identifier,
                    iid = json.iid,
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
                val tiles =
                    mutableMapOf<Int, MutableList<LDtkTilesLayer.TileInfo>>().apply {
                        json.gridTiles.forEach {
                            getOrPut(it.d[0]) { mutableListOf() }
                                .add(
                                    LDtkTilesLayer.TileInfo(
                                        tileId = it.t,
                                        flipBits = it.f,
                                        renderX = it.px[0],
                                        // flip since ldtk is y-down
                                        renderY = (json.cHei - 1) * json.gridSize - it.px[1]
                                    )
                                )
                        }
                    }
                LDtkTilesLayer(
                    tileset =
                        tilesets[json.tilesetDefUid]
                            ?: error(
                                "Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"
                            ),
                    tiles = tiles,
                    identifier = json.identifier,
                    iid = json.iid,
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
                    tileset =
                        tilesets[json.tilesetDefUid]
                            ?: error(
                                "Unable to retrieve LDtk tileset: ${json.tilesetDefUid} at ${json.tilesetRelPath}"
                            ),
                    autoTiles =
                        json.autoLayerTiles.map {
                            LDtkAutoLayer.AutoTile(
                                tileId = it.t,
                                flips = it.f,
                                renderX = it.px[0],
                                // flip since ldtk is y-down
                                renderY = (json.cHei - 1) * json.gridSize - it.px[1]
                            )
                        },
                    identifier = json.identifier,
                    iid = json.iid,
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

    suspend fun loadTileset(vfs: VfsFile, tilesetDef: LDtkTilesetDefinition) =
        LDtkTileset(
            identifier = tilesetDef.identifier,
            uid = tilesetDef.uid,
            cellSize = tilesetDef.tileGridSize,
            pxWidth = tilesetDef.pxWid,
            pxHeight = tilesetDef.pxHei,
            tiles =
                atlas
                    ?.get(tilesetDef.relPath.pathInfo.baseName)
                    ?.slice
                    ?.slice(tilesetDef.tileGridSize, tilesetDef.tileGridSize, sliceBorder)
                    ?.flatten()
                    ?: vfs[tilesetDef.relPath]
                        .readPixmap()
                        .sliceWithBorderToTexture(
                            device = vfs.vfs.context.graphics.device,
                            preferredFormat = vfs.vfs.context.graphics.textureFormat,
                            sliceWidth = tilesetDef.tileGridSize,
                            sliceHeight = tilesetDef.tileGridSize,
                            border = sliceBorder
                        )
        )

    override fun release() {
        assetCache.values.forEach { it.texture.release() }
        assetCache.clear()
        tilesets.clear()
    }

    companion object {
        private val ARRAY_REGEX = "Array<(.*)>".toRegex()
        private val logger = Logger<LDtkLevelLoader>()
    }
}
