package com.lehaine.littlekt.graphics.g2d.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.LDtkLevelBackgroundPositionData
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.calculateViewBounds

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkLevel(
    val uid: Int,
    val identifier: String,
    val iid: String,
    val pxWidth: Int,
    val pxHeight: Int,
    val worldX: Int,
    val worldY: Int,
    val neighbors: List<Neighbor>,
    val layers: List<LDtkLayer>,
    val entities: List<LDtkEntity>,
    val backgroundColor: String,
    levelBackgroundPos: LDtkLevelBackgroundPositionData? = null,
    bgImageTexture: TextureSlice? = null,
) {
    val layersByIdentifier by lazy { layers.associateBy { it.identifier } }
    val layersByIid by lazy { layers.associateBy { it.iid } }

    val entitiesByIdentifier: Map<String, List<LDtkEntity>> by lazy { entities.groupBy { it.identifier } }
    val entitiesByIid: Map<String, LDtkEntity> by lazy { entities.associateBy { it.iid } }

    val hasBgImage: Boolean
        get() = levelBackgroundImage != null

    val backgroundImageSlice: TextureSlice? = bgImageTexture?.let {
        val crop =
            levelBackgroundPos?.cropRect
                ?: error("Unable to read background crop rectangle when it should be available.")
        TextureSlice(it, crop[0].toInt(), crop[1].toInt(), crop[2].toInt(), crop[3].toInt())
    }
    val levelBackgroundImage: LevelBgImage? = if (levelBackgroundPos != null) {
        LevelBgImage(
            topLeftX = levelBackgroundPos.topLeftPx[0],
            topLeftY = levelBackgroundPos.topLeftPx[1],
            scaleX = levelBackgroundPos.scale[0],
            scaleY = levelBackgroundPos.scale[1],
            cropRect = CropRect(
                x = levelBackgroundPos.cropRect[0],
                y = levelBackgroundPos.cropRect[1],
                w = levelBackgroundPos.cropRect[2],
                h = levelBackgroundPos.cropRect[3]
            ),
            slice = backgroundImageSlice
                ?: error("Unable to retrieve background TextureSlice when it should be available.")
        )
    } else {
        null
    }

    private val viewBounds = Rect()

    fun render(
        batch: Batch,
        camera: Camera,
        x: Float = worldX.toFloat(),
        y: Float = worldY.toFloat(),
        scale: Float = 1f,
    ) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale)
    }

    fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float = worldX.toFloat(),
        y: Float = worldY.toFloat(),
        scale: Float = 1f,
    ) {
        levelBackgroundImage?.render(batch, x, y, scale)
        // need to render back to front - layers last in the list need to render first
        for (i in layers.size - 1 downTo 0) {
            layers[i].render(batch, viewBounds, x, y, scale)
        }
    }

    fun entities(name: String): List<LDtkEntity> =
        entitiesByIdentifier[name] ?: error("Entities: '$name' do not exist in this level!")

    fun entity(iid: String): LDtkEntity = entitiesByIid[iid] ?: error("Entity: '$iid' do not exist in this level!")

    fun layer(name: String): LDtkLayer =
        layersByIdentifier[name] ?: error("Layer: '$name' does not exist in this level!")

    operator fun get(layer: String) = layer(layer)

    override fun toString(): String {
        return "Level(uid=$uid, identifier='$identifier', pxWidth=$pxWidth, pxHeight=$pxHeight, worldX=$worldX, worldY=$worldY, backgroundColor=$backgroundColor, layers=$layers, neighbors=$neighbors)"
    }

    enum class NeighborDirection {
        North,
        NorthWest,
        NorthEast,
        South,
        SouthWest,
        SouthEast,
        West,
        East,
        DepthLower,
        DepthGreater,
        Overlap;

        companion object {
            fun fromDir(dir: String): NeighborDirection {
                return when (dir.lowercase()) {
                    "n" -> North
                    "e" -> East
                    "s" -> South
                    "w" -> West
                    "ne" -> NorthEast
                    "se" -> SouthEast
                    "sw" -> SouthWest
                    "nw" -> NorthWest
                    "<" -> DepthLower
                    ">" -> DepthGreater
                    "o" -> Overlap
                    else -> {
                        println("WARNING: unknown neighbor level direction: $dir")
                        North
                    }
                }
            }
        }
    }

    data class Neighbor(val levelUid: Int, val levelIid: String, val dir: NeighborDirection)

    data class CropRect(val x: Float, val y: Float, val w: Float, val h: Float);

    data class LevelBgImage(
        val topLeftX: Int,
        val topLeftY: Int,
        val scaleX: Float,
        val scaleY: Float,
        val cropRect: CropRect,
        val slice: TextureSlice,
    ) {

        fun render(batch: Batch, x: Float, y: Float, scale: Float = 1f) {
            batch.draw(
                slice,
                topLeftX.toFloat() + x,
                topLeftY.toFloat() + y,
                0f,
                0f,
                slice.width.toFloat(),
                slice.height.toFloat(),
                scaleX * scale,
                scaleY * scaleY,
                Angle.ZERO,
                flipX = false,
                flipY = false
            )
        }
    }
}