package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.EntityInstance

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntity(val json: EntityInstance) {
    val identifier: String = json.identifier

    /** Grid-based X coordinate **/
    val cx: Int = json.grid[0]

    /** Grid-based Y coordinate **/
    val cy: Int = json.grid[1]

    /** Pixel-based X coordinate **/
    val pixelX: Int = json.px[0]

    /** Pixel-based Y coordinate **/
    val pixelY: Int = json.px[1]

    /** Pivot X coord (0-1) **/
    val pivotX: Float = if (json.pivot.isNullOrEmpty()) 0f else json.pivot[0]

    /** Pivot Y coord (0-1) **/
    val pivotY: Float = if (json.pivot.isNullOrEmpty()) 0f else json.pivot[1]

    /** Width in pixels **/
    val width: Int = json.width

    /** Height in pixels**/
    val height: Int = json.height

    /** Tile infos if the entity has one (it could have been overridden by a Field value, such as Enums) **/
    val tileInfo: TileInfo? = if (json.tile == null) {
        null
    } else {
        TileInfo(
            tilesetUid = json.tile.tilesetUid,
            x = json.tile.srcRect[0],
            y = json.tile.srcRect[1],
            w = json.tile.srcRect[2],
            h = json.tile.srcRect[3]
        )
    }

    data class TileInfo(val tilesetUid: Int, val x: Int, val y: Int, val w: Int, val h: Int)

    override fun toString(): String {
        return "Entity(identifier='$identifier', cx=$cx, cy=$cy, pixelX=$pixelX, pixelY=$pixelY, tileInfosJson=$tileInfo)"
    }

    protected fun entityInfoString(): String {
        return "identifier='$identifier', cx=$cx, cy=$cy, pixelX=$pixelX, pixelY=$pixelY, tileInfosJson=$tileInfo"
    }


}