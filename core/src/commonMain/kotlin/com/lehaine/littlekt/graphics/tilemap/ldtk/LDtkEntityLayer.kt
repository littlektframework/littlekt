package com.lehaine.littlekt.graphics.tilemap.ldtk

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntityLayer(
    val entities: List<LDtkEntity>,
    identifier: String,
    type: LayerType,
    cellSize: Int,
    gridWidth: Int,
    gridHeight: Int,
    pxTotalOffsetX: Int,
    pxTotalOffsetY: Int,
    opacity: Float,
) : LDtkLayer(
    identifier, type, cellSize, gridWidth, gridHeight, pxTotalOffsetX, pxTotalOffsetY, opacity
) {
    val entitiesMap: Map<String, List<LDtkEntity>>

    init {
        val map = mutableMapOf<String, MutableList<LDtkEntity>>()
        entities.forEach {
            map.getOrPut(it.identifier) {
                mutableListOf()
            }.add(it)
        }
        entitiesMap = map
    }

    operator fun get(entity: String) = entitiesMap[entity]

    override fun toString(): String {
        return "LayerEntities(entities=$entities)"
    }

}