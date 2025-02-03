package com.littlekt.graphics.g3d

import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
open class MeshNode
private constructor(
    val primitives: List<MeshPrimitive>,
    var skin: Skin? = null,
    addInstanceOnInit: Boolean,
) : Node3D() {

    constructor(primitives: List<MeshPrimitive>, skin: Skin? = null) : this(primitives, skin, true)

    init {
        if (addInstanceOnInit) {
            primitives.forEach { prim -> addChild(VisualInstance().apply { addTo(prim) }) }
        }
    }

    override fun forEachMeshPrimitive(action: (MeshPrimitive) -> Unit) {
        primitives.forEach { action(it) }
        children.fastForEach { it.forEachMeshPrimitive(action) }
    }

    override fun copy(): Node3D {
        val copy =
            MeshNode(primitives, skin?.copy(), false).also {
                it.name = name
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }
}
