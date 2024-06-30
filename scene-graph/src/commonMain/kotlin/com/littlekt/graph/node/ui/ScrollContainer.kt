package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.popAndEndCanvasRenderPass
import com.littlekt.graph.node.pushRenderPassToCanvas
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graph.util.signal
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.input.gesture.GestureController
import com.littlekt.input.gesture.GestureProcessor
import com.littlekt.math.MutableVec2f
import com.littlekt.math.floor
import com.littlekt.util.seconds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Adds a [ScrollContainer] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.scrollContainer(
    callback: @SceneGraphDslMarker ScrollContainer.() -> Unit = {}
): ScrollContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return ScrollContainer().also(callback).addTo(this)
}

/**
 * Adds a [ScrollContainer] to the current [SceneGraph.root] as a child and then triggers the
 * [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.scrollContainer(
    callback: @SceneGraphDslMarker ScrollContainer.() -> Unit = {}
): ScrollContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.scrollContainer(callback)
}

/**
 * A [Container] node that will automatically create a [ScrollBar] child ([HScrollBar] or
 * [VScrollBar], or both) when needed.
 *
 * @author Colton Daily
 * @date 10/16/2022
 */
class ScrollContainer : Container(), GestureProcessor {

    /** Triggered when the container scrolling is started */
    val onScroll = signal()

    /** Triggered when the container scrolling is stopped */
    val onScrollStop = signal()

    /** The panel [Drawable]. */
    var panel: Drawable
        get() = getThemeDrawable(themeVars.panel)
        set(value) {
            drawableOverrides[themeVars.panel] = value
        }

    /** The [ScrollMode] for horizontal scrolling. */
    var horizontalScrollMode: ScrollMode = ScrollMode.AUTO

    /** The [ScrollMode] for vertical scrolling. */
    var verticalScrollMode: ScrollMode = ScrollMode.AUTO

    /** If `true` scrolling will work by clicking & dragging / flinging the contents. */
    var dragToScroll = true

    private var hScrollBar: HScrollBar = hScrollBar {
        onValueChanged.connect(this, ::onScrollMoved)
    }
    private var vScrollBar: VScrollBar = vScrollBar {
        onValueChanged.connect(this, ::onScrollMoved)
    }
    private var largestChildWidth = 0f
    private var largestChildHeight = 0f
    private var shiftPressed = false

    private val velocity = MutableVec2f()
    private var flingTimer = Duration.ZERO
    private var flingTime = 1.seconds

    private val gestureController by lazy { GestureController(context.input, processor = this) }

    init {
        mouseFilter = MouseFilter.STOP
    }

    override fun ready() {
        super.ready()
        updateScrollbarPosition()
        repositionChildren()
    }

    override fun onThemeChanged() {
        super.onThemeChanged()
        updateScrollbarPosition()
    }

    override fun uiInput(event: InputEvent<*>) {
        super.uiInput(event)

        when (event.type) {
            InputEvent.Type.KEY_DOWN -> {
                if (event.key == Key.SHIFT_LEFT) {
                    shiftPressed = true
                }
            }
            InputEvent.Type.KEY_UP -> {
                if (event.key == Key.SHIFT_LEFT) {
                    shiftPressed = false
                }
            }
            InputEvent.Type.SCROLLED -> {
                val prevHScroll = hScrollBar.value
                val prevVScroll = vScrollBar.value
                if (hScrollBar.visible && (!vScrollBar.visible || shiftPressed)) {
                    hScrollBar.value += hScrollBar.page / 8f * -event.scrollAmountY.sign
                } else if (vScrollBar.visible) {
                    vScrollBar.value += vScrollBar.page / 8f * -event.scrollAmountY.sign
                }
                if (prevHScroll != hScrollBar.value || prevVScroll != vScrollBar.value) {
                    event.handle()
                    onScroll.emit()
                }
            }
            InputEvent.Type.TOUCH_DOWN -> {
                if (dragToScroll) {
                    val result =
                        gestureController.touchDown(event.canvasX, event.canvasY, event.pointer)
                    if (result) {
                        event.handle()
                    }
                }
            }
            InputEvent.Type.TOUCH_DRAGGED -> {
                if (dragToScroll) {
                    val result =
                        gestureController.touchDragged(event.canvasX, event.canvasY, event.pointer)
                    if (result) {
                        event.handle()
                    }
                }
            }
            InputEvent.Type.TOUCH_UP -> {
                if (dragToScroll) {
                    val result =
                        gestureController.touchUp(event.canvasX, event.canvasY, event.pointer)
                    if (result) {
                        event.handle()
                    }
                }
            }
            else -> {
                // nothing
            }
        }
    }

