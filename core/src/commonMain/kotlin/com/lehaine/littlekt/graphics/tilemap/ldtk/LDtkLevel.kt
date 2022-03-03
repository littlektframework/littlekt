package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.LevelBackgroundPositionData
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureSlice
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
    val pxWidth: Int,
    val pxHeight: Int,
    val worldX: Int,
    val worldY: Int,
    val neighbors: List<Neighbor>,
    val layers: List<LDtkLayer>,
    val entities: List<LDtkEntity>,
    val backgroundColor: String,
    levelBackgroundPos: LevelBackgroundPositionData? = null,
    bgImageTexture: Texture? = null
) {
    val layersMap = layers.associateBy { it.identifier }
    val entitiesMap: Map<String, List<LDtkEntity>> = entities.groupBy { it.identifier }

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

    fun render(batch: Batch, camera: Camera, x: Float = worldX.toFloat(), y: Float = worldY.toFloat()) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y)
    }

    fun render(batch: Batch, viewBounds: Rect, x: Float = worldX.toFloat(), y: Float = worldY.toFloat()) {
        levelBackgroundImage?.render(batch, x, y)
        // need to render back to front - layers last in the list need to render first
        for (i in layers.size - 1 downTo 0) {
            layers[i].render(batch, viewBounds, x, y)
        }
    }

    fun entities(name: String): List<LDtkEntity> =
        entitiesMap[name] ?: error("Entities: '$name' do not exist in this level!")

    fun layer(name: String): LDtkLayer = layersMap[name] ?: error("Layer: '$name' does not exist in this level!")

    operator fun get(layer: String) = layer(layer)

    override fun toString(): String {
        return "Level(uid=$uid, identifier='$identifier', pxWidth=$pxWidth, pxHeight=$pxHeight, worldX=$worldX, worldY=$worldY, backgroundColor=$backgroundColor, layers=$layers, neighbors=$neighbors)"
    }

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
        val topLeftX: Int,
        val topLeftY: Int,
        val scaleX: Float,
        val scaleY: Float,
        val cropRect: CropRect,
        val slice: TextureSlice
    ) {

        fun render(batch: Batch, x: Float, y: Float) {
            batch.draw(
                slice,
                topLeftX.toFloat() + x,
                topLeftY.toFloat() + y,
                0f,
                0f,
                slice.width.toFloat(),
                slice.height.toFloat(),
                scaleX,
                scaleY,
                Angle.ZERO,
                flipX = false,
                flipY = false
            )
        }
    }
}