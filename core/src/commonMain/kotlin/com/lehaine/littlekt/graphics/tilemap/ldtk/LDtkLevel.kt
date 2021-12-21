package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.LayerDefinition
import com.lehaine.littlekt.file.ldtk.LayerInstance
import com.lehaine.littlekt.file.ldtk.LevelDefinition
import com.lehaine.littlekt.file.ldtk.ProjectJson
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevel(
    private val projectJson: ProjectJson,
    private val tilesets: Map<Int, LDtkTileset>,
    json: LevelDefinition,
    bgImageTexture: Texture? = null
) {
    enum class NeighborDirection {
        North,
        South,
        West,
        East;

        companion object {
            fun fromDir(dir: String): NeighborDirection {
                return when (dir.toLowerCase()) {
                    "n" -> North
                    "e" -> East
                    "s" -> South
                    "w" -> West
                    else -> {
                        println("WARNING: unknown neighbor level direction: $dir")
                        North
                    }
                }
            }
        }
    }

    data class Neighbor(val levelUid: Int, val dir: NeighborDirection)

    data class CropRect(val x: Float, val y: Float, val w: Float, val h: Float);

    data class LevelBgImage(
        val relFilePath: String,
        val topLeftX: Int,
        val topLeftY: Int,
        val scaleX: Float,
        val scaleY: Float,
        val cropRect: CropRect
    )

    var uid = json.uid
        private set
    var identifier = json.identifier
        private set
    var pxWidth = json.pxWid
        private set
    var pxHeight = json.pxHei
        private set
    var worldX = json.worldX
        private set
    var worldY = json.worldY
        private set
    val bgColorHex = json.bgColor
    val hasBgImage: Boolean
        get() = bgImageInfos != null
    var bgImageInfos: LevelBgImage? = if (json.bgRelPath.isNullOrEmpty() || json.bgPos == null) {
        null
    } else {
        LevelBgImage(
            relFilePath = json.bgRelPath,
            topLeftX = json.bgPos.topLeftPx[0],
            topLeftY = json.bgPos.topLeftPx[1],
            scaleX = json.bgPos.scale[0],
            scaleY = json.bgPos.scale[1],
            cropRect = CropRect(
                x = json.bgPos.cropRect[0],
                y = json.bgPos.cropRect[1],
                w = json.bgPos.cropRect[2],
                h = json.bgPos.cropRect[3]
            )
        )
    }
        private set

    val bgImage: TextureSlice? = bgImageTexture?.let {
        val crop = bgImageInfos!!.cropRect
        TextureSlice(it, crop.x.toInt(), crop.y.toInt(), crop.w.toInt(), crop.h.toInt())
    }

    private val _allUntypedLayers = mutableListOf<LDtkLayer>()
    val layers: List<LDtkLayer>
        get() = _allUntypedLayers

    private var entityLayer: LDtkEntityLayer? = null

    val entities get() = entityLayer?.entities

    private val _neighbors = mutableListOf<Neighbor>()
    val neighors: List<Neighbor>
        get() = _neighbors

    init {
        json.layerInstances?.forEach { layerInstance ->
            instantiateLayer(layerInstance).also { _allUntypedLayers.add(it) }
        }
        json.neighbours?.forEach {
            _neighbors.add(Neighbor(it.levelUid, NeighborDirection.fromDir(it.dir)))
        }
    }

    fun render(batch: SpriteBatch, viewBounds: Rect) {
        bgImageInfos?.let { bgImageInfo ->
            bgImage?.let {
                batch.draw(
                    it,
                    bgImageInfo.topLeftX.toFloat(),
                    bgImageInfo.topLeftY.toFloat(),
                    0f,
                    0f,
                    it.width.toFloat(),
                    it.height.toFloat(),
                    bgImageInfo.scaleX,
                    bgImageInfo.scaleY,
                    0f,
                    false,
                    false
                )
            }
        }
        layers.forEach { it.render(batch, viewBounds) }
    }

    private fun instantiateLayer(json: LayerInstance): LDtkLayer {
        return when (json.type) { //IntGrid, Entities, Tiles or AutoLayer
            "IntGrid" -> {
                val intGridValues = getLayerDef(json.layerDefUid)!!.intGridValues
                LDtkIntGridLayer(intGridValues, json)
            }
            "Entities" -> {
                LDtkEntityLayer(json).apply {
                    instantiateEntities()
                }
            }
            "Tiles" -> {
                LDtkTilesLayer(tilesets, json)
            }
            "AutoLayer" -> {
                LDtkAutoLayer(tilesets, json)
            }
            else -> error("Unable to instantiate layer for level $identifier")
        }
    }

    private fun getLayerDef(uid: Int?, identifier: String? = ""): LayerDefinition? {
        if (uid == null && identifier == null) {
            return null
        }
        return projectJson.defs.layers.find { it.uid == uid || it.identifier == identifier }
    }

    override fun toString(): String {
        return "Level(uid=$uid, identifier='$identifier', pxWidth=$pxWidth, pxHeight=$pxHeight, worldX=$worldX, worldY=$worldY, bgColorHex=$bgColorHex, _allUntypedLayers=$_allUntypedLayers, _neighbors=$_neighbors)"
    }
}