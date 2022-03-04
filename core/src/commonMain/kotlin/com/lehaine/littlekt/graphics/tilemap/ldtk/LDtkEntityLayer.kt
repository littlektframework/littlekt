package com.lehaine.littlekt.graphics.tilemap.ldtk

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntityLayer(
    val entities: List<LDtkEntity>,
    identifier: String,
    iid: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : LDtkLayer(
    identifier, iid, type, cellSize, gridWidth, gridHeight, pxTotalOffsetX, pxTotalOffsetY, opacity
) {
    val entitiesMap: Map<String, List<LDtkEntity>> by lazy {
        entities.groupBy { it.identifier }
    }

    fun entities(name: String) = entitiesMap[name] ?: error("Entities: '$name' does not exist in the Entity layer!")

    operator fun get(entity: String) = entities(entity)

    override fun toString(): String {
        return "LayerEntities(entities=$entities)"
    }

}