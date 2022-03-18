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
 * Adds a [ViewportNode] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [ViewportNode] context in order to initialize any values
 * @return the newly created [ViewportNode]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.viewport(callback: @SceneGraphDslMarker ViewportNode.() -> Unit = {}): ViewportNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return ViewportNode().also(callback).addTo(this)
}

/**
 * Adds a [ViewportNode] to the current [SceneGraph.root] as a child and then triggers the [ViewportNode]
 * @param callback the callback that is invoked with a [ViewportNode] context in order to initialize any values
 * @return the newly created [ViewportNode]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.viewport(callback: @SceneGraphDslMarker ViewportNode.() -> Unit = {}): ViewportNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.viewport(callback)
}

/**
 * A [Node] that applies a [Viewport] to its children when rendering via [strategy].
 * @author Colton Daily
 * @date 3/13/2022
 */
class ViewportNode : Node() {
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

    override fun resize(width: Int, height: Int) {
        scene?.let {
            strategy.update(width, height, it.context)
        }
        super.resize(width, height)
    }

    override fun propagateInternalRender(batch: Batch, camera: Camera, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        if (!enabled) return
        scene?.applyViewport(strategy)
        nodes.forEach {
            it.propagateInternalRender(batch, camera, renderCallback)
        }
        batch.flush()
        scene?.applyPreviousViewport()
    }
}