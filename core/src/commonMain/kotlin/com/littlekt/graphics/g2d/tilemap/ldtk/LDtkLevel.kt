package com.littlekt.graphics.g2d.tilemap.ldtk

import com.littlekt.file.ldtk.LDtkLevelBackgroundPositionData
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.Rect
import com.littlekt.math.geom.Angle
import com.littlekt.util.calculateViewBounds
import com.littlekt.util.datastructure.fastForEach

/**
 * Level info related to an LDtk level.
 *
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
    val backgroundColor: Color,
    levelBackgroundPos: LDtkLevelBackgroundPositionData? = null,
    bgImageTexture: TextureSlice? = null,
) {
    val layersByIdentifier by lazy { layers.associateBy { it.identifier } }
    val layersByIid by lazy { layers.associateBy { it.iid } }

    val entitiesByIdentifier: Map<String, List<LDtkEntity>> by lazy {
        entities.groupBy { it.identifier }
    }
    val entitiesByIid: Map<String, LDtkEntity> by lazy { entities.associateBy { it.iid } }

    val hasBgImage: Boolean
        get() = levelBackgroundImage != null

    val backgroundImageSlice: TextureSlice? =
        bgImageTexture?.let {
            val crop =
                levelBackgroundPos?.cropRect
                    ?: error(
                        "Unable to read background crop rectangle when it should be available."
                    )
            TextureSlice(it, crop[0].toInt(), crop[1].toInt(), crop[2].toInt(), crop[3].toInt())
        }
    val levelBackgroundImage: LevelBgImage? =
        if (levelBackgroundPos != null) {
            LevelBgImage(
                topLeftX = levelBackgroundPos.topLeftPx[0],
                topLeftY = levelBackgroundPos.topLeftPx[1],
                scaleX = levelBackgroundPos.scale[0],
                scaleY = levelBackgroundPos.scale[1],
                cropRect =
                    CropRect(
                        x = levelBackgroundPos.cropRect[0],
                        y = levelBackgroundPos.cropRect[1],
                        w = levelBackgroundPos.cropRect[2],
                        h = levelBackgroundPos.cropRect[3]
                    ),
                slice =
                    backgroundImageSlice
                        ?: error(
                            "Unable to retrieve background TextureSlice when it should be available."
                        )
            )
        } else {
            null
        }

    private val viewBounds = Rect()

    fun addToCache(cache: SpriteCache, x: Float = 0f, y: Float = 0f, scale: Float = 1f) {
        levelBackgroundImage?.addToCache(cache, x, y, scale)
        for (i in layers.size - 1 downTo 0) {
            layers[i].addToCache(cache, x, y, scale)
        }
    }

    fun removeFromCache(cache: SpriteCache) {
        levelBackgroundImage?.removeFromCache(cache)
        layers.fastForEach { it.removeFromCache(cache) }
    }

    fun render(
        batch: Batch,
        camera: Camera,
        x: Float,
        y: Float,
        scale: Float = 1f,
    ) {
        viewBounds.calculateViewBounds(camera)
        render(batch, viewBounds, x, y, scale)
    }

    fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float = 1f,
    ) {
        levelBackgroundImage?.render(batch, viewBounds, x, y, scale)
        // need to render back to front - layers last in the list need to render first
        for (i in layers.size - 1 downTo 0) {
            layers[i].render(batch, viewBounds, x, y, scale)
        }
    }

    fun render(
        batch: Batch,
        camera: Camera,
        scale: Float = 1f,
    ) = render(batch, camera, worldX * scale, worldY * scale, scale)

    fun render(
        batch: Batch,
        viewBounds: Rect,
        scale: Float = 1f,
    ) = render(batch, viewBounds, worldX * scale, worldY * scale, scale)

    fun entities(name: String): List<LDtkEntity> =
        entitiesByIdentifier[name] ?: error("Entities: '$name' do not exist in this level!")

    fun entity(iid: String): LDtkEntity =
        entitiesByIid[iid] ?: error("Entity: '$iid' do not exist in this level!")

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

    data class CropRect(val x: Float, val y: Float, val w: Float, val h: Float)

    data class LevelBgImage(
        val topLeftX: Int,
        val topLeftY: Int,
        val scaleX: Float,
        val scaleY: Float,
        val cropRect: CropRect,
        val slice: TextureSlice,
    ) {
        private var cacheId: Int = -1

        fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float = 1f) {
            check(cacheId == -1) {
                "LDtk LevelBGImage has already been added to a cache with an id of: $cacheId"
            }
            cacheId =
                cache.add(slice) {
                    position.set(
                        topLeftX * scale + x + slice.width * 0.5f * scale * scaleX,
                        topLeftY * scale + y + slice.height * 0.5f * scale * scaleY
                    )
                    this.scale.set(scaleX * scale, scaleY * scale)
                }
        }

        fun removeFromCache(cache: SpriteCache) {
            if (cacheId != -1) {
                cache.remove(cacheId)
                cacheId = -1
            }
        }

        fun render(batch: Batch, bounds: Rect, x: Float, y: Float, scale: Float = 1f) {
            if (
                bounds.intersects(
                    topLeftX * scale + x,
                    topLeftY * scale + y,
                    topLeftX * scale + x + slice.width * scaleX * scale,
                    topLeftY * scale + y + slice.height * scaleY * scale,
                )
            ) {
                batch.draw(
                    slice = slice,
                    x = topLeftX * scale + x,
                    y = topLeftY * scale + y,
                    originX = 0f,
                    originY = 0f,
                    width = slice.width.toFloat(),
                    height = slice.height.toFloat(),
                    scaleX = scaleX * scale,
                    scaleY = scaleY * scale,
                    rotation = Angle.ZERO,
                    flipX = false,
                    flipY = false
                )
            }
        }
    }
}
