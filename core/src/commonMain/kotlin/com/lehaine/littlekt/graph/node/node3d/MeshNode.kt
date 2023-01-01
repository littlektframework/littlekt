package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.render.ModelMaterial
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g3d.model.Skin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [MeshNode] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [MeshNode] context in order to initialize any values
 * @return the newly created [MeshNode]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.meshNode(callback: @SceneGraphDslMarker MeshNode.() -> Unit = {}): MeshNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return MeshNode().also(callback).addTo(this)
}

/**
 * Adds a [MeshNode] to the current [SceneGraph.root] as a child and then triggers the [MeshNode]
 * @param callback the callback that is invoked with a [MeshNode] context in order to initialize any values
 * @return the newly created [MeshNode]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.meshNode(callback: @SceneGraphDslMarker MeshNode.() -> Unit = {}): MeshNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.meshNode(callback)
}

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class MeshNode : VisualInstance() {
    var mesh: Mesh? = null
    var morphWeights: FloatArray? = null

    val textures = mutableMapOf<String, Texture>()
    var skin: Skin? = null
    var isOpaque = true

    override fun render(camera: Camera) {
        super.render(camera)
        val material = material ?: scene?.currentMaterial as? ModelMaterial ?: return
        material.shader?.let {
            mesh?.render(it)
        }
    }

    override fun setMaterialParameters(material: ModelMaterial, camera: Camera) {
        super.setMaterialParameters(material, camera)
        textures["albedo"]?.let {
            material.texture = it
        }
    }
}