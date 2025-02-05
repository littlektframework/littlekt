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
private constructor(private val primitives: List<MeshPrimitive>, addInstanceOnInit: Boolean) :
    Node3D() {

    constructor(primitives: List<MeshPrimitive>) : this(primitives, true)

    init {
        if (addInstanceOnInit) {
            primitives.forEach { prim -> addChild(MeshPrimitiveInstance().apply { addTo(prim) }) }
        }
    }

    /**
     * Copying a [MeshNode] will reuse the underlying [primitives] list. In other words, this will
     * create an instance of the underlying geometry. Just the fact of copying this node will update
     * the [MeshPrimitive] instance data. This node will still need to have [update] called and
     * passed into the renderer to handle any frustum related updates.
     */
    override fun copy(): Node3D {
        val copy =
            MeshNode(primitives, false).also {
                it.name = name
                it.frustumCulled = frustumCulled
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }
}