    override fun unhandledInput(event: InputEvent<*>) {
        super.unhandledInput(event)

        when (event.type) {
            InputEvent.Type.KEY_DOWN -> {
                if (event.key == Key.SHIFT_LEFT) {
                    shiftPressed = true
                }
            }
            InputEvent.Type.KEY_UP -> {
                if (event.key == Key.SHIFT_LEFT) {
                    shiftPressed = false
                }
            }
            else -> {
                // do nothing
            }
        }
    }

    override fun touchDown(screenX: Float, screenY: Float, pointer: Pointer): Boolean {
        onScrollStop.emit()
        velocity.set(0f, 0f)
        flingTimer = Duration.ZERO
        return true
    }

    override fun fling(velocityX: Float, velocityY: Float, pointer: Pointer): Boolean {
        if (velocityX.absoluteValue > 150f) {
            velocity.x = velocityX
        } else {
            velocity.x = 0f
        }
        if (velocityY.absoluteValue > 150f) {
            velocity.y = -velocityY
        } else {
            velocity.y = 0f
        }
        if (velocityX != 0f || velocityY != 0f) {
            onScroll.emit()
            flingTimer = flingTime
        }
        return true
    }

    override fun pan(screenX: Float, screenY: Float, dx: Float, dy: Float): Boolean {
        onScroll.emit()
        if (hScrollBar.visible) {
            hScrollBar.value -= dx
        }

        if (vScrollBar.visible) {
            vScrollBar.value += dy
        }
        return true
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        largestChildWidth = 0f
        largestChildHeight = 0f
        _internalMinWidth = 0f
        _internalMinHeight = 0f

        nodes.forEach {
            if (it !is Control || !it.visible || !it.enabled || it.isDestroyed) return@forEach
            if (it == hScrollBar || it == vScrollBar) return@forEach

            largestChildWidth = max(it.combinedMinWidth, largestChildWidth)
            largestChildHeight = max(it.combinedMinHeight, largestChildHeight)
        }

        if (horizontalScrollMode == ScrollMode.DISABLED) {
            _internalMinWidth = max(_internalMinWidth, largestChildWidth)
        }

        if (verticalScrollMode == ScrollMode.DISABLED) {
            _internalMinHeight = max(_internalMinHeight, largestChildHeight)
        }

        val showHorizontalScroll =
            horizontalScrollMode == ScrollMode.ALWAYS ||
                (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > _internalMinWidth)
        val showVerticalScroll =
            verticalScrollMode == ScrollMode.ALWAYS ||
                (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > _internalMinHeight)

        if (showHorizontalScroll && hScrollBar.parent == this) {
            _internalMinHeight += hScrollBar.combinedMinHeight
        }
        if (showVerticalScroll && vScrollBar.parent == this) {
            _internalMinWidth += vScrollBar.combinedMinWidth
        }

        _internalMinWidth += panel.minWidth
        _internalMinHeight += panel.minHeight

        minSizeInvalid = false
    }

    override fun update(dt: Duration) {
        super.update(dt)
        // move the scroll bars to bottom of parent to ensure they get the click events
        if (vScrollBar.index != nodes.size - 1) {
            moveChild(vScrollBar, nodes.size - 1)
        }
        if (hScrollBar.index != nodes.size - 2) {
            moveChild(hScrollBar, nodes.size - 2)
        }

        if (flingTimer > Duration.ZERO) {
            val alpha = flingTimer.seconds / flingTime.seconds
            if (hScrollBar.visible) {
                hScrollBar.value += -velocity.x * alpha * dt.seconds
            }

            if (vScrollBar.visible) {
                vScrollBar.value += -velocity.y * alpha * dt.seconds
            }
            flingTimer -= dt
            if (flingTimer <= Duration.ZERO) {
                velocity.set(0f, 0f)
                onScrollStop.emit()
            }
        }
    }

    override fun preRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.preRender(batch, camera, shapeRenderer)
        if (batch.drawing) {
            batch.flush(canvasRenderPass)
        }
        popAndEndCanvasRenderPass()
        pushRenderPassToCanvas("ScrollContainer Pass")
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val canvas = canvas ?: return

        panel.draw(
            batch,
            globalX,
            globalY,
            width,
            height,
            globalScaleX,
            globalScaleY,
            globalRotation
        )

