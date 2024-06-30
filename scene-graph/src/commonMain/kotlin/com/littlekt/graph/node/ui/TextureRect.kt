package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * Adds a [TextureRect] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [TextureRect] context in order to initialize
 *   any values
 * @return the newly created [TextureRect]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.textureRect(
    callback: @SceneGraphDslMarker TextureRect.() -> Unit = {}
): TextureRect {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return TextureRect().also(callback).addTo(this)
}

/**
 * Adds a [TextureRect] to the [SceneGraph.root] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [TextureRect] context in order to initialize
 *   any values
 * @return the newly created [TextureRect]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.textureRect(
    callback: @SceneGraphDslMarker TextureRect.() -> Unit = {}
): TextureRect {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.textureRect(callback)
}

/**
 * A [Control] node that display a [TextureSlice].
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class TextureRect : Control() {
    /** Flips the current rendering of the [TextureRect] horizontally. */
    var flipX = false

    /** Flips the current rendering of the [TextureRect] vertically. */
    var flipY = false

    /** The texture behavior when resizing the node's bounding rectangle. */
    var stretchMode = StretchMode.KEEP

    /**
     * If `true`, then the internal size calculations with use the [TextureSlice.originalWidth] &
     * [TextureSlice.originalHeight] for its sizing instead of the trimmed width & height. This is
     * useful if a TextureSlice requires the empty margin around it.
     */
    var useOriginalSize = false
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that should be displayed by this [TextureRect] node. Sets the origin of
     * the [TextureRect] to the center.
     */
    var slice: TextureSlice?
        get() = _slice
        set(value) {
            textureSlice(value)
        }

    protected var _slice: TextureSlice? = null

    override val membersAndPropertiesString: String
        get() =
            "${super.membersAndPropertiesString}, flipX=$flipX, flipY=$flipY, stretchMode=$stretchMode, textureRegion=$slice"

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        slice?.let {
            var newWidth = 0f
            var newHeight = 0f
            var offsetX = 0f
            var offsetY = 0f
            var tile = false

            var sliceX = it.x.toFloat()
            var sliceY = it.y.toFloat()
            if (useOriginalSize) {
                sliceX -= it.virtualFrame?.x ?: 0f
                sliceY -= it.virtualFrame?.y ?: 0f
            }
            var sliceWidth = if (useOriginalSize) it.originalWidth.toFloat() else it.width.toFloat()
            var sliceHeight =
                if (useOriginalSize) it.originalHeight.toFloat() else it.height.toFloat()

            when (stretchMode) {
                StretchMode.SCALE -> {
                    newWidth = width
                    newHeight = height
                }
                StretchMode.TILE -> {
                    newWidth = sliceWidth
                    newHeight = sliceHeight
                    tile = true
                }
                StretchMode.KEEP -> {
                    newWidth = sliceWidth
                    newHeight = sliceHeight
                }
                StretchMode.KEEP_CENTERED -> {
                    offsetX = (width - sliceWidth) * 0.5f
                    offsetY = (height - sliceHeight) * 0.5f
                    newWidth = sliceWidth
                    newHeight = sliceHeight
                }
                StretchMode.KEEP_ASPECT,
                StretchMode.KEEP_ASPECT_CENTERED -> {
                    newWidth = width
                    newHeight = height
                    var texWidth = sliceWidth * newHeight / sliceHeight
                    var textHeight = newHeight

                    if (texWidth > newWidth) {
                        texWidth = newWidth
                        textHeight = sliceHeight * texWidth / sliceWidth
                    }

                    if (stretchMode == StretchMode.KEEP_ASPECT_CENTERED) {
                        offsetX += (newWidth - texWidth) * 0.5f
                        offsetY += (newHeight - textHeight) * 0.5f
                    }
                    newWidth = texWidth
                    newHeight = textHeight
                }
                StretchMode.KEEP_ASPECT_COVERED -> {
                    newWidth = width
                    newHeight = height
                    var texWidth = sliceWidth
                    var texHeight = sliceHeight
                    val widthRatio = newWidth / texWidth
                    val heightRatio = newHeight / texHeight
                    val scale = if (widthRatio > heightRatio) widthRatio else heightRatio

                    texWidth *= scale
                    texHeight *= scale

                    sliceX = ((texWidth - newWidth) / scale).absoluteValue * 0.5f
                    sliceY = ((texHeight - newHeight) / scale).absoluteValue * 0.5f

                    sliceWidth = newWidth / scale
                    sliceHeight = newHeight / scale
                }
            }
            if (tile) {
                var totalH = 0f
                while (totalH < height) {
                    var totalW = 0f
                    while (totalW < width) {
                        if (width - totalW >= sliceWidth && height - totalH >= sliceHeight) {
                            batch.draw(
                                it,
                                x = globalX + totalW,
                                y = globalY + totalH,
                                scaleX = globalScaleX,
                                scaleY = globalScaleY,
                                rotation = globalRotation,
                                color = color
                            )
                        } else {
                            batch.draw(
                                it.texture,
                                globalX + totalW,
                                globalY + totalH,
                                0f,
                                0f,
                                width = min(width - totalW, sliceWidth),
                                height = min(height - totalH, sliceHeight),
                                scaleX = globalScaleX,
                                scaleY = globalScaleY,
                                rotation = globalRotation,
                                srcX = it.x,
                                srcY = it.y,
                                srcWidth = min(width - totalW, sliceWidth).toInt(),
                                srcHeight = min(height - totalH, sliceHeight).toInt(),
                                flipX = flipX,
                                flipY = flipY,
                                color = color
                            )
                        }
                        totalW += sliceWidth
                    }
                    totalH += sliceHeight
                }
            } else {
                batch.draw(
                    it.texture,
                    globalX + offsetX,
                    globalY + offsetY,
                    0f,
                    0f,
                    width = newWidth,
                    height = newHeight,
                    scaleX = globalScaleX,
                    scaleY = globalScaleY,
                    rotation = globalRotation,
                    srcX = sliceX.toInt(),
                    srcY = sliceY.toInt(),
                    srcWidth = sliceWidth.toInt(),
                    srcHeight = sliceHeight.toInt(),
                    flipX = flipX,
                    flipY = flipY,
                    color = color
                )
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinWidth =
            if (useOriginalSize) _slice?.originalWidth?.toFloat() ?: 0f
            else _slice?.width?.toFloat() ?: 0f
        _internalMinHeight =
            if (useOriginalSize) _slice?.originalHeight?.toFloat() ?: 0f
            else _slice?.height?.toFloat() ?: 0f

        minSizeInvalid = false
    }

    fun textureSlice(slice: TextureSlice?) {
        if (slice == _slice) {
            return
        }

        _slice = slice
        onMinimumSizeChanged()
    }

    enum class StretchMode {
        /** Scale to fit the node's bounding rectangle. */
        SCALE,

        /** Tile inside the node's bounding rectangle. */
        TILE,

        /**
         * The texture keeps its original size and stays in the bounding rectangle's top-left
         * corner.
         */
        KEEP,

        /**
         * The texture keeps its original size and stays centered in the node's bounding rectangle.
         */
        KEEP_CENTERED,

        /**
         * Scale the texture to fit the node's bounding rectangle, but maintain the texture's aspect
         * ratio.
         */
        KEEP_ASPECT,

        /**
         * Scale the texture to fit the node's bounding rectangle, center it and maintain its aspect
         * ratio.
         */
        KEEP_ASPECT_CENTERED,

        /**
         * Scale the texture so that the shorter side fits the bounding rectangle. The other side
         * clips to the node's limits.
         */
        KEEP_ASPECT_COVERED,
    }
}
