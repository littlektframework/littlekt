package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

/**
 * Adds a [TextureProgress] to the current [Node] as a child and then triggers the [callback].
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.textureProgress(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker TextureProgress.() -> Unit = {},
): TextureProgress {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return TextureProgress().addTo(this, modifier, callback)
}

/**
 * Adds a [TextureProgress] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 *
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.textureProgress(
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker TextureProgress.() -> Unit = {},
): TextureProgress {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.textureProgress(modifier = modifier, callback = callback)
}

/**
 * A textured-based progress bar. Useful for loading screens and health bars.
 *
 * @author Colton Daily
 * @date 2/23/2022
 */
open class TextureProgress : Range() {

    private var backgroundNine: NinePatch? = null
    private var progressNine: NinePatch? = null
    private var foregroundNine: NinePatch? = null

    /**
     * If `true`, treats the textures as a [NinePatchRect]. Use the [left], [right], [top],
     * [bottom], properties to set up the nine patch's 3x3 grid.
     */
    var useNinePatch: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /** The width of the 9-patch's left column. */
    var left: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /** The width of the 9-patch's right column. */
    var right: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /** The height of the 9-patch's top row. */
    var top: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /** The height of the 9-patch's bottom row. */
    var bottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /** The [TextureSlice] that draws behind the progress bar as the background. */
    var background: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            backgroundNine =
                if (useNinePatch) {
                    if (value != null) {
                        NinePatch(value, left, right, top, bottom)
                    } else {
                        null
                    }
                } else {
                    null
                }
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that clips based on the node's [value] and [fillMode]. As [value]
     * increases, the texture fills up. When [value] reaches [max] the texture will show entirely.
     * The texture is hidden if the [value] is equal to [min].
     *
     * @see Range.value
     * @see Range.max
     * @see Range.min
     */
    var progressBar: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            progressNine =
                if (useNinePatch) {
                    if (value != null) {
                        NinePatch(value, left, right, top, bottom)
                    } else {
                        null
                    }
                } else {
                    null
                }
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that draws on top of the progress bar. Use it to add highlights or an
     * upper-frame that hides part of the progress.
     */
    var foreground: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            foregroundNine =
                if (useNinePatch) {
                    if (value != null) {
                        NinePatch(value, left, right, top, bottom)
                    } else {
                        null
                    }
                } else {
                    null
                }
            if (background == null) {
                onMinimumSizeChanged()
            }
        }

    /** Multiplies the color of the [background] texture. */
    var backgroundColor: Color = Color.WHITE

    /** Multiplies the color of the [foreground] texture. */
    var foregroundColor: Color = Color.WHITE

    /** Multiplies the color of the [progressBar] texture. */
    var progressBarColor: Color = Color.WHITE

    /**
     * The fill direction.
     *
     * @see [FillMode]
     */
    var fillMode: FillMode = FillMode.LEFT_TO_RIGHT

    /**
     * How the texture sizes affect the minimum size of this [TextureProgress]. Use
     * [TextureRect.ExpandMode.IGNORE_SIZE] to draw the textures smaller than their pixel size.
     */
    var expandMode: TextureRect.ExpandMode = TextureRect.ExpandMode.KEEP_SIZE
        set(value) {
            if (field == value) return
            field = value
            onMinimumSizeChanged()
        }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)

        if (useNinePatch) {
            renderNinePatches(batch)
        } else {
            renderSlices(batch)
        }
    }

    private fun renderNinePatches(batch: Batch) {
        val x = globalX - originX
        val y = globalY - originY
        backgroundNine?.draw(
            batch = batch,
            x = x,
            y = y,
            width = width,
            height = height,
            originX = originX,
            originY = originY,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation,
            color = backgroundColor,
        )

        val ratio = ratio
        if (ratio > 0f) {
            var srcX = 0f
            var srcY = 0f
            var srcWidth = width
            var srcHeight = height
            when (fillMode) {
                FillMode.LEFT_TO_RIGHT -> srcWidth = width * ratio
                FillMode.RIGHT_TO_LEFT -> srcX = width * (1f - ratio)
                FillMode.TOP_TO_BOTTOM -> srcY = height * (1f - ratio)
                FillMode.BOTTOM_TO_TOP -> srcHeight = height * ratio
            }
            progressNine?.draw(
                batch = batch,
                x = x,
                y = y,
                width = width,
                height = height,
                originX = originX,
                originY = originY,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = globalRotation,
                color = progressBarColor,
                srcX = srcX,
                srcY = srcY,
                srcWidth = srcWidth,
                srcHeight = srcHeight,
            )
        }

        foregroundNine?.draw(
            batch = batch,
            x = x,
            y = y,
            width = width,
            height = height,
            originX = originX,
            originY = originY,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation,
            color = foregroundColor,
        )
    }

    private fun renderSlices(batch: Batch) {
        val x = globalX - originX
        val y = globalY - originY
        background?.let {
            batch.draw(
                slice = it,
                x = x,
                y = y,
                originX = originX,
                originY = originY,
                width = width,
                height = height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = globalRotation,
                color = backgroundColor,
            )
        }
        progressBar?.let { drawProgress(batch, it, x, y) }
        foreground?.let {
            batch.draw(
                slice = it,
                x = x,
                y = y,
                originX = originX,
                originY = originY,
                width = width,
                height = height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = globalRotation,
                color = foregroundColor,
            )
        }
    }

    private fun drawProgress(batch: Batch, slice: TextureSlice, x: Float, y: Float) {
        val ratio = ratio
        val sliceWidth = slice.width
        val sliceHeight = slice.height
        if (ratio <= 0f || sliceWidth <= 0 || sliceHeight <= 0) return

        var srcX = slice.x
        var srcY = slice.y
        var srcWidth = sliceWidth
        var srcHeight = sliceHeight
        var offsetX = 0f
        var offsetY = 0f
        var drawWidth = width
        var drawHeight = height

        when (fillMode) {
            FillMode.LEFT_TO_RIGHT -> {
                srcWidth = (sliceWidth * ratio).roundToInt()
                drawWidth = width * srcWidth / sliceWidth
            }
            FillMode.RIGHT_TO_LEFT -> {
                srcWidth = (sliceWidth * ratio).roundToInt()
                srcX += sliceWidth - srcWidth
                drawWidth = width * srcWidth / sliceWidth
                offsetX = width - drawWidth
            }
            FillMode.TOP_TO_BOTTOM -> {
                srcHeight = (sliceHeight * ratio).roundToInt()
                drawHeight = height * srcHeight / sliceHeight
                offsetY = height - drawHeight
            }
            FillMode.BOTTOM_TO_TOP -> {
                srcHeight = (sliceHeight * ratio).roundToInt()
                srcY += sliceHeight - srcHeight
                drawHeight = height * srcHeight / sliceHeight
            }
        }
        if (srcWidth <= 0 || srcHeight <= 0) return

        batch.draw(
            slice = slice,
            x = x,
            y = y,
            originX = originX - offsetX,
            originY = originY - offsetY,
            width = drawWidth,
            height = drawHeight,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation,
            color = progressBarColor,
            srcX = srcX,
            srcY = srcY,
            srcWidth = srcWidth,
            srcHeight = srcHeight,
        )
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        if (expandMode == TextureRect.ExpandMode.IGNORE_SIZE) {
            _internalMinWidth = 0f
            _internalMinHeight = 0f
        } else if (useNinePatch) {
            _internalMinWidth = (left + right).toFloat()
            _internalMinHeight = (top + bottom).toFloat()
        } else {
            _internalMinWidth =
                background?.width?.toFloat()
                    ?: foreground?.width?.toFloat()
                    ?: progressBar?.width?.toFloat()
                    ?: 0f
            _internalMinHeight =
                background?.height?.toFloat()
                    ?: foreground?.height?.toFloat()
                    ?: progressBar?.height?.toFloat()
                    ?: 0f
        }

        minSizeInvalid = false
    }

    private fun validateNinePatches() {
        val bg = background
        val prog = progressBar
        val fg = foreground

        if (bg != null) {
            backgroundNine =
                if (useNinePatch) {
                    NinePatch(bg, left, right, top, bottom)
                } else {
                    null
                }
        }

        if (prog != null) {
            progressNine =
                if (useNinePatch) {
                    NinePatch(prog, left, right, top, bottom)
                } else {
                    null
                }
        }

        if (fg != null) {
            foregroundNine =
                if (useNinePatch) {
                    NinePatch(fg, left, right, top, bottom)
                } else {
                    null
                }
        }
    }

    enum class FillMode {
        /** Progress fills from left to right. */
        LEFT_TO_RIGHT,

        /** Progress fills from right to left. */
        RIGHT_TO_LEFT,

        /** Progress fills from the top edge down. */
        TOP_TO_BOTTOM,

        /** Progress fills from the bottom edge up. */
        BOTTOM_TO_TOP,
    }
}
