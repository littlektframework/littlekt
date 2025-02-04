package com.littlekt.graphics.g3d

import com.littlekt.graphics.g3d.skin.Skin
import com.littlekt.util.datastructure.fastForEach

/**
 * A MeshNode is a [Node3D] that used a list of [MeshPrimitive] for rendering, and an optional
 * [Skin].
 *
 * @property primitives the list of [MeshPrimitive] that make up this "model" (mesh node).
 * @author Colton Daily
 * @date 11/24/2024
 */
open class MeshNode
private constructor(val primitives: List<MeshPrimitive>, addInstanceOnInit: Boolean) : Node3D() {

    constructor(primitives: List<MeshPrimitive>) : this(primitives, true)

    init {
        if (addInstanceOnInit) {
            primitives.forEach { prim -> addChild(VisualInstance().apply { addTo(prim) }) }
        }
    }

    override fun forEachMeshPrimitive(action: (MeshPrimitive) -> Unit) {
        primitives.forEach { action(it) }
        children.fastForEach { it.forEachMeshPrimitive(action) }
    }

    /**
     * Copying a [MeshNode] will reuse the underlying [primitives] list. In other words, this will
     * create an instance of the underlying geometry. You only need to pass in the original
     * [MeshNode] into a ModelBatch render function otherwise, you'll get duplicate draw calls. Just
     * the fact of copying this node will update the [MeshPrimitive] instance data.
     */
    override fun copy(): Node3D {
        val copy =
            MeshNode(primitives, false).also {
                it.name = name
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }
}