        tempVec2.set(globalX, globalY)
        tempVec2.mul(batch.transformMatrix)
        canvas.canvasToScreenCoordinates(tempVec2)
        val scissorX = tempVec2.x
        val scissorY = tempVec2.y
        tempVec2.set(globalX + width, globalY + height)
        tempVec2.mul(batch.transformMatrix)
        canvas.canvasToScreenCoordinates(tempVec2)
        val scissorWidth = tempVec2.x - scissorX
        val scissorHeight = tempVec2.y - scissorY

        canvas.setScissorRect(
            scissorX.toInt(),
            scissorY.toInt(),
            scissorWidth.toInt(),
            scissorHeight.toInt()
        )
    }

    override fun postRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.postRender(batch, camera, shapeRenderer)
        if (batch.drawing) {
            batch.flush(canvasRenderPass)
        }
        popAndEndCanvasRenderPass()
        pushRenderPassToCanvas("Canvas Pass")
    }

    override fun onSortChildren() {
        repositionChildren()
    }

    private fun updateScrollbars() {
        var width = width
        var height = height
        width -= panel.minWidth
        height -= panel.minHeight

        hScrollBar.visible =
            horizontalScrollMode == ScrollMode.ALWAYS ||
                (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > width)
        vScrollBar.visible =
            verticalScrollMode == ScrollMode.ALWAYS ||
                (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > height)

        hScrollBar.max = largestChildWidth
        hScrollBar.page =
            if (vScrollBar.visible && vScrollBar.parent == this) width - vScrollBar.width else width

        vScrollBar.max = largestChildHeight
        vScrollBar.page =
            if (hScrollBar.visible && hScrollBar.parent == this) height - hScrollBar.height
            else height

        hScrollBar.setAnchor(Side.RIGHT, 1f, false)
        if (vScrollBar.visible && vScrollBar.parent == this) {
            hScrollBar.marginRight = -vScrollBar.width
        }
        vScrollBar.setAnchor(Side.TOP, 1f, false)
        if (hScrollBar.visible && hScrollBar.parent == this) {
            vScrollBar.marginBottom = hScrollBar.height
        }
    }

    private fun repositionChildren() {
        updateScrollbars()
        var width = width
        var height = height
        width -= panel.minWidth
        height -= panel.minHeight

        nodes.forEach {
            if (it !is Control || !it.visible || !it.enabled || it.isDestroyed) return@forEach
            if (it == hScrollBar || it == vScrollBar) return@forEach

            var tx = -hScrollBar.value
            var ty = vScrollBar.value + height
            var tw = it.combinedMinWidth
            var th = it.combinedMinHeight

            if (it.horizontalSizing.isFlagSet(SizeFlag.EXPAND)) {
                tw = max(width, it.combinedMinWidth)
            }
            if (it.verticalSizing.isFlagSet(SizeFlag.EXPAND)) {
                th = max(height, it.combinedMinHeight)
            }
            tx += panel.marginLeft
            ty += panel.marginBottom - th
            tx = tx.floor()
            ty = ty.floor()
            fitChild(it, tx, ty, tw, th)
        }
    }

    private fun onScrollMoved(value: Float) {
        queueSort()
    }

    private fun updateScrollbarPosition() {
        hScrollBar.anchorLeft = 0f
        hScrollBar.marginLeft = 0f
        hScrollBar.anchorRight = 1f
        hScrollBar.marginRight = 0f
        hScrollBar.anchorBottom = 0f
        hScrollBar.marginBottom = 0f
        hScrollBar.anchorTop = 0f
        hScrollBar.marginTop = 0f

        vScrollBar.anchorLeft = 1f
        vScrollBar.marginLeft = -vScrollBar.width
        vScrollBar.anchorRight = 1f
        vScrollBar.marginRight = 0f
        vScrollBar.anchorBottom = 0f
        vScrollBar.marginBottom = 0f
        vScrollBar.anchorTop = 1f
        vScrollBar.marginTop = 0f
    }

    /** The type of scrolling. */
    enum class ScrollMode {
        /** Scrolling is disabled and the scrollbar is always invisible. */
        DISABLED,

        /**
         * Scrolling is enabled and the scrollbar will only be visible when necessary, such as when
         * the content is too large to fit inside the container.
         */
        AUTO,

        /** Scrolling is enabled and the scrollbar is always visible. */
        ALWAYS,

        /** Scroll is disabled and the scrollbar is hidden. */
        NEVER
    }

    class ThemeVars {
        val panel = "panel"
    }

    companion object {

        /** [Theme] related variable names when setting theme values for a [ScrollContainer] */
        val themeVars = ThemeVars()
        private val tempVec2 = MutableVec2f()
    }
}
