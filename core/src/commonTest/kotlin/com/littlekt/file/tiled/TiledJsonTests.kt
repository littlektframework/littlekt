package com.littlekt.file.tiled

import com.littlekt.file.Base64.decodeFromBase64
import com.littlekt.file.toIntArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class TiledJsonTests {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private fun map(layers: String = "", tilesets: String = "", properties: String = "") =
        json.decodeFromString<TiledMapData>(
            """
            {
              "compressionlevel": -1,
              "orientation": "orthogonal",
              "renderorder": "right-down",
              "width": 4,
              "height": 3,
              "tilewidth": 16,
              "tileheight": 8,
              "infinite": false,
              "nextlayerid": 2,
              "layers": [$layers],
              "tilesets": [$tilesets],
              "properties": [$properties],
              "type": "map",
              "version": "1.10"
            }
            """
                .trimIndent()
        )

    private fun layer(type: String, extra: String) =
        """{"id": 1, "name": "l", "type": "$type", "x": 0, "y": 0, "visible": true, "opacity": 1, $extra}"""

    private fun objectLayer(vararg objects: String) =
        map(layers = layer("objectgroup", """"draworder": "topdown", "objects": [${objects.joinToString(",")}]"""))
            .layers[0]
            .objects

    private fun obj(extra: String = "") =
        """{"id": 3, "name": "o", "type": "", "x": 1, "y": 2, "width": 4, "height": 5, "rotation": 0, "visible": true${if (extra.isEmpty()) "" else ", $extra"}}"""

    @Test
    fun test_decode_map_header() {
        val data = map()
        assertEquals("orthogonal", data.orientation)
        assertEquals("right-down", data.renderorder)
        assertEquals(4, data.width)
        assertEquals(3, data.height)
        assertEquals(16, data.tilewidth)
        assertEquals(8, data.tileheight)
        assertFalse(data.infinite)
    }

    @Test
    fun test_decode_map_defaults() {
        val data = map()
        assertNull(data.backgroundColor)
        assertNull(data.staggeraxis)
        assertNull(data.staggerindex)
        assertEquals(0, data.hexsidelength)
    }

    @Test
    fun test_decode_tile_layer_csv_array() {
        val tileLayer =
            map(layers = layer("tilelayer", """"width": 2, "height": 2, "data": [1, 0, 2, 3]""")).layers[0]
        assertEquals("tilelayer", tileLayer.type)
        assertEquals(listOf(1L, 0L, 2L, 3L), tileLayer.data.array)
        assertEquals("", tileLayer.data.base64)
        assertEquals("", tileLayer.encoding)
    }

    @Test
    fun test_decode_tile_layer_gid_with_flip_flags() {
        val tileLayer =
            map(layers = layer("tilelayer", """"width": 2, "height": 1, "data": [2147483649, 3221225474]""")).layers[0]
        val gids = tileLayer.data.array.map { it.toInt() }
        assertEquals(0x80000001.toInt(), gids[0])
        assertEquals(0xC0000002.toInt(), gids[1])
    }

    @Test
    fun test_decode_tile_layer_base64_data() {
        val tileLayer =
            map(
                    layers =
                        layer(
                            "tilelayer",
                            """"width": 2, "height": 1, "encoding": "base64", "data": "AQAAAAEAAIA=""""
                        )
                )
                .layers[0]
        assertEquals("base64", tileLayer.encoding)
        assertEquals("", tileLayer.compression)
        assertTrue(tileLayer.data.array.isEmpty())
        assertContentEquals(
            intArrayOf(1, 0x80000001.toInt()),
            tileLayer.data.base64.decodeFromBase64().toIntArray()
        )
    }

    @Test
    fun test_decode_layer_offsets_and_tint() {
        val data =
            map(layers = layer("imagelayer", """"image": "bg.png", "offsetx": 2.5, "offsety": -4, "tintcolor": "#ff0000""""))
                .layers[0]
        assertEquals("bg.png", data.image)
        assertEquals(2.5f, data.offsetx)
        assertEquals(-4f, data.offsety)
        assertEquals("#ff0000", data.tintcolor)
    }

    @Test
    fun test_decode_group_layer_children() {
        val group =
            map(layers = layer("group", """"layers": [${layer("tilelayer", """"data": [1]""")}]""")).layers[0]
        assertEquals("group", group.type)
        assertEquals(1, group.layers.size)
        assertEquals(listOf(1L), group.layers[0].data.array)
    }

    @Test
    fun test_decode_object_layer_draw_order() {
        val data = map(layers = layer("objectgroup", """"draworder": "index", "objects": []""")).layers[0]
        assertEquals("index", data.draworder)
        assertTrue(data.objects.isEmpty())
    }

    @Test
    fun test_decode_rectangle_object() {
        val o = objectLayer(obj()).single()
        assertEquals(3, o.id)
        assertEquals(1f, o.x)
        assertEquals(2f, o.y)
        assertEquals(4f, o.width)
        assertEquals(5f, o.height)
        assertFalse(o.point)
        assertFalse(o.ellipse)
        assertNull(o.polygon)
        assertNull(o.polyline)
        assertNull(o.text)
        assertNull(o.gid)
    }

    @Test
    fun test_decode_point_and_ellipse_objects() {
        val objects = objectLayer(obj(""""point": true"""), obj(""""ellipse": true"""))
        assertTrue(objects[0].point)
        assertTrue(objects[1].ellipse)
    }

    @Test
    fun test_decode_polygon_object() {
        val o = objectLayer(obj(""""polygon": [{"x": 0, "y": 0}, {"x": 8, "y": 0}, {"x": 4, "y": -6}]""")).single()
        val polygon = assertNotNull(o.polygon)
        assertEquals(listOf(TiledPointData(0f, 0f), TiledPointData(8f, 0f), TiledPointData(4f, -6f)), polygon)
    }

    @Test
    fun test_decode_polyline_object() {
        val o = objectLayer(obj(""""polyline": [{"x": 1.5, "y": 2}]""")).single()
        assertEquals(listOf(TiledPointData(1.5f, 2f)), o.polyline)
    }

    @Test
    fun test_decode_tile_object_gid_with_flip_flag() {
        val o = objectLayer(obj(""""gid": 2147483653""")).single()
        assertEquals(2147483653L, o.gid)
        assertEquals(0x80000005.toInt(), o.gid?.toInt())
    }

    @Test
    fun test_decode_full_text_object() {
        val o =
            objectLayer(
                    obj(
                        """"text": {"text": "hi", "wrap": true, "bold": true, "italic": false, "underline": false, "strikeout": false, "kerning": true, "color": "#00ff00", "halign": "center", "valign": "bottom", "pixelsize": 12, "fontfamily": "serif"}"""
                    )
                )
                .single()
        val text = assertNotNull(o.text)
        assertEquals("hi", text.text)
        assertTrue(text.wrap)
        assertTrue(text.bold)
        assertEquals("#00ff00", text.color)
        assertEquals("center", text.halign)
        assertEquals("bottom", text.valign)
        assertEquals(12, text.pixelsize)
        assertEquals("serif", text.fontfamily)
    }

    @Test
    fun test_decode_minimal_text_object_uses_tiled_defaults() {
        val text = assertNotNull(objectLayer(obj(""""text": {"text": "hello", "wrap": true}""")).single().text)
        assertEquals("hello", text.text)
        assertTrue(text.wrap)
        assertFalse(text.bold)
        assertFalse(text.italic)
        assertFalse(text.underline)
        assertFalse(text.strikeout)
        assertTrue(text.kerning)
        assertEquals("#000000", text.color)
        assertEquals("left", text.halign)
        assertEquals("top", text.valign)
        assertEquals(16, text.pixelsize)
        assertEquals("sans-serif", text.fontfamily)
    }

    @Test
    fun test_decode_string_property() {
        val data = map(properties = """{"name": "title", "type": "string", "value": "Level 1"}""")
        assertEquals(TiledProperty("title", "string", "Level 1"), data.properties.single())
    }

    @Test
    fun test_decode_non_string_property_values_as_strings() {
        val data =
            map(
                properties =
                    """
                    {"name": "hp", "type": "int", "value": 5},
                    {"name": "speed", "type": "float", "value": 1.5},
                    {"name": "solid", "type": "bool", "value": true},
                    {"name": "tint", "type": "color", "value": "#ff00ff00"},
                    {"name": "ref", "type": "object", "value": 12}
                    """
                        .trimIndent()
            )
        assertEquals(listOf("5", "1.5", "true", "#ff00ff00", "12"), data.properties.map { it.value })
    }

    @Test
    fun test_decode_layer_properties() {
        val data = map(layers = layer("tilelayer", """"data": [], "properties": [{"name": "z", "type": "int", "value": 3}]""")).layers[0]
        assertEquals(TiledProperty("z", "int", "3"), data.properties.single())
    }

    @Test
    fun test_decode_tileset_reference() {
        val tileset = map(tilesets = """{"firstgid": 17, "source": "tiles.tsj"}""").tilesets.single()
        assertEquals(17, tileset.firstgid)
        assertEquals("tiles.tsj", tileset.source)
        assertEquals("unspecified", tileset.objectalignment)
    }

    @Test
    fun test_decode_tileset_data() {
        val tileset =
            json.decodeFromString<TiledTilesetData>(
                """
                {
                  "columns": 8,
                  "image": "tiles.png",
                  "imagewidth": 128,
                  "imageheight": 64,
                  "margin": 1,
                  "spacing": 2,
                  "tilecount": 32,
                  "tilewidth": 16,
                  "tileheight": 16,
                  "tileoffset": {"x": 2, "y": -3},
                  "tiles": [
                    {
                      "id": 4,
                      "animation": [{"tileid": 4, "duration": 100}, {"tileid": 5, "duration": 150}],
                      "properties": [{"name": "water", "type": "bool", "value": true}]
                    }
                  ]
                }
                """
                    .trimIndent()
            )
        assertEquals(8, tileset.columns)
        assertEquals("tiles.png", tileset.image)
        assertEquals(1, tileset.margin)
        assertEquals(2, tileset.spacing)
        assertEquals(TiledOffsetData(2, -3), tileset.tileoffset)
        val tile = tileset.tiles.single()
        assertEquals(4, tile.id)
        assertEquals(listOf(TiledTileFrame(100, 4), TiledTileFrame(150, 5)), tile.animation)
        assertEquals("true", tile.properties.single().value)
    }

    @Test
    fun test_decode_tile_object_group() {
        val tile =
            json.decodeFromString<TiledTileData>(
                """{"id": 0, "objectgroup": ${layer("objectgroup", """"objects": [${obj()}]""")}}"""
            )
        assertEquals(1, tile.objectgroup?.objects?.size)
    }
}
