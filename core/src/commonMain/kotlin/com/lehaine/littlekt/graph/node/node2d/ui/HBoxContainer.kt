package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Theme

/**
 * Adds a [HBoxContainer] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.hBoxContainer(callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}) =
    HBoxContainer().also(callback).addTo(this)

/**
 * Adds a [HBoxContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.hBoxContainer(callback: @SceneGraphDslMarker HBoxContainer.() -> Unit = {}) =
    root.hBoxContainer(callback)

/**
 * A vertical [BoxContainer] by adding [Control] from left to right.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class HBoxContainer : BoxContainer() {

    init {
        vertical = false
    }

    companion object {
        /**
         * [Theme] related variable names when setting theme values for a [Button]
         */
        object ThemeVars {
            val SEPARATION get() = BoxContainer.Companion.ThemeVars.SEPARATION
        }
    }
}