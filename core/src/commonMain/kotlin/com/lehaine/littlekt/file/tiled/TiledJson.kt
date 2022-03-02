package com.lehaine.littlekt.file.tiled

import kotlinx.serialization.Serializable

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
@Serializable
internal data class TiledMapData(
    val backgroundColor: String? = null,
    val orientation: String,
    val renderorder: String,
    val layers: List<TiledLayerData> = emptyList(),
    val width: Int,
    val height: Int,
    val properties: List<TiledProperty> = emptyList(),
    val staggeraxis: String? = null,
    val staggerindex: String? = null,
    val hexsidelength: Int = 0,
    val infinite: Boolean = false,
    val tilewidth: Int,
    val tileheight: Int,
    val tilesets: List<TiledTilesetData> = emptyList()
)

@Serializable
internal data class TiledProperty(val key: String, val type: String, val value: String)

@Serializable
internal data class TiledLayerData(
    val id: Int,
    val draworder: String? = null,
    val data: List<Long> = emptyList(),
    val objects: List<TiledObjectData> = emptyList(),
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val image: String? = null,
    val offsetx: Float = 0f,
    val offsety: Float = 0f,
    val type: String,
    val startx: Int = 0,
    val starty: Int = 0,
    val tintColor: String? = null,
    val properties: List<TiledProperty> = emptyList(),
    val name: String,
    val visible: Boolean,
    val opacity: Float
)

@Serializable
internal data class TiledObjectData(
    val id: Int,
    val gid: Int,
    val name: String,
    val properties: List<TiledProperty> = emptyList(),
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val visible: Boolean,
    val point: Boolean,
    val ellipse: Boolean,
    val text: TiledTextData? = null
)

@Serializable
internal data class TiledTextData(
    val text: String,
    val wrap: Boolean,
    val bold: Boolean,
    val color: String,
    val halign: String,
    val valign: String,
    val pixelsize: Int,
    val fontfamily: String
)

@Serializable
internal data class TiledTilesetData(
    val firstgid: Int = 0,
    val columns: Int = 0,
    val source: String = "",
    val tilewidth: Int = 0,
    val tileheight: Int = 0,
    val tileoffset: TiledOffsetData? = null,
    val wangsets: List<TiledWangSetData> = emptyList(),
    val image: String = "",
    val imagewidth: Int = 0,
    val imageheight: Int = 0,
    val margin: Int = 0,
    val spacing: Int = 0,
    val grid: TiledGridData? = null,
    val objectalignment: String = "unspecified",
    val terrains: List<TiledTerrainData> = emptyList(),
    val tiles: List<TiledTileData> = emptyList()
)

@Serializable
internal data class TiledOffsetData(val x: Int, val y: Int)

@Serializable
internal data class TiledGridData(val width: Int, val height: Int, val orientation: String)

@Serializable
internal data class TiledTerrainData(val name: String, val properties: List<TiledProperty> = emptyList(), val tile: Int)

@Serializable
internal data class TiledTileData(
    val animation: List<TiledTileFrame> = emptyList(),
    val id: Int,
    val imageheight: Int = 0,
    val imagewidth: Int = 0,
    val objectgroup: TiledLayerData? = null,
    val probability: Float = 0f,
    val properties: List<TiledProperty> = emptyList(),
    val terrain: List<Int> = emptyList(),
    val type: String = ""
)

@Serializable
internal data class TiledTileFrame(val duration: Int, val tileid: Int)

@Serializable
internal data class TiledWangSetData(
    val colors: List<TiledWangColorData> = emptyList(),
    val name: String,
    val properties: List<TiledProperty> = emptyList(),
    val type: String,
    val wangtiles: List<TiledWangTileData> = emptyList()
)

@Serializable
internal data class TiledWangColorData(
    val color: String,
    val name: String,
    val tile: Int,
    val properties: List<TiledProperty> = emptyList()
)

@Serializable
internal data class TiledWangTileData(val tileid: Int, val wangid: List<Int>)