package com.littlekt.file.ldtk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class LDtkJsonTests {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    private fun field(value: String) =
        json.decodeFromString<LDtkFieldInstance>(
            """{"__identifier": "f", "__type": "Any", "__value": $value, "defUid": 1}"""
        )

    @Test
    fun test_field_value_primitive_content() {
        val value = field("12").value
        assertEquals("12", value?.content)
        assertNull(value?.stringList)
        assertNull(value?.stringMap)
    }

    @Test
    fun test_field_value_string_content() {
        assertEquals("hello", field("\"hello\"").value?.content)
    }

    @Test
    fun test_field_value_bool_content() {
        assertEquals("true", field("true").value?.content)
    }

    @Test
    fun test_field_value_null() {
        assertNull(field("null").value)
    }

    @Test
    fun test_field_value_primitive_array() {
        assertEquals(listOf("1", "2", "a"), field("[1, 2, \"a\"]").value?.stringList)
    }

    @Test
    fun test_field_value_empty_array() {
        assertEquals(emptyList(), field("[]").value?.stringList)
    }

    @Test
    fun test_field_value_point_object() {
        assertEquals(mapOf("cx" to "3", "cy" to "4"), field("""{"cx": 3, "cy": 4}""").value?.stringMap)
    }

    @Test
    fun test_field_value_object_skips_nested_values() {
        val map = field("""{"entityIid": "abc", "nested": {"x": 1}, "list": [1]}""").value?.stringMap
        assertEquals(mapOf("entityIid" to "abc"), map)
    }

    @Test
    fun test_field_value_point_array() {
        val list = field("""[{"cx": 1, "cy": 2}, {"cx": 5, "cy": 6}]""").value?.stringMapList
        assertEquals(listOf(mapOf("cx" to "1", "cy" to "2"), mapOf("cx" to "5", "cy" to "6")), list)
    }

    @Test
    fun test_field_instance_tile() {
        val instance =
            json.decodeFromString<LDtkFieldInstance>(
                """{"__identifier": "icon", "__type": "Tile", "__value": null, "defUid": 2, "__tile": {"tilesetUid": 3, "x": 16, "y": 32, "w": 8, "h": 8}}"""
            )
        assertEquals(LDtkTileRect(tilesetUid = 3, h = 8, w = 8, x = 16, y = 32), instance.tile)
    }

    @Test
    fun test_tile_rect_defaults() {
        val rect = json.decodeFromString<LDtkTileRect>("""{"tilesetUid": 1}""")
        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(0, rect.w)
        assertEquals(0, rect.h)
    }

    @Test
    fun test_tile_instance_alpha_default() {
        val tile = json.decodeFromString<LDtkTileInstance>("""{"d": [0], "f": 3, "px": [16, 8], "src": [32, 0], "t": 2}""")
        assertEquals(1f, tile.a)
        assertEquals(3, tile.f)
        assertEquals(listOf(16, 8), tile.px)
        assertEquals(listOf(32, 0), tile.src)
        assertEquals(2, tile.t)
    }

    @Test
    fun test_layer_type_names() {
        assertEquals(LDtkLayerType.AutoLayer, json.decodeFromString<LDtkLayerType>("\"AutoLayer\""))
        assertEquals(LDtkLayerType.Entities, json.decodeFromString<LDtkLayerType>("\"Entities\""))
        assertEquals(LDtkLayerType.IntGrid, json.decodeFromString<LDtkLayerType>("\"IntGrid\""))
        assertEquals(LDtkLayerType.Tiles, json.decodeFromString<LDtkLayerType>("\"Tiles\""))
    }

    @Test
    fun test_unknown_layer_type_throws() {
        assertFailsWith<IllegalArgumentException> { json.decodeFromString<LDtkLayerType>("\"Nope\"") }
    }

    @Test
    fun test_world_layout_and_bg_pos_names() {
        assertEquals(LDtkWorldLayout.GridVania, json.decodeFromString<LDtkWorldLayout>("\"GridVania\""))
        assertEquals(LDtkBgPos.CoverDirty, json.decodeFromString<LDtkBgPos>("\"CoverDirty\""))
    }

    @Test
    fun test_layer_instance() {
        val layer =
            json.decodeFromString<LDtkLayerInstance>(
                """
                {
                  "__cHei": 2, "__cWid": 3, "__gridSize": 16, "__identifier": "Collisions",
                  "__opacity": 0.5, "__pxTotalOffsetX": 4, "__pxTotalOffsetY": -2, "__type": "IntGrid",
                  "__tilesetDefUid": null, "iid": "abc",
                  "autoLayerTiles": [], "entityInstances": [], "gridTiles": [],
                  "intGridCsv": [0, 1, 1, 0, 2, 0],
                  "layerDefUid": 7, "levelId": 9, "pxOffsetX": 4, "pxOffsetY": -2, "seed": 123, "visible": true
                }
                """
                    .trimIndent()
            )
        assertEquals(3, layer.cWid)
        assertEquals(2, layer.cHei)
        assertEquals("Collisions", layer.identifier)
        assertEquals(0.5f, layer.opacity)
        assertEquals(listOf(0, 1, 1, 0, 2, 0), layer.intGridCSV)
        assertNull(layer.tilesetDefUid)
        assertEquals(9, layer.levelID)
        assertEquals("abc", layer.iid)
    }

    @Test
    fun test_entity_instance() {
        val entity =
            json.decodeFromString<LDtkEntityInstance>(
                """
                {
                  "__grid": [1, 2], "__identifier": "Player", "__pivot": [0.5, 1], "__tags": ["hero"],
                  "__worldX": 100, "__worldY": 200, "defUid": 5, "height": 16, "width": 8, "px": [24, 48],
                  "iid": "e1",
                  "fieldInstances": [{"__identifier": "hp", "__type": "Int", "__value": 10, "defUid": 6}]
                }
                """
                    .trimIndent()
            )
        assertEquals("Player", entity.identifier)
        assertEquals(listOf(1, 2), entity.grid)
        assertEquals(listOf(0.5f, 1f), entity.pivot)
        assertEquals(listOf("hero"), entity.tags)
        assertEquals(100, entity.worldX)
        assertEquals(listOf(24, 48), entity.px)
        assertEquals("10", entity.fieldInstances.single().value?.content)
    }

    @Test
    fun test_tileset_definition() {
        val tileset =
            json.decodeFromString<LDtkTilesetDefinition>(
                """{"identifier": "Tiles", "padding": 1, "pxHei": 64, "pxWid": 128, "relPath": "tiles.png", "spacing": 2, "tileGridSize": 16, "uid": 3, "tags": []}"""
            )
        assertEquals("tiles.png", tileset.relPath)
        assertEquals(16, tileset.tileGridSize)
        assertEquals(1, tileset.padding)
        assertEquals(2, tileset.spacing)
        assertNull(tileset.embedAtlas)
    }

    @Test
    fun test_layer_definition() {
        val def =
            json.decodeFromString<LDtkLayerDefinition>(
                """
                {
                  "__type": "IntGrid", "type": "IntGrid", "autoRuleGroups": [], "displayOpacity": 1,
                  "excludedTags": [], "gridSize": 16, "identifier": "Collisions",
                  "intGridValues": [{"value": 1, "identifier": "wall", "color": "#FFFFFF"}],
                  "pxOffsetX": 0, "pxOffsetY": 0, "requiredTags": [], "tilePivotX": 0, "tilePivotY": 0,
                  "uid": 11, "parallaxFactorX": 0.25
                }
                """
                    .trimIndent()
            )
        assertEquals(LDtkLayerType.IntGrid, def.layerDefinitionType)
        assertEquals("wall", def.intGridValues.single().identifier)
        assertEquals(0, def.intGridValues.single().groupUid)
        assertEquals(0.25f, def.parallaxFactorX)
        assertEquals(0f, def.parallaxFactorY)
        assertTrue(def.autoRuleGroups.isEmpty())
    }

    @Test
    fun test_level_definition_with_external_layers() {
        val level =
            json.decodeFromString<LDtkLevelDefinition>(
                """
                {
                  "__bgColor": "#000000", "__neighbours": [{"dir": "e", "levelIid": "l2"}],
                  "bgPivotX": 0.5, "bgPivotY": 0.5, "externalRelPath": "Level_0.ldtkl",
                  "fieldInstances": [], "identifier": "Level_0", "layerInstances": null,
                  "pxHei": 256, "pxWid": 512, "uid": 0, "worldX": 0, "worldY": -256, "bgPos": "Cover"
                }
                """
                    .trimIndent()
            )
        assertEquals("Level_0.ldtkl", level.externalRelPath)
        assertNull(level.layerInstances)
        assertEquals(LDtkBgPos.Cover, level.levelBgPos)
        assertEquals("l2", level.neighbours?.single()?.levelIid)
        assertEquals(-256, level.worldY)
        assertEquals(0, level.worldDepth)
    }
}
