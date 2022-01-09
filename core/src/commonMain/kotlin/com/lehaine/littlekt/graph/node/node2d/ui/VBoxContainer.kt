package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker


/**
 * Adds a [VBoxContainer] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to initialize any values
 * @return the newly created [VBoxContainer]
 */
inline fun Node.vBoxContainer(callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}) =
    VBoxContainer().also(callback).addTo(this)

/**
 * Adds a [VBoxContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [VBoxContainer] context in order to initialize any values
 * @return the newly created [VBoxContainer]
 */
inline fun SceneGraph.vBoxContainer(callback: @SceneGraphDslMarker VBoxContainer.() -> Unit = {}) =
    root.vBoxContainer(callback)

/**
 * A vertical [BoxContainer] by adding [Control] from bottom up.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class VBoxContainer : BoxContainer() {

    init {
        vertical = true
    }
}