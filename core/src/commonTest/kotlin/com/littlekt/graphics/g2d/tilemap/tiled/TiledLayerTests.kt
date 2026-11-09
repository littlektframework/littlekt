package com.littlekt.graphics.g2d.tilemap.tiled

import com.littlekt.graphics.g2d.tilemap.tiled.internal.TileData
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TiledLayerTests {
    private val h = TiledMap.FLAG_FLIP_HORIZONTALLY
    private val v = TiledMap.FLAG_FLIP_VERTICALLY
    private val d = TiledMap.FLAG_FLIP_DIAGONALLY

    private fun layer(width: Int = 3, height: Int = 2, tileData: IntArray = IntArray(width * height)) =
        TiledTilesLayer(
            type = "tilelayer",
            name = "layer",
            id = 1,
            visible = true,
            width = width,
            height = height,
            offsetX = 0f,
            offsetY = 0f,
            tileWidth = 16,
            tileHeight = 16,
            tintColor = null,
            opacity = 1f,
            properties = emptyMap(),
            staggerIndex = null,
            staggerAxis = null,
            orientation = TiledMap.Orientation.ORTHOGONAL,
            tileData = tileData,
            tiles = emptyMap(),
        )

    private fun decode(bits: Int): TileData = with(layer()) { bits.bitsToTileData(TileData()) }

    @Test
    fun test_get_coord_id_flips_y() {
        val layer = layer()
        assertEquals(3, layer.getCoordId(0, 0))
        assertEquals(5, layer.getCoordId(2, 0))
        assertEquals(0, layer.getCoordId(0, 1))
    }

    @Test
    fun test_get_cell_x_and_y() {
        val layer = layer()
        assertEquals(2, layer.getCellX(5))
        assertEquals(1, layer.getCellY(5))
        assertEquals(0, layer.getCellX(3))
    }

    @Test
    fun test_get_tile_id_clears_flip_flags() {
        val layer = layer(tileData = intArrayOf(7 or h, 2 or v or d, 0, 1, 0, 0))
        assertEquals(7, layer.getTileId(0, 1))
        assertEquals(2, layer.getTileId(1, 1))
        assertEquals(1, layer.getTileId(0, 0))
    }

    @Test
    fun test_bits_to_tile_data_no_flags() {
        val data = decode(42)
        assertEquals(42, data.id)
        assertFalse(data.flipX)
        assertFalse(data.flipY)
        assertEquals(Angle.ZERO, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_horizontal_and_vertical() {
        val data = decode(5 or h or v)
        assertEquals(5, data.id)
        assertTrue(data.flipX)
        assertTrue(data.flipY)
        assertEquals(Angle.ZERO, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_diagonal_only() {
        val data = decode(5 or d)
        assertFalse(data.flipX)
        assertTrue(data.flipY)
        assertEquals((-270).degrees, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_diagonal_horizontal() {
        val data = decode(5 or d or h)
        assertFalse(data.flipX)
        assertFalse(data.flipY)
        assertEquals((-270).degrees, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_diagonal_vertical() {
        val data = decode(5 or d or v)
        assertFalse(data.flipX)
        assertFalse(data.flipY)
        assertEquals((-90).degrees, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_all_flags() {
        val data = decode(5 or d or h or v)
        assertEquals(5, data.id)
        assertTrue(data.flipX)
        assertFalse(data.flipY)
        assertEquals((-270).degrees, data.rotation)
    }

    @Test
    fun test_bits_to_tile_data_reuses_result() {
        val result = TileData(flipX = true, flipY = true, rotation = 90.degrees)
        val out = with(layer()) { 3.bitsToTileData(result) }
        assertSame(result, out)
        assertFalse(out.flipX)
        assertFalse(out.flipY)
        assertEquals(Angle.ZERO, out.rotation)
    }
}
