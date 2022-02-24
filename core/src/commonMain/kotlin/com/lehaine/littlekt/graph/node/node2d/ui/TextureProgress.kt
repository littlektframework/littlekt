package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.*
import kotlin.math.ceil
import kotlin.math.min

/**
 * Adds a [TextureProgress] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.textureProgress(callback: @SceneGraphDslMarker TextureProgress.() -> Unit = {}) =
    TextureProgress().also(callback).addTo(this)

/**
 * Adds a [TextureProgress] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph<*>.textureProgress(callback: @SceneGraphDslMarker TextureProgress.() -> Unit = {}) =
    root.textureProgress(callback)

/**
 * A textured-based progress bar. Useful for loading screens and health bars.
 * @author Colton Daily
 * @date 2/23/2022
 */
open class TextureProgress : Range() {

    private var backgroundNine: NinePatch? = null
    private var progressNine: NinePatch? = null
    private var foregroundNine: NinePatch? = null

    /**
     * If `true`, treats the textures as a [NinePatchRect]. Use the [left], [right], [top], [bottom], properties to
     * set up the nine patch's 3x3 grid.
     */
    var useNinePatch: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /**
     * The width of the 9-patch's left column.
     */
    var left: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /**
     * The width of the 9-patch's right column.
     */
    var right: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /**
     * The height of the 9-patch's top row.
     */
    var top: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /**
     * The height of the 9-patch's bottom row.
     */
    var bottom: Int = 0
        set(value) {
            if (field == value) return
            field = value
            validateNinePatches()
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that draws behind the progress bar as the background.
     */
    var background: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            backgroundNine = if (useNinePatch) {
                if (value != null) {
                    NinePatch(value, left, right, bottom, top)
                } else {
                    null
                }
            } else {
                null
            }
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that clips based on the node's [value] and [fillMode]. As [value] increases, the texture
     * fills up. When [value] reaches [max] the texture will show entirely. The texture is hidden if the [value]
     * is equal to [min].
     *
     * @see Range.value
     * @see Range.max
     * @see Range.min
     */
    var progressBar: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            progressNine = if (useNinePatch) {
                if (value != null) {
                    NinePatch(value, left, right, bottom, top)
                } else {
                    null
                }
            } else {
                null
            }
            onMinimumSizeChanged()
        }

