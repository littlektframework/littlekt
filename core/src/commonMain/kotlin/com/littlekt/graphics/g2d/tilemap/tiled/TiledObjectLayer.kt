package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.SpriteCache
import com.littlekt.graphics.g2d.shape.JoinType
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.littlekt.math.Rect

/**
 * A Tiled "Object" layer.
 *
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledObjectLayer(
    type: String,
    name: String,
    id: Int,
    visible: Boolean,
    width: Int,
    height: Int,
    offsetX: Float,
    offsetY: Float,
    tileWidth: Int,
    tileHeight: Int,
    tintColor: Color?,
    opacity: Float,
    properties: Map<String, TiledMap.Property>,
    val drawOrder: TiledMap.Object.DrawOrder?,
    val objects: List<TiledMap.Object>,
    private val tiles: Map<Int, TiledTileset.Tile>
) :
    TiledLayer(
        type,
        name,
        id,
        visible,
        width,
        height,
        offsetX,
        offsetY,
        tileWidth,
        tileHeight,
        tintColor,
        opacity,
        properties
    ) {

    private val flipData = TileData()

    val objectsById by lazy { objects.associateBy { it.id } }
    val objectsByName by lazy { objects.associateBy { it.name } }
    val objectsByType by lazy { objects.groupBy { it.type } }

    private val objectDrawOuterColor = Color.LIGHT_GRAY.withAlpha(0.5f)
    private val objectDrawInnerColor = Color.LIGHT_GRAY.withAlpha(0.05f)

    override fun render(
        batch: Batch,
        viewBounds: Rect,
        x: Float,
        y: Float,
        scale: Float,
        displayObjects: Boolean,
        shapeRenderer: ShapeRenderer?
    ) {
        if (!displayObjects || !visible) return

        objects.forEach { obj ->
            if (!obj.visible) return@forEach
            val objX = obj.x * scale + offsetX * scale + x
            val objY = obj.y * scale + offsetY * scale + y
            if (shapeRenderer != null && obj.gid == null) {
                when (val shape = obj.shape) {
                    is TiledMap.Object.Shape.Rectangle -> {
                        shapeRenderer.filledRectangle(
                            objX,
                            objY - shape.height * scale,
                            shape.width * scale,
                            shape.height * scale,
                            obj.rotation,
                            color = objectDrawInnerColor
                        )
                        shapeRenderer.rectangle(
                            objX,
                            objY - shape.height * scale,
                            shape.width * scale,
                            shape.height * scale,
                            obj.rotation,
                            thickness = scale,
                            color = objectDrawOuterColor
                        )
                    }
                    is TiledMap.Object.Shape.Ellipse -> {

                        shapeRenderer.filledEllipse(
                            objX + shape.width * 0.5f * scale,
                            objY - shape.height * 0.5f * scale,
                            shape.width * 0.5f * scale,
                            shape.height * 0.5f * scale,
                            obj.rotation,
                            innerColor = objectDrawInnerColor,
                            outerColor = objectDrawInnerColor
                        )
                        shapeRenderer.ellipse(
                            objX + shape.width * 0.5f * scale,
                            objY - shape.height * 0.5f * scale,
                            shape.width * 0.5f * scale,
                            shape.height * 0.5f * scale,
                            thickness = scale,
                            color = objectDrawOuterColor
                        )
                    }
                    is TiledMap.Object.Shape.Polygon -> {
                        val vertices = FloatArray(shape.points.size * 2)
                        var index = 0
                        shape.points.forEach { point ->
                            vertices[index++] = objX + point.x * scale
                            vertices[index++] = objY + point.y * scale
                        }
                        shapeRenderer.filledPolygon(vertices, color = objectDrawInnerColor)
                        shapeRenderer.path(
                            vertices,
                            joinType = JoinType.SMOOTH,
                            thickness = scale,
                            color = objectDrawOuterColor,
                            open = false
                        )
                    }
                    TiledMap.Object.Shape.Point -> {
                        shapeRenderer.filledEllipse(
                            objX,
                            objY,
                            5f * scale,
                            innerColor = objectDrawInnerColor,
                            outerColor = objectDrawInnerColor
                        )
                        shapeRenderer.ellipse(
                            objX,
                            objY,
                            5f * scale,
                            thickness = scale,
                            color = objectDrawOuterColor
                        )
                    }
                    is TiledMap.Object.Shape.Polyline -> {
                        val vertices = FloatArray(shape.points.size * 2)
                        var index = 0
                        shape.points.forEach { point ->
                            vertices[index++] = objX + point.x * scale
                            vertices[index++] = objY + point.y * scale
                        }
                        shapeRenderer.path(
                            vertices,
                            joinType = JoinType.SMOOTH,
                            thickness = scale,
                            color = objectDrawOuterColor
                        )
                    }
                    is TiledMap.Object.Shape.Text -> {
                        error(
                            "Text not supported! Delete the text or hide it in Tiled to get rid of this error!"
                        )
                    }
                }
            }

            obj.gid?.let { gid ->
                val tileData = gid.toInt().bitsToTileData(flipData)
                tiles[tileData.id]?.let {
                    batch.draw(
                        slice = it.slice,
                        x = objX + it.offsetX * scale,
                        y = objY + it.offsetY * scale,
                        originX = 0f,
                        originY = 0f,
                        width = obj.bounds.width,
                        height = obj.bounds.height,
                        scaleX = scale,
                        scaleY = scale,
                        rotation = obj.rotation,
                        flipX = tileData.flipX,
                        flipY = tileData.flipY,
                        color = tintColor ?: Color.WHITE
                    )
                }
            }
        }
    }

    override fun addToCache(cache: SpriteCache, x: Float, y: Float, scale: Float) {
        // tiled object layers not supported by cache
    }

    fun getById(id: Int): TiledMap.Object =
        objectsById[id] ?: error("Object: '$id' does not exist in this layer!")

    fun getByName(name: String): TiledMap.Object =
        objectsByName[name] ?: error("Object: '$name' does not exist in this layer!")

    fun getByType(type: String): List<TiledMap.Object> = objects.filter { it.type == type }

    operator fun get(name: String) = getByName(name)

    operator fun get(id: Int) = getById(id)
}
