package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [GraphViewport] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [GraphViewport] context in order to initialize any values
 * @return the newly created [GraphViewport]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.graphViewport(callback: @SceneGraphDslMarker GraphViewport.() -> Unit = {}): GraphViewport {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return GraphViewport().also(callback).addTo(this)
}

/**
 * Adds a [GraphViewport] to the current [SceneGraph.root] as a child and then triggers the [GraphViewport]
 * @param callback the callback that is invoked with a [GraphViewport] context in order to initialize any values
 * @return the newly created [GraphViewport]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.graphViewport(callback: @SceneGraphDslMarker GraphViewport.() -> Unit = {}): GraphViewport {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.graphViewport(callback)
}

/**
 * @author Colton Daily
 * @date 3/13/2022
 */
class GraphViewport : Node() {
    var strategy: Viewport = Viewport()
        set(value) {
            field = value
            viewport = value
        }

    var virtualWidth: Int
        get() = strategy.virtualWidth
        set(value) {
            strategy.virtualWidth = value
        }

    var virtualHeight: Int
        get() = strategy.virtualHeight
        set(value) {
            strategy.virtualHeight = value
        }

    var width: Int
        get() = strategy.width
        set(value) {
            strategy.width = value
        }

    var height: Int
        get() = strategy.height
        set(value) {
            strategy.height = value
        }

    var x: Int
        get() = strategy.x
        set(value) {
            strategy.x = value
        }

    var y: Int
        get() = strategy.y
        set(value) {
            strategy.y = value
        }

    override fun onAddedToScene() {
        super.onAddedToScene()
        viewport = strategy
    }

    override fun _onResize(width: Int, height: Int, center: Boolean) {
        scene?.let {
            strategy.update(width, height, it.context)
        }
        super._onResize(width, height, center)
    }

    override fun _render(batch: Batch, camera: Camera) {
        if (!enabled || !visible) return
        if (batch.drawing) batch.end()
        scene?.let {
            strategy.apply(it.context)
        }
        batch.begin()
        super._render(batch, camera)
        batch.end()
        batch.begin()
    }
}