    /**
     * The [TextureSlice] that draws on top of the progress bar. Use it to add highlights or an upper-frame that hides
     * part of the progress.
     */
    var foreground: TextureSlice? = null
        set(value) {
            if (field == value) return
            field = value
            foregroundNine = if (useNinePatch) {
                if (value != null) {
                    NinePatch(value, left, right, bottom, top)
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

    /**
     * Multiplies the color of the [background] texture.
     */
    var backgroundColor: Color = Color.WHITE

    /**
     * Multiplies the color of the [foreground] texture.
     */
    var foregroundColor: Color = Color.WHITE

    /**
     * Multiplies the color of the [progressBar] texture.
     */
    var progressBarColor: Color = Color.WHITE

    /**
     * The fill direction.
     *
     * @see [FillMode]
     */
    var fillMode: FillMode = FillMode.LEFT_TO_RIGHT

    override fun render(batch: Batch, camera: Camera) {
        super.render(batch, camera)

        if (useNinePatch) {
            backgroundNine?.draw(
                batch,
                globalX,
                globalY,
                width,
                height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = globalRotation,
                color = backgroundColor,
            )

            progressNine?.let {
                when (fillMode) {
                    FillMode.LEFT_TO_RIGHT -> {
                        progressNine?.draw(
                            batch,
                            globalX,
                            globalY,
                            width,
                            height,
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            color = progressBarColor,
                            srcWidth = width - width * (1f - ratio)
                        )
                    }
                    FillMode.RIGHT_TO_LEFT -> {
                        progressNine?.draw(
                            batch,
                            globalX,
                            globalY,
                            width,
                            height,
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            color = progressBarColor,
                            srcX = width * (1f - ratio),
                        )
                    }
                    FillMode.TOP_TO_BOTTOM -> {
                        progressNine?.draw(
                            batch,
                            globalX,
                            globalY,
                            width,
                            height,
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            color = progressBarColor,
                            srcHeight = height - height * (1f - ratio)
                        )
                    }
                    FillMode.BOTTOM_TO_TOP -> {
                        progressNine?.draw(
                            batch,
                            globalX,
                            globalY,
                            width,
                            height,
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            color = progressBarColor,
                            srcY = height * (1f - ratio),
                        )
                    }
                }

                foregroundNine?.draw(
                    batch,
                    globalX,
                    globalY,
                    width,
                    height,
                    scaleX = globalScaleX,
                    scaleY = globalScaleY,
                    rotation = globalRotation,
                    color = foregroundColor
                )

            }

        } else {
            background?.let {
                batch.draw(
                    it,
                    globalX,
                    globalY,
                    scaleX = globalScaleX,
                    scaleY = globalScaleY,
                    rotation = globalRotation,
                    colorBits = backgroundColor.toFloatBits()
                )
            }
            progressBar?.let {
                val sliceX = it.x
                val sliceY = it.y
                val sliceWidth = it.width
                val sliceHeight = it.height
                val widthRatio = ceil(sliceWidth * ratio).toInt()
                val heightRatio = ceil(sliceHeight * ratio).toInt()

                when (fillMode) {
                    FillMode.LEFT_TO_RIGHT -> {
                        batch.draw(
                            it.texture,
                            globalX,
                            globalY,
                            0f,
                            0f,
                            width = min(widthRatio, sliceWidth).toFloat(),
                            height = sliceHeight.toFloat(),
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            srcX = sliceX,
                            srcY = sliceY,
                            srcWidth = min(widthRatio, sliceWidth),
                            srcHeight = sliceHeight,
                            colorBits = progressBarColor.toFloatBits()
                        )
                    }
                    FillMode.RIGHT_TO_LEFT -> {
                        batch.draw(
                            it.texture,
                            globalX + sliceWidth - widthRatio,
                            globalY,
                            0f,
                            0f,
                            width = min(widthRatio, sliceWidth).toFloat(),
                            height = sliceHeight.toFloat(),
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            srcX = sliceX + sliceWidth - widthRatio,
                            srcY = sliceY,
                            srcWidth = min(widthRatio, sliceWidth),
                            srcHeight = sliceHeight,
                            colorBits = progressBarColor.toFloatBits()
                        )
                    }
                    FillMode.TOP_TO_BOTTOM -> {
                        batch.draw(
                            it.texture,
                            globalX,
                            globalY,
                            0f,
                            0f,
                            width = sliceWidth.toFloat(),
                            height = min(heightRatio, sliceHeight).toFloat(),
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            srcX = sliceX,
                            srcY = sliceY,
                            srcWidth = sliceWidth,
                            srcHeight = min(heightRatio, sliceHeight),
                            colorBits = progressBarColor.toFloatBits()
                        )
                    }
                    FillMode.BOTTOM_TO_TOP -> {
                        batch.draw(
                            it.texture,
                            globalX,
                            globalY + sliceHeight - heightRatio,
                            0f,
                            0f,
                            width = sliceWidth.toFloat(),
                            height = min(heightRatio, sliceHeight).toFloat(),
                            scaleX = globalScaleX,
                            scaleY = globalScaleY,
                            rotation = globalRotation,
                            srcX = sliceX,
                            srcY = sliceY + sliceHeight - heightRatio,
                            srcWidth = sliceWidth,
                            srcHeight = min(heightRatio, sliceHeight),
                            colorBits = progressBarColor.toFloatBits()
                        )
                    }
                }
            }
            foreground?.let {
                batch.draw(
                    it,
                    globalX,
                    globalY,
                    scaleX = globalScaleX,
                    scaleY = globalScaleY,
                    rotation = globalRotation,
                    colorBits = foregroundColor.toFloatBits()
                )
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        if (useNinePatch) {
            _internalMinWidth = (left + right).toFloat()
            _internalMinHeight = (top + bottom).toFloat()
        } else {
            _internalMinWidth =
                background?.width?.toFloat() ?: foreground?.width?.toFloat() ?: progressBar?.width?.toFloat() ?: 0f
            _internalMinHeight =
                background?.height?.toFloat() ?: foreground?.height?.toFloat() ?: progressBar?.height?.toFloat() ?: 0f
        }

        minSizeInvalid = false
    }

    private fun validateNinePatches() {
        val bg = background
        val prog = progressBar
        val fg = foreground

        if (bg != null) {
            backgroundNine = if (useNinePatch) {
                NinePatch(bg, left, right, bottom, top)
            } else {
                null
            }
        }

        if (prog != null) {
            progressNine = if (useNinePatch) {
                NinePatch(prog, left, right, bottom, top)
            } else {
                null
            }
        }

        if (fg != null) {
            foregroundNine = if (useNinePatch) {
                NinePatch(fg, left, right, bottom, top)
            } else {
                null
            }
        }
    }

    enum class FillMode {
        /**
         * Progress fills from left to right.
         */
        LEFT_TO_RIGHT,

        /**
         * Progress fills from right to left.
         */
        RIGHT_TO_LEFT,

        /**
         * Progress fills from top to bottom.
         */
        TOP_TO_BOTTOM,

        /**
         * Progress fills from bottom to top.
         */
        BOTTOM_TO_TOP
    }
}