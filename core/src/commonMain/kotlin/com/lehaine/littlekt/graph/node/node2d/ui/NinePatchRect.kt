package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.*

/**
 * Adds a [NinePatchRect] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.ninePatchRect(callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}) =
    NinePatchRect().also(callback).addTo(this)

/**
 * Adds a [NinePatchRect] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph<*>.ninePatchRect(callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}) =
    root.ninePatchRect(callback)

/**
 * Creates a [Control] that uses and renders a [NinePatch].
 * @author Colton Daily
 * @date 1/18/2022
 */
open class NinePatchRect : Control() {

    /**
     * The width of the 9-patch's left column.
     */
    var left: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, bottom, top)
            onMinimumSizeChanged()
        }

    /**
     * The width of the 9-patch's right column.
     */
    var right: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, bottom, top)
            onMinimumSizeChanged()
        }

    /**
     * The height of the 9-patch's top row.
     */
    var top: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, bottom, top)
            onMinimumSizeChanged()
        }

    /**
     * The height of the 9-patch's bottom row.
     */
    var bottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, bottom, top)
            onMinimumSizeChanged()
        }

    /**
     * The texture to be used as a [NinePatch].
     */
    var texture: TextureSlice = Textures.white
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(value, left, right, bottom, top)
            onMinimumSizeChanged()
        }

    private var ninePatch: NinePatch = NinePatch(Textures.white, left, right, bottom, top)

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth = (left + right).toFloat()
        _internalMinHeight = (top + bottom).toFloat()

        minSizeInvalid = false
    }

    override fun render(batch: Batch, camera: Camera) {
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