package com.lehaine.littlekt.graphics.tilemap.ldtk

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntity(
    val identifier: String,//= json.identifier

    /** Grid-based X coordinate **/
    val cx: Int,

    /** Grid-based Y coordinate **/
    val cy: Int,

    /** Pixel-based X coordinate **/
    val x: Float,

    /** Pixel-based Y coordinate **/
    val y: Float,

    /** Pivot X coord (0-1) **/
    val pivotX: Float,

    /** Pivot Y coord (0-1) **/
    val pivotY: Float,

    /** Width in pixels **/
    val width: Int,

    /** Height in pixels**/
    val height: Int,

    /** Tile infos if the entity has one (it could have been overridden by a Field value, such as Enums) **/
    val tileInfo: TileInfo?
) {

    data class TileInfo(val tilesetUid: Int, val x: Int, val y: Int, val w: Int, val h: Int)

    override fun toString(): String {
        return "Entity(identifier='$identifier', cx=$cx, cy=$cy, pixelX=$x, pixelY=$y, tileInfosJson=$tileInfo)"
    }

    protected fun entityInfoString(): String {
        return "identifier='$identifier', cx=$cx, cy=$cy, pixelX=$x, pixelY=$y, tileInfosJson=$tileInfo"
    }


}