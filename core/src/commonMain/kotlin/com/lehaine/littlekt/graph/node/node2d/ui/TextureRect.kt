package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.StretchMode
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.TextureSlice
import kotlin.math.absoluteValue

/**
 * Adds a [TextureRect] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [TextureRect] context in order to initialize any values
 * @return the newly created [TextureRect]
 */
inline fun Node.textureRect(callback: @SceneGraphDslMarker TextureRect.() -> Unit = {}) =
    TextureRect().also(callback).addTo(this)

/**
 * Adds a [TextureRect] to the  [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [TextureRect] context in order to initialize any values
 * @return the newly created [TextureRect]
 */
inline fun SceneGraph.textureRect(callback: @SceneGraphDslMarker TextureRect.() -> Unit = {}) =
    root.textureRect(callback)

/**
 * A [Control] node that display a [TextureSlice].
 * @author Colton Daily
 * @date 1/2/2022
 */
open class TextureRect : Control() {
    /**
     * Flips the current rendering of the [TextureRect] horizontally.
     */
    var flipX = false

    /**
     * Flips the current rendering of the [TextureRect] vertically.
     */
    var flipY = false


    var stretchMode = StretchMode.SCALE_ON_EXPAND

    var expand: Boolean
        get() = _expand
        set(value) {
            expand(value)
        }
    protected var _expand = false

    /**
     * The [TextureSlice] that should be displayed by this [TextureRect] node. Sets the origin of the [TextureRect] to the center.
     */
    var slice: TextureSlice?
        get() = _slice
        set(value) {
            textureSlice(value)
        }
    protected var _slice: TextureSlice? = null

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, flipX=$flipX, flipY=$flipY, stretchMode=$stretchMode, expand=$expand, textureRegion=$slice"

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)
        slice?.let {
            var newWidth = 0f
            var newHeight = 0f
            var offsetX = 0f
            var offsetY = 0f
            var tile = false

            var sliceX = it.x.toFloat()
            var sliceY = it.y.toFloat()
            var width = it.width.toFloat()
            var height = it.height.toFloat()

            when (stretchMode) {
                StretchMode.SCALE_ON_EXPAND -> {
                    newWidth = if (expand) width else it.width.toFloat()
                    newHeight = if (expand) height else it.height.toFloat()
                }
                StretchMode.SCALE -> {
                    newWidth = width
                    newHeight = height
                }
                StretchMode.TILE -> {
                    // TODO - impl a tile mode
                    newWidth = width
                    newHeight = height
                    tile = true
                }
                StretchMode.KEEP -> {
                    newWidth = it.width.toFloat()
                    newHeight = it.height.toFloat()
                }
                StretchMode.KEEP_CENTERED -> {
                    offsetX = (width - it.width.toFloat()) * 0.5f
                    offsetY = (height - it.height.toFloat()) * 0.5f
                    newWidth = it.width.toFloat()
                    newHeight = it.height.toFloat()
                }
                StretchMode.KEEP_ASPECT, StretchMode.KEEP_ASPECT_CENTERED -> {
                    newWidth = width
                    newHeight = height
                    var texWidth = it.width.toFloat() * newHeight / it.height.toFloat()
                    var textHeight = newHeight

                    if (texWidth > newWidth) {
                        texWidth = newWidth
                        textHeight = it.height.toFloat() * texWidth / it.width.toFloat()
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
                    var texWidth = it.width.toFloat()
                    var texHeight = it.height.toFloat()
                    val widthRatio = newWidth / texWidth
                    val heightRatio = newHeight / texHeight
                    val scale = if (widthRatio > heightRatio) widthRatio else heightRatio

                    texWidth *= scale
                    texHeight *= -scale

                    sliceX = ((texWidth - newWidth) / scale).absoluteValue * 0.5f
                    sliceY = ((texHeight - newHeight) / scale).absoluteValue * 0.5f
                    width = newWidth / scale
                    height = newHeight / scale
                }
            }
            batch.color = color
            batch.draw(
                it.texture,
                globalPosition.x + offsetX,
                globalPosition.y + offsetY,
                0f,
                0f,
                width = newWidth,
                height = newHeight,
                scaleX = globalScale.x,
                scaleY = globalScale.y,
                rotation = globalRotation,
                srcX = sliceX.toInt(),
                srcY = sliceY.toInt(),
                srcWidth = width.toInt(),
                srcHeight = height.toInt(),
                flipX = flipX,
                flipY = flipY
            )

        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        _internalMinHeight = 0f
        _internalMinWidth = 0f
        val texture = _slice
        if (!expand && texture != null) {
            _internalMinWidth = texture.width.toFloat()
            _internalMinHeight = texture.height.toFloat()
        }
        minSizeInvalid = false
    }

    fun textureSlice(slice: TextureSlice?) {
        if (slice == _slice) {
            return
        }

        _slice = slice
        onMinimumSizeChanged()
    }

    fun expand(expand: Boolean) {
        if (expand == _expand) {
            return
        }
        _expand = expand
        onMinimumSizeChanged()
    }

}