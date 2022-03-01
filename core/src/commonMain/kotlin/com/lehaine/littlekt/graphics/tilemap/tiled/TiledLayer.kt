package com.lehaine.littlekt.graphics.tilemap.tiled

import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.tilemap.TileLayer

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
abstract class TiledLayer(
    val type: String,
    val name: String,
    val id: Int,
    val width: Int,
    val height: Int,
    val offsetX: Float,
    val offsetY: Float,
    val tileWidth: Int,
    val tileHeight: Int,
    val tintColor: Color?,
    val opacity: Float,
    val properties: Map<String, TiledMap.Property>
) : TileLayer() {

    /**
     * @return true if grid-based coordinates are within layer bounds.
     */
    fun isCoordValid(cx: Int, cy: Int): Boolean {
        return cx in 0 until width && cy >= 0 && cy < height
    }

    fun getCellX(coordId: Int): Int {
        return coordId - coordId / width * width
    }

    fun getCellY(coordId: Int): Int {
        return coordId / width
    }

    fun getCoordId(cx: Int, cy: Int) = cx + cy * width
}