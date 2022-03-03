package com.lehaine.littlekt.file.tiled

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

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
internal data class TiledProperty(val name: String, val type: String, val value: String)

@Serializable
internal data class TiledLayerData(
    val id: Int,
    val draworder: String? = null,
    val data: TiledTileLayerDataValue = TiledTileLayerDataValue(),
    val encoding: String = "",
    val compression:String = "",
    val objects: List<TiledObjectData> = emptyList(),
    val width: Int = 0,
    val height: Int = 0,
    val x: Int,
    val y: Int,
    val image: String? = null,
    val offsetx: Float = 0f,
    val offsety: Float = 0f,
    val type: String,
    val startx: Int = 0,
    val starty: Int = 0,
    val tintcolor: String? = null,
    val properties: List<TiledProperty> = emptyList(),
    val layers: List<TiledLayerData> = emptyList(),
    val name: String,
    val visible: Boolean,
    val opacity: Float
)

@Serializable(with = TiledDataValueSerializer::class)
internal data class TiledTileLayerDataValue(
    val base64: String = "",
    val array: List<Long> = emptyList()
)

private object TiledDataValueSerializer : KSerializer<TiledTileLayerDataValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.lehaine.littlekt.file.tiled.TiledDataValueSerializer") {
            element<List<Long>>("array", isOptional = true)
            element<String>("base64", isOptional = true)
        }

    override fun serialize(encoder: Encoder, value: TiledTileLayerDataValue) {
        throw NotImplementedError("TiledDataValueSerializer serialization is not supported!")
    }

    override fun deserialize(decoder: Decoder): TiledTileLayerDataValue {
        val input = decoder as? JsonDecoder ?: error("Unable to cast to JsonDecoder")
        val json = input.decodeJsonElement()
        if (json is JsonArray) {
            val arrList = json.jsonArray

            return TiledTileLayerDataValue(array = arrList.map { it.jsonPrimitive.content.toLong() })
        }
        return TiledTileLayerDataValue(base64 = json.jsonPrimitive.content)
    }
}

@Serializable
internal data class TiledObjectData(
    val id: Int,
    val gid: Long? = null,
    val name: String,
    val type: String,
    val properties: List<TiledProperty> = emptyList(),
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val visible: Boolean,
    val point: Boolean = false,
    val ellipse: Boolean = false,
    val polygon: List<TiledPointData>? = null,
    val polyline: List<TiledPointData>? = null,
    val text: TiledTextData? = null
)

@Serializable
internal data class TiledTextData(
    val text: String,
    val wrap: Boolean,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strikeout: Boolean,
    val kerning: Boolean,
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
internal data class TiledPointData(val x: Float, val y: Float)

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