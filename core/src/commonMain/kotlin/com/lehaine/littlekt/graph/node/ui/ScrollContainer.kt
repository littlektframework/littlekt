package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.math.floor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.time.Duration


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
class ScrollContainer : Container() {
    var horizontalScrollMode: ScrollMode = ScrollMode.AUTO
    var verticalScrollMode: ScrollMode = ScrollMode.AUTO

    private var hScrollBar: HScrollBar = hScrollBar {
        onValueChanged.connect(this, ::onScrollMoved)
    }
    private var vScrollBar: VScrollBar = vScrollBar {
        onValueChanged.connect(this, ::onScrollMoved)
    }
    private var largestChildWidth = 0f
    private var largestChildHeight = 0f

    override fun ready() {
        super.ready()
        updateScrollbarPosition()
        repositionChildren()
    }

    override fun calculateMinSize() {
        largestChildWidth = 0f
        largestChildHeight = 0f

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
            _internalMinHeight += vScrollBar.minWidth
        }
        val panel = getThemeDrawable(themeVars.panel)

        _internalMinHeight += panel.minWidth
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
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.render(batch, camera, shapeRenderer)
        val gl = context.gl

        val panel = getThemeDrawable(themeVars.panel)
        panel.draw(batch, globalX, globalY, width, height, globalScaleX, globalScaleY, globalRotation)

        //     batch.flush()

        gl.enable(State.SCISSOR_TEST)
        //   gl.scissor(globalX.toInt(), globalY.toInt(), width.toInt(), height.toInt())
    }

    override fun postRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.postRender(batch, camera, shapeRenderer)
        val gl = context.gl
        // batch.flush()
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
        val hMinHeight = hScrollBar.combinedMinHeight
        val vMinWidth = vScrollBar.combinedMinWidth

        hScrollBar.visible =
            horizontalScrollMode == ScrollMode.ALWAYS || (horizontalScrollMode == ScrollMode.AUTO && largestChildWidth > width)
        vScrollBar.visible =
            verticalScrollMode == ScrollMode.ALWAYS || (verticalScrollMode == ScrollMode.AUTO && largestChildHeight > height)

        hScrollBar.max = largestChildWidth
        hScrollBar.page = if (vScrollBar.visible && vScrollBar.parent == this) width - vMinWidth else width

        vScrollBar.max = largestChildHeight
        vScrollBar.page = if (hScrollBar.visible && hScrollBar.parent == this) height - hMinHeight else height

        hScrollBar.setAnchor(Side.RIGHT, 1f, false)
        hScrollBar.marginRight = if (vScrollBar.visible && vScrollBar.parent == this) -vMinWidth else 0f
        vScrollBar.setAnchor(Side.BOTTOM, 1f, false)
        vScrollBar.marginBottom = if (hScrollBar.visible && hScrollBar.parent == this) -hMinHeight else 0f
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
            ty += panel.marginRight
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
    }
}