package com.lehaine.littlekt.file.tiled

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledLayer
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledMap
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledTilesLayer
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledTileset

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledMapLoader internal constructor(private val root: VfsFile, private val mapData: TiledMapData) {

    suspend fun loadMap(): TiledMap {
        val tileSets = mapData.tilesets.map { loadTileSet(it.firstgid, it.source) }
        val tiles = tileSets.flatMap { it.tiles }

        return TiledMap(
            backgroundColor = mapData.backgroundColor?.let { Color.fromHex(it) },
            orientation = mapData.orientation.toOrientation(),
            renderOrder = mapData.renderorder.toRenderOrder(),
            staggerAxis = mapData.staggeraxis?.toStaggerAxis(),
            staggerIndex = mapData.staggerindex?.toStaggerIndex(),
            layers = mapData.layers.map { instantiateLayer(mapData, it, tiles) },
            width = mapData.width,
            height = mapData.height,
            properties = mapData.properties.toTiledMapProperty(),
            tileWidth = mapData.tilewidth,
            tileHeight = mapData.tileheight,
            tileSets = tileSets
        )
    }

    private fun instantiateLayer(
        mapData: TiledMapData,
        layerData: TiledLayerData,
        tiles: List<TextureSlice>
    ): TiledLayer {
        println(layerData.name)
        println(tiles.size)
        val layerTiles = mutableMapOf<Int, TextureSlice>().apply {
            layerData.data.forEach {
                if (it > 0) {
                    put(it, tiles[it - 1])
                }
            }
        }
        return when (layerData.type) {
            "tilelayer" -> TiledTilesLayer(
                type = layerData.type,
                name = layerData.name,
                id = layerData.id,
                width = layerData.width,
                height = layerData.height,
                offsetX = layerData.offsetx,
                offsetY = layerData.offsety,
                tileWidth = mapData.tilewidth,
                tileHeight = mapData.tileheight,
                tintColor = layerData.tintColor?.let { Color.fromHex(it) },
                opacity = layerData.opacity,
                properties = layerData.properties.toTiledMapProperty(),
                tileData = layerData.data.toIntArray(),
                tiles = layerTiles
            )
            "objectgroup" -> TODO()
            "imagelayer" -> TODO()
            "group" -> TODO()
            else -> error("Unsupported TiledLayer '${layerData.type}")
        }
    }

    private suspend fun loadTileSet(gid: Int, source: String): TiledTileset {
        val tiledData = root[source].decodeFromString<TiledTilesetData>()
        val texture = root[tiledData.image].readTexture()
        return TiledTileset(
            tileWidth = tiledData.tilewidth,
            tileHeight = tiledData.tileheight,
            tiles = texture.sliceWithBorder(root.vfs.context, tiledData.tilewidth, tiledData.tileheight)
        )
    }

    private fun List<TiledProperty>.toTiledMapProperty() = associateBy(keySelector = { it.key }) {
        when (it.type) {
            "string" -> TiledMap.Property.StringProp(it.value)
            "int" -> TiledMap.Property.IntProp(it.value.toIntOrNull() ?: 0)
            "float" -> TiledMap.Property.FloatProp(it.value.toFloatOrNull() ?: 0f)
            "bool" -> TiledMap.Property.BoolProp(it.value == "true")
            "color" -> TiledMap.Property.ColorProp(Color.fromHex(it.value))
            "file" -> TiledMap.Property.FileProp(it.value)
            "object" -> TiledMap.Property.ObjectProp(it.value.toIntOrNull() ?: 0)
            else -> TiledMap.Property.StringProp(it.value)
        }
    }

    private fun String.toOrientation() = when (this) {
        "orthogonal" -> TiledMap.Orientation.ORTHOGONAL
        "isometric" -> TiledMap.Orientation.ISOMETRIC
        "staggered" -> TiledMap.Orientation.STAGGERED
        "hexagonal" -> TiledMap.Orientation.HEXAGONAL
        else -> error("Unsupported TiledMap orientation: '$this'")
    }

    private fun String.toRenderOrder() = when (this) {
        "right-down" -> TiledMap.RenderOrder.RIGHT_DOWN
        "right-up" -> TiledMap.RenderOrder.RIGHT_UP
        "left-down" -> TiledMap.RenderOrder.LEFT_DOWN
        "left-up" -> TiledMap.RenderOrder.LEFT_UP
        else -> TiledMap.RenderOrder.RIGHT_DOWN
    }

    private fun String.toStaggerAxis() = when (this) {
        "x" -> TiledMap.StaggerAxis.X
        "y" -> TiledMap.StaggerAxis.Y
        else -> null
    }

    private fun String.toStaggerIndex() = when (this) {
        "even" -> TiledMap.StaggerIndex.EVEN
        "odd" -> TiledMap.StaggerIndex.ODD
        else -> null
    }
}