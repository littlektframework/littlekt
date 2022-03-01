package com.lehaine.littlekt.file.tiled

import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.sliceWithBorder
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledLayer
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledMap
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledTilesLayer
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledTileset
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledMapLoader internal constructor(private val root: VfsFile, private val mapData: TiledMapData) {

    suspend fun loadMap(): TiledMap {
        val tileSets = mapData.tilesets.map { loadTileSet(it.firstgid, it.source) }
        val tiles = tileSets.flatMap { it.tiles }.associateBy { it.id }

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
        tiles: Map<Int, TiledTileset.Tile>
    ): TiledLayer {
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
                tiles = tiles
            )
            "objectgroup" -> TODO()
            "imagelayer" -> TODO()
            "group" -> TODO()
            else -> error("Unsupported TiledLayer '${layerData.type}")
        }
    }

    private suspend fun loadTileSet(gid: Int, source: String): TiledTileset {
        val tilesetData = root[source].decodeFromString<TiledTilesetData>()
        val texture = root[tilesetData.image].readTexture()
        val slices = texture.sliceWithBorder(root.vfs.context, tilesetData.tilewidth, tilesetData.tileheight)

        return TiledTileset(
            tileWidth = tilesetData.tilewidth,
            tileHeight = tilesetData.tileheight,
            tiles = slices.mapIndexed { index, slice ->
                val tileData = tilesetData.tiles.firstOrNull { it.id == index }

                TiledTileset.Tile(
                    slice,
                    index + gid,
                    tileData?.animation?.map {
                        TiledTileset.AnimatedTile(
                            slices[it.tileid],
                        it.tileid + gid,
                            it.duration.milliseconds
                        )
                    }
                        ?: emptyList(),
                    tileData?.properties?.toTiledMapProperty() ?: emptyMap()
                )
            }
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