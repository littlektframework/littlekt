package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.NinePatch
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Textures

/**
 * Adds a [NinePatchRect] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.ninePatchRect(callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}) =
    NinePatchRect().also(callback).addTo(this)

/**
 * Adds a [NinePatchRect] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.ninePatchRect(callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}) =
    root.ninePatchRect(callback)

/**
 * Creates a [Control] that uses and renders a [NinePatch].
 * @author Colton Daily
 * @date 1/18/2022
 */
open class NinePatchRect : Control() {

    var ninePatch: NinePatch = NinePatch(Textures.white, 0, 0, 0, 0)
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth = (ninePatch.left + ninePatch.right).toFloat()
        _internalMinHeight = (ninePatch.top + ninePatch.bottom).toFloat()

        minSizeInvalid = false
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        ninePatch.draw(
            batch,
            globalX,
            globalY,
            width,
            height,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation
        )
    }
}