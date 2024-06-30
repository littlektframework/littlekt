package com.littlekt.graphics.g2d.tilemap.ldtk

/**
 * A reference to an entity in **LDtk**.
 *
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntity(
    val identifier: String,
    val iid: String,

    /** Grid-based X coordinate * */
    val cx: Int,

    /** Grid-based Y coordinate * */
    val cy: Int,

    /** Pixel-based X coordinate * */
    val x: Float,

    /** Pixel-based Y coordinate * */
    val y: Float,

    /** Pivot X coord (0-1) * */
    val pivotX: Float,

    /** Pivot Y coord (0-1) * */
    val pivotY: Float,

    /** Width in pixels * */
    val width: Int,

    /** Height in pixels* */
    val height: Int,

    /**
     * Tile infos if the entity has one (it could have been overridden by a Field value, such as
     * Enums) *
     */
    val tileInfo: LDtkTileInfo?,
    val fields: Map<String, LDtkField<*>>
) {
    @Suppress("UNCHECKED_CAST") fun <T> field(name: String) = fields[name] as LDtkValueField<T>

    @Suppress("UNCHECKED_CAST") fun <T> fieldArray(name: String) = fields[name] as LDtkArrayField<T>

    override fun toString(): String {
        return "Entity(identifier='$identifier', cx=$cx, cy=$cy, pixelX=$x, pixelY=$y, tileInfosJson=$tileInfo, fields=$fields)"
    }
}
