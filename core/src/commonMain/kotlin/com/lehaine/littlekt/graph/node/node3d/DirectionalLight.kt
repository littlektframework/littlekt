package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Adds a [DirectionalLight] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [DirectionalLight] context in order to initialize any values
 * @return the newly created [DirectionalLight]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.directionalLight(callback: @SceneGraphDslMarker DirectionalLight.() -> Unit = {}): DirectionalLight {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return DirectionalLight().also(callback).addTo(this)
}

/**
 * Adds a [DirectionalLight] to the current [SceneGraph.root] as a child and then triggers the [DirectionalLight]
 * @param callback the callback that is invoked with a [DirectionalLight] context in order to initialize any values
 * @return the newly created [DirectionalLight]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.directionalLight(callback: @SceneGraphDslMarker DirectionalLight.() -> Unit = {}): DirectionalLight {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.directionalLight(callback)
}

/**
 * @author Colton Daily
 * @date 12/26/2022
 */
class DirectionalLight : Light() {

    override fun onAddedToScene() {
        super.onAddedToScene()
        scene?.environment?.lights?.add(this)
    }

    override fun onRemovedFromScene() {
        super.onRemovedFromScene()
        scene?.environment?.lights?.remove(this)
    }
}