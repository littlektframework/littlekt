package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.input.gesture.GestureController
import com.lehaine.littlekt.input.gesture.GestureProcessor
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.floor
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.seconds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * Adds a [ScrollContainer] to the current [Node] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.scrollContainer(callback: @SceneGraphDslMarker ScrollContainer.() -> Unit = {}): ScrollContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return ScrollContainer().also(callback).addTo(this)
}

/**
 * Adds a [ScrollContainer] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.scrollContainer(callback: @SceneGraphDslMarker ScrollContainer.() -> Unit = {}): ScrollContainer {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.scrollContainer(callback)
}

/**
 * A [Container] node that will automatically create a [ScrollBar] child ([HScrollBar] or [VScrollBar], or both)
 * when needed.
 * @author Colton Daily
 * @date 10/16/2022
 */
class ScrollContainer : Container(), GestureProcessor {
    var horizontalScrollMode: ScrollMode = ScrollMode.AUTO
    var verticalScrollMode: ScrollMode = ScrollMode.AUTO

    /**
     * If `true` scrolling will work by clicking & dragging / flinging the contents.
     */
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
        repositionChildren()
    }

    override fun onThemeChanged() {
        super.onThemeChanged()
        updateScrollbarPosition()
    }

    override fun onPostEnterScene() {
        super.onPostEnterScene()
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
                    hScrollBar.value = hScrollBar.value + hScrollBar.page / 8f * event.scrollAmountY
                } else if (vScrollBar.visible) {
                    vScrollBar.value = vScrollBar.value + vScrollBar.page / 8f * event.scrollAmountY
                }
                if (prevHScroll != hScrollBar.value || prevVScroll != vScrollBar.value) {
                    event.handle()
                }
            }

            InputEvent.Type.TOUCH_DOWN -> {
                if(dragToScroll) {
                    val result = gestureController.touchDown(event.sceneX, event.sceneY, event.pointer)
                    if (result) {
                        event.handle()
                    }
                }
            }

            InputEvent.Type.TOUCH_DRAGGED -> {
                if(dragToScroll) {
                    val result = gestureController.touchDragged(event.sceneX, event.sceneY, event.pointer)
                    if (result) {
                        event.handle()
                    }
                }
            }

            InputEvent.Type.TOUCH_UP -> {
                if(dragToScroll) {
                    val result = gestureController.touchUp(event.sceneX, event.sceneY, event.pointer)
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
        velocity.set(0f, 0f)
        flingTimer = Duration.ZERO
        return true
    }

    override fun fling(velocityX: Float, velocityY: Float, pointer: Pointer): Boolean {
        if (velocityX.absoluteValue > 0.15f) {
            velocity.x = velocityX
        } else {
            velocity.x = 0f
        }
        if (velocityY.absoluteValue > 0.15f) {
            velocity.y = velocityY
        } else {
            velocity.y = 0f
        }
        if(velocityX != 0f || velocityY != 0f) {
            flingTimer = flingTime
        }
        return true
    }

    override fun pan(screenX: Float, screenY: Float, dx: Float, dy: Float): Boolean {
        if (hScrollBar.visible) {
            hScrollBar.value -= dx
        }

        if (vScrollBar.visible) {
            vScrollBar.value -= dy
        }
        return true
    }

    override fun calculateMinSize() {
        largestChildWidth = 0f
        largestChildHeight = 0f
        _internalMinWidth = 0f
        _internalMinHeight = 0f

        nodes.forEach {
            if (it !is Control || !it.visible) return@forEach
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
            horizontalScrollMode == ScrollMode.ALWAYS || (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > _internalMinWidth)
        val showVerticalScroll =
            verticalScrollMode == ScrollMode.ALWAYS || (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > _internalMinHeight)

        if (showHorizontalScroll && hScrollBar.parent == this) {
            _internalMinHeight += hScrollBar.minHeight
        }
        if (showVerticalScroll && vScrollBar.parent == this) {
            _internalMinWidth += vScrollBar.minWidth
        }
        val panel = getThemeDrawable(themeVars.panel)

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
                hScrollBar.value += -velocity.x * alpha * dt.milliseconds
            }

            if (vScrollBar.visible) {
                vScrollBar.value += -velocity.y * alpha * dt.milliseconds
            }
            flingTimer -= dt
            if (flingTimer <= Duration.ZERO) {
                velocity.set(0f, 0f)
            }
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val canvas = canvas ?: return
        val gl = context.gl

        val panel = getThemeDrawable(themeVars.panel)
        panel.draw(batch, globalX, globalY, width, height, globalScaleX, globalScaleY, globalRotation)

        batch.flush()
        tempVec2.set(globalX, globalY)
        tempVec2.mul(batch.transformMatrix)
        canvas.canvasToScreenCoordinates(tempVec2)
        val scissorX = tempVec2.x
        val scissorY = tempVec2.y
        tempVec2.set(
            globalX + width + if (vScrollBar.visible) vScrollBar.combinedMinWidth else 0f,
            globalY + height + if (hScrollBar.visible) hScrollBar.combinedMinHeight else 0f
        )
        tempVec2.mul(batch.transformMatrix)
        canvas.canvasToScreenCoordinates(tempVec2)
        val scissorWidth = tempVec2.x - scissorX
        val scissorHeight = tempVec2.y - scissorY

        gl.enable(State.SCISSOR_TEST)
        gl.scissor(
            scissorX.toInt(),
            context.graphics.height - scissorY.toInt() - scissorHeight.toInt(), // need to flip the y-coord to y-up
            scissorWidth.toInt(),
            scissorHeight.toInt()
        )
    }

    override fun postRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.postRender(batch, camera, shapeRenderer)
        val gl = context.gl
        batch.flush()
        gl.disable(State.SCISSOR_TEST)
    }

    override fun onSortChildren() {
        repositionChildren()
    }

    private fun updateScrollbars() {
        var width = width
        var height = height
        val panel = getThemeDrawable(themeVars.panel)
        width -= panel.minWidth
        height -= panel.minHeight

        hScrollBar.visible =
            horizontalScrollMode == ScrollMode.ALWAYS || (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > width)
        vScrollBar.visible =
            verticalScrollMode == ScrollMode.ALWAYS || (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > height)

        hScrollBar.max = largestChildWidth
        hScrollBar.page = width

        vScrollBar.max = largestChildHeight
        vScrollBar.page = height

        hScrollBar.setAnchor(Side.RIGHT, 1f, false)
        vScrollBar.setAnchor(Side.BOTTOM, 1f, false)
    }

    private fun repositionChildren() {
        updateScrollbars()
        var width = width
        var height = height
        val panel = getThemeDrawable(themeVars.panel)
        width -= panel.minWidth
        height -= panel.minHeight

        if (hScrollBar.visible && hScrollBar.parent == this) {
            height -= hScrollBar.minHeight
        }

        if (vScrollBar.visible && vScrollBar.parent == this) {
            width -= vScrollBar.minWidth
        }

        nodes.forEach {
            if (it !is Control || !it.visible) return@forEach
            if (it == hScrollBar || it == vScrollBar) return@forEach

            var tx = -hScrollBar.value
            var ty = -vScrollBar.value
            var tw = it.combinedMinWidth
            var th = it.combinedMinHeight

            if (it.horizontalSizeFlags.isFlagSet(SizeFlag.EXPAND)) {
                tw = max(width, it.combinedMinWidth)
            }
            if (it.verticalSizeFlags.isFlagSet(SizeFlag.EXPAND)) {
                th = max(height, it.combinedMinHeight)
            }
            tx += panel.marginLeft
            ty += panel.marginTop
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
        hScrollBar.anchorTop = 1f
        hScrollBar.marginTop = -hScrollBar.minHeight
        hScrollBar.anchorBottom = 1f
        hScrollBar.marginBottom = 0f

        vScrollBar.anchorLeft = 1f
        vScrollBar.marginLeft = -vScrollBar.minWidth
        vScrollBar.anchorRight = 1f
        vScrollBar.marginRight = 0f
        vScrollBar.anchorTop = 0f
        vScrollBar.marginTop = 0f
        vScrollBar.anchorBottom = 1f
        vScrollBar.marginBottom = 0f
    }

    enum class ScrollMode {
        DISABLED,
        AUTO,
        ALWAYS,
        NEVER
    }

    class ThemeVars {
        val panel = "panel"
    }

    companion object {

        /**
         * [Theme] related variable names when setting theme values for a [ScrollContainer]
         */
        val themeVars = ThemeVars()
        private val tempVec2 = MutableVec2f()
    }
}