package com.lehaine.littlekt.graphics.tilemap.ldtk

import com.lehaine.littlekt.file.ldtk.EntityInstance
import com.lehaine.littlekt.file.ldtk.LayerInstance

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
open class LDtkEntityLayer(
    json: LayerInstance
) : LDtkLayer(json) {

    private val _entities = mutableListOf<LDtkEntity>()
    var entities = listOf<LDtkEntity>()
        private set

    internal fun instantiateEntities() {
        _entities.clear()
        json.entityInstances.forEach { entityInstanceJson ->
            instantiateEntity(entityInstanceJson)?.also { _entities.add(it) }
        }
        entities = _entities.toList()
    }

    /**
     * This function will be overridden in the ProjectProcessor if used.
     */
    private fun instantiateEntity(json: EntityInstance): LDtkEntity? {
        return LDtkEntity(json).also { _entities.add(it) }
    }

    override fun toString(): String {
        return "LayerEntities(entities=$entities)"
    }

}