package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.util.SingleSignal
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.font.TextureFont
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

@PublishedApi
internal fun createWindow(title: String?): Window =
    Window().also {
        if (title != null) it.title = title
    }

/**
 * Adds a [Window] to the current [Node] as a child and then triggers the [callback].
 *
 * @param title the text of the title bar
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.window(
    title: String? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker Window.() -> Unit = {},
): Window {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return createWindow(title).addTo(this, modifier, callback)
}

/**
 * Adds a [Window] to the current [SceneGraph.root] as a child and then triggers the [callback].
 *
 * @param title the text of the title bar
 * @param modifier the [Modifier] to apply
 * @param callback the block to configure the node
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.window(
    title: String? = null,
    modifier: Modifier = Modifier,
    callback: @SceneGraphDslMarker Window.() -> Unit = {},
): Window {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.window(title, modifier = modifier, callback = callback)
}

/**
 * A [PanelContainer] with a title bar that can be dragged to move it. Its children fit the area
 * under the title bar. Dragging moves the window's position, so put it in a parent that doesn't
 * lay out its children, such as the scene root or a plain [Control].
 *
 * ```
 * window("Tools") {
 *     x = 20f
 *     y = 20f
 *     column { button("Paint") }
 * }
 * ```
 *
 * @author Colton Daily
 * @date 11/27/2026
 */
open class Window : PanelContainer() {
    private val scaledText = ScaledText()
    private val layout = GlyphLayout()
    private var dragging = false
    private var lastX = 0f
    private var lastY = 0f
    private var titleDirty = true
    private var laidOutHeight = -1f

    /** The text of the title bar. */
    var title: String = ""
        set(value) {
            if (value == field) return
            field = value
            titleDirty = true
            onMinimumSizeChanged()
        }

    /** Whether dragging the title bar moves the window. */
    var draggable = true

    /** Whether a drag keeps the window inside the scene's canvas. */
    var keepInCanvas = true

    /** Triggered with `true` when a title bar drag starts and `false` when it ends. */
    val onDrag: SingleSignal<Boolean> = SingleSignal()

    /** The background of the title bar. */
    var titleBar: Drawable
        get() = getThemeDrawable(themeVars.titleBar)
        set(value) {
            drawableOverrides[themeVars.titleBar] = value
        }

    /** The height, in pixels, of the title bar. */
    var titleHeight: Int
        get() = getThemeConstant(themeVars.titleHeight)
        set(value) {
            constantOverrides[themeVars.titleHeight] = value
            onMinimumSizeChanged()
        }

    /** The color of the title. */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /** The font of the title. */
    var font: TextureFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            titleDirty = true
            onMinimumSizeChanged()
        }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return
        super.calculateMinSize()
        layoutTitle()
        _internalMinHeight += titleHeight
        _internalMinWidth = max(_internalMinWidth, layout.width + TITLE_PADDING * 2f)
        minSizeInvalid = false
    }

    override fun onSortChildren() {
        val w = width - panel.marginRight - panel.marginLeft
        val h = height - panel.marginBottom - panel.marginTop - titleHeight
        nodes.forEach {
            if (it is Control && it.enabled && !it.isDestroyed) {
                fitChild(it, panel.marginLeft - originX, panel.marginBottom - originY, w, h)
            }
        }
    }

    private fun layoutTitle() {
        val font = scaledText.update(font, pixelScale)
        val scale = 1f / scaledText.ratio
        layout.setText(font, title, Color.WHITE, 0f, scaleX = scale, scaleY = scale, HAlign.LEFT, false, null)
        val lineHeight = font.metrics.lineHeight * scale
        val ty = height - titleHeight + (titleHeight - lineHeight) * 0.5f + lineHeight
        scaledText.cache.setText(layout, scaledText.snap(TITLE_PADDING) - originX, scaledText.snap(ty - lineHeight) - originY, scale, scale)
        laidOutHeight = height
        titleDirty = false
    }

    override fun onHierarchyChanged(flag: Int) {
        super.onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY) onMinimumSizeChanged()
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val bar = titleBar
        val th = titleHeight.toFloat()
        bar.draw(batch, globalX - originX, globalY - originY + height - th, 0f, 0f, width, th, color = bar.tint)
        if (scaledText.font == null || pixelScale != scaledText.pixelScale) {
            titleDirty = true
            onMinimumSizeChanged()
        }
        if (titleDirty || laidOutHeight != height) layoutTitle()
        tempColor.set(color).mul(fontColor)
        scaledText.cache.tint(tempColor)
        scaledText.cache.setPosition(scaledText.snap(globalX), scaledText.snap(globalY))
        scaledText.draw(batch)
    }

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)
        when (event.type) {
            InputEvent.Type.TOUCH_DOWN -> {
                if (draggable && event.localY >= height - titleHeight) {
                    dragging = true
                    lastX = event.canvasX
                    lastY = event.canvasY
                    onDrag.emit(true)
                }
                event.handle()
            }
            InputEvent.Type.TOUCH_DRAGGED -> {
                if (!dragging) return
                moveBy(event.canvasX - lastX, event.canvasY - lastY)
                lastX = event.canvasX
                lastY = event.canvasY
                event.handle()
            }
            InputEvent.Type.TOUCH_UP -> {
                if (dragging) {
                    dragging = false
                    onDrag.emit(false)
                    event.handle()
                }
            }
            else -> {}
        }
    }

    /** Moves the window by ([dx], [dy]), staying inside the canvas when [keepInCanvas] is set. */
    fun moveBy(dx: Float, dy: Float) {
        var nx = x + dx
        var ny = y + dy
        val canvas = scene?.sceneCanvas
        if (keepInCanvas && canvas != null) {
            val gx = globalX - x
            val gy = globalY - y
            nx = nx.coerceIn(-gx, max(-gx, canvas.virtualWidth - width - gx))
            ny = ny.coerceIn(-gy, max(-gy, canvas.virtualHeight - height - gy))
        }
        x = nx
        y = ny
    }

    override fun onDestroy() {
        super.onDestroy()
        onDrag.clear()
    }

    /** [Theme] related variable names of a [Window], besides [PanelContainer.themeVars]. */
    class ThemeVars {
        val titleBar = "titleBar"
        val titleHeight = "titleHeight"
        val fontColor = "fontColor"
        val font = "font"
    }

    companion object {
        private const val TITLE_PADDING = 10f
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for a [Window]. */
        val themeVars = ThemeVars()
    }
}
