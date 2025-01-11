package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.resources.Textures
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Adds a [NinePatchRect] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.ninePatchRect(
    callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}
): NinePatchRect {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return NinePatchRect().also(callback).addTo(this)
}

/**
 * Adds a [NinePatchRect] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.ninePatchRect(
    callback: @SceneGraphDslMarker NinePatchRect.() -> Unit = {}
): NinePatchRect {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.ninePatchRect(callback)
}

/**
 * Creates a [Control] that uses and renders a [NinePatch].
 *
 * @author Colton Daily
 * @date 1/18/2022
 */
open class NinePatchRect : Control() {

    /** The width of the 9-patch's left column. */
    var left: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, top, bottom)
            onMinimumSizeChanged()
        }

    /** The width of the 9-patch's right column. */
    var right: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, top, bottom)
            onMinimumSizeChanged()
        }

    /** The height of the 9-patch's top row. */
    var top: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, top, bottom)
            onMinimumSizeChanged()
        }

    /** The height of the 9-patch's bottom row. */
    var bottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(texture, left, right, top, bottom)
            onMinimumSizeChanged()
        }

    /** The texture to be used as a [NinePatch]. */
    var texture: TextureSlice = Textures.white
        set(value) {
            if (field == value) return
            field = value
            ninePatch = NinePatch(value, left, right, top, bottom)
            onMinimumSizeChanged()
        }

    private var ninePatch: NinePatch = NinePatch(Textures.white, left, right, top, bottom)

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth = (left + right).toFloat()
        _internalMinHeight = (top + bottom).toFloat()

        minSizeInvalid = false
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        ninePatch.draw(
            batch = batch,
            x = globalX - originX,
            y = globalY - originY,
            originX = originX,
            originY = originY,
            width = width,
            height = height,
            color = color,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation,
        )
    }
}
