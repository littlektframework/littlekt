package com.lehaine.littlekt.graph.node.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.CanvasItem
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.resource.Drawable
import com.lehaine.littlekt.graph.node.resource.InputEvent
import com.lehaine.littlekt.graph.node.resource.OverrideMap
import com.lehaine.littlekt.graph.node.resource.Theme
import com.lehaine.littlekt.graph.node.ui.Control.AnchorLayout.*
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.font.BitmapFont
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.*
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.SingleSignal
import com.lehaine.littlekt.util.internal.isFlagSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * Adds a [Control] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Control] context in order to initialize any values
 * @return the newly created [Control]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.control(callback: @SceneGraphDslMarker Control.() -> Unit = {}): Control {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Control().also(callback).addTo(this)
}

/**
 * Adds a [Control] to the current [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Control] context in order to initialize any values
 * @return the newly created [Control]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.control(callback: @SceneGraphDslMarker Control.() -> Unit = {}): Control {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.control(callback)
}


/**
 * The base [Node] for deriving ui element nodes. Handles size changes, anchoring, and margins.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Control : CanvasItem() {

    /**
     * A [Signal] the is emitted when the [horizontalSizeFlags] or [verticalSizeFlags] are changed.
     */
    val onSizeFlagsChanged: Signal = Signal()

    /**
     * A [Signal] that is emitted when the control changes size.
     */
    @JsName("onSizeChangedSignal")
    val onSizeChanged: Signal = Signal()

    /**
     * A [Signal] that is emitted when the control's minimum size changes via [minWidth] or [minHeight]
     */
    @JsName("onMinimumSizeChangedSignal")
    val onMinimumSizeChanged: Signal = Signal()

    /**
     * A [Signal] that is emitted when the control receives an [InputEvent].
     */
    val onUiInput: SingleSignal<InputEvent<*>> = SingleSignal()

    /**
     * A [Signal] that is emitted when the control gains focus.
     */
    @JsName("onFocusSignal")
    val onFocus: Signal = Signal()

    /**
     * A [Signal] that is emitted when the control loses focus.
     */
    @JsName("onFocusLostSignal")
    val onFocusLost: Signal = Signal()

    private var lastAnchorLayout: AnchorLayout = NONE

    private var _anchorLeft = 0f
    private var _anchorRight = 0f
    private var _anchorBottom = 0f
    private var _anchorTop = 0f

    /**
     * Anchors the left edge of the node to the origin, the center or the end of its parent control.
     * It changes how the left offset updates when the node moves or changes size.
     */
    var anchorLeft: Float
        get() = _anchorLeft
        set(value) {
            if (value == _anchorLeft) return
            lastAnchorLayout = NONE
            setAnchor(Side.LEFT, value)
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Anchors the right edge of the node to the origin, the center or the end of its parent control.
     * It changes how the right offset updates when the node moves or changes size.
     */
    var anchorRight: Float
        get() = _anchorRight
        set(value) {
            if (value == _anchorRight) return
            lastAnchorLayout = NONE
            setAnchor(Side.RIGHT, value)
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Anchors the top edge of the node to the origin, the center or the end of its parent control.
     * It changes how the top offset updates when the node moves or changes size.
     */
    var anchorTop: Float
        get() = _anchorTop
        set(value) {
            if (value == _anchorTop) return
            lastAnchorLayout = NONE
            setAnchor(Side.TOP, value)
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Anchors the bottom edge of the node to the origin, the center or the end of its parent control.
     * It changes how the bottom offset updates when the node moves or changes size.
     */
    var anchorBottom: Float
        get() = _anchorBottom
        set(value) {
            if (value == _anchorBottom) return
            lastAnchorLayout = NONE
            setAnchor(Side.BOTTOM, value)
            if (insideTree) {
                onSizeChanged()
            }
        }

    private var _marginLeft = 0f
    private var _marginRight = 0f
    private var _marginBottom = 0f
    private var _marginTop = 0f

    /**
     * Tells the parent Container nodes how they should resize and place the node on the Y axis.
     * Use one of the [SizeFlag] constants to change the flags.
     */
    var verticalSizeFlags = SizeFlag.FILL
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }

    /**
     * Tells the parent Container nodes how they should resize and place the node on the X axis.
     * Use one of the [SizeFlag] constants to change the flags.
     */
    var horizontalSizeFlags = SizeFlag.FILL
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }

    /**
     * If the node and at least one of its neighbors uses the SIZE_EXPAND size flag,
     * the parent Container will let it take more or less space depending on this property.
     * If this node has a stretch ratio of 2 and its neighbor a ratio of 1, this node
     * will take two thirds of the available space.
     */
    var stretchRatio: Float = 1f
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }

    /**
     * Distance between the node's left edge and its parent control, based on [anchorLeft].
     *
     * Margins are often controlled by one or multiple parent [Container] nodes, so you should not modify them
     * manually if your node is a direct child of a [Container]. Margins update automatically when you move or resize
     * the node.
     */
    var marginLeft: Float
        get() = _marginLeft
        set(value) {
            if (value == _marginLeft) return
            _marginLeft = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Distance between the node's right edge and its parent control, based on [anchorRight].
     *
     * Margins are often controlled by one or multiple parent [Container] nodes, so you should not modify them
     * manually if your node is a direct child of a [Container]. Margins update automatically when you move or resize
     * the node.
     */
    var marginRight: Float
        get() = _marginRight
        set(value) {
            if (value == _marginRight) return
            _marginRight = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Distance between the node's top edge and its parent control, based on [anchorTop].
     *
     * Margins are often controlled by one or multiple parent [Container] nodes, so you should not modify them
     * manually if your node is a direct child of a [Container]. Margins update automatically when you move or resize
     * the node.
     */
    var marginTop: Float
        get() = _marginTop
        set(value) {
            if (value == _marginBottom) return
            _marginTop = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Distance between the node's bottom edge and its parent control, based on [anchorBottom].
     *
     * Margins are often controlled by one or multiple parent [Container] nodes, so you should not modify them
     * manually if your node is a direct child of a [Container]. Margins update automatically when you mvoe or resize
     * the node.
     */
    var marginBottom: Float
        get() = _marginBottom
        set(value) {
            if (value == _marginBottom) return
            _marginBottom = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    /**
     * Controls the direction on the horizontal axis in which the control should grow
     * if its horizontal minimum size is changed to be greater than its current size,
     * as the control always has to be at least the minimum size.
     */
    var horizontalGrowDirection: GrowDirection = GrowDirection.END
        set(value) {
            if (value == field) return
            field = value
            onSizeChanged()
        }

    /**
     * Controls the direction on the vertical axis in which the control should grow if its vertical minimum size is
     * changed to be greater than its current size, as the control always has to be at least the minimum size.
     */
    var verticalGrowDirection: GrowDirection = GrowDirection.END
        set(value) {
            if (value == field) return
            field = value
            onSizeChanged()
        }

    private var _width = 0f
    private var _height = 0f

    /**
     * The width of the node's bounding rectangle, in pixels. [Container] node's update this property automatically.
     */
    var width: Float
        get() = _width
        set(value) {
            size(value, height)
        }

    /**
     * The height of the node's bounding rectangle, in pixels. [Container] node's update this property automatically.
     */
    var height: Float
        get() = _height
        set(value) {
            size(width, value)
        }

    /**
     * The minimum width that this node should use.
     */
    var minWidth: Float = 0f
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /**
     * The minimum height that this node should use.
     */
    var minHeight: Float = 0f
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }


    protected var minSizeInvalid = false

    protected var _internalMinWidth = 0f
    protected var _internalMinHeight = 0f

    /**
     * The internal minimum height of this control. Useful when creating custom [Control] that needs an absolute
     * minimum height.
     *
     * @see combinedMinWidth
     */
    protected var internalMinWidth: Float
        get() {
            if (minSizeInvalid) {
                calculateMinSize()
            }
            return _internalMinWidth
        }
        set(value) {
            if (value == _internalMinWidth) return
            _internalMinWidth = value
            onMinimumSizeChanged()
        }

    /**
     * The internal minimum height of this control. Useful when creating custom [Control] that needs an absolute
     * minimum height.
     *
     * @see combinedMinHeight
     */
    protected var internalMinHeight: Float
        get() {
            if (minSizeInvalid) {
                calculateMinSize()
            }
            return _internalMinHeight
        }
        set(value) {
            if (value == _internalMinHeight) return
            _internalMinHeight = value
            onMinimumSizeChanged()
        }

    /**
     * The combined width of [minWidth] and [internalMinWidth].
     */
    val combinedMinWidth get() = max(minWidth, internalMinWidth)

    /**
     * The combined height of [minHeight] and [internalMinHeight].
     */
    val combinedMinHeight get() = max(minHeight, internalMinHeight)

    private var lastMinWidth = 0f
    private var lastMinHeight = 0f

    private var recomputeMargins = false

    var color = Color.WHITE
    var debugColor = Color.GREEN

    /**
     * The theme of this node and all its [Control] children use.
     */
    var theme: Theme? = null
        set(value) {
            if (field == value) return
            field = value
            _onThemeChanged()
        }

    /**
     * The map of overrides for a theme [Drawable]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val drawableOverrides by lazy { OverrideMap<String, Drawable>(::_onThemeValueChanged) }

    /**
     * A map for storing the result of [getThemeDrawable] when needed to prevent searching the tree every frame.
     */
    val drawableCache by lazy { mutableMapOf<String, Drawable>() }

    /**
     * The map of overrides for a theme [BitmapFont]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val fontOverrides by lazy { OverrideMap<String, com.lehaine.littlekt.graphics.g2d.font.BitmapFont>(::_onThemeValueChanged) }

    /**
     * A map for storing the result of [getThemeFont] when needed to prevent searching the tree every frame.
     */
    val fontCache by lazy { mutableMapOf<String, com.lehaine.littlekt.graphics.g2d.font.BitmapFont>() }

    /**
     * The map of overrides for a theme [Color]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val colorOverrides by lazy { OverrideMap<String, Color>(::_onThemeValueChanged) }

    /**
     * A map for storing the result of [getThemeColor] when needed to prevent searching the tree every frame.
     */
    val colorCache by lazy { mutableMapOf<String, Color>() }

    /**
     * The map of overrides for a theme [Int]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val constantOverrides by lazy { OverrideMap<String, Int>(::_onThemeValueChanged) }

    /**
     * A map for storing the result of [getThemeConstant] when needed to prevent searching the tree every frame.
     */
    val constantCache by lazy { mutableMapOf<String, Int>() }

    /**
     * Controls when the control will be able to receive mouse button input events through [onUiInput] and how these
     * events are handled.
     */
    var mouseFilter = MouseFilter.STOP

    var focusMode = FocusMode.NONE

    var focusNext: Control? = null
    var focusPrev: Control? = null
    var focusNeighborTop: Control? = null
    var focusNeighborRight: Control? = null
    var focusNeighborBottom: Control? = null
    var focusNeighborLeft: Control? = null
    val hasFocus: Boolean get() = scene?.hasFocus(this) ?: false

    private val tempRect = Rect()

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, anchorLeft=$anchorLeft, anchorRight=$anchorRight, anchorTop=$anchorTop, anchorBottom=$anchorBottom, verticalSizeFlags=$verticalSizeFlags, horizontalSizeFlags=$horizontalSizeFlags, marginLeft=$marginLeft, marginRight=$marginRight, marginTop=$marginTop, marginBottom=$marginBottom, horizontalGrowDirection=$horizontalGrowDirection, verticalGrowDirection=$verticalGrowDirection, width=$width, height=$height, minWidth=$minWidth, minHeight=$minHeight, combinedMinWidth=$combinedMinWidth, combinedMinHeight=$combinedMinHeight, color=$color, debugColor=$debugColor"

    override fun debugRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        super.debugRender(batch, camera, shapeRenderer)
        if (globalRotation.normalized.radians.isFuzzyZero()) {
            shapeRenderer.rectangle(
                globalPosition,
                width,
                height,
                globalRotation,
                1,
                color = debugColor.toFloatBits()
            )
        } else {
            val p1x = 0f
            val p1y = 0f
            val p2x = 0f
            val p2y = height
            val p3x = width
            val p3y = height

            var x1: Float
            var y1: Float
            var x2: Float
            var y2: Float
            var x3: Float
            var y3: Float

            val cos = rotation.cosine
            val sin = rotation.sine

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y

            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y

            var x4: Float = x1 + (x3 - x2)
            var y4: Float = y3 - (y2 - y1)


            x1 += globalPosition.x
            y1 += globalPosition.y
            x2 += globalPosition.x
            y2 += globalPosition.y
            x3 += globalPosition.x
            y3 += globalPosition.y
            x4 += globalPosition.x
            y4 += globalPosition.y

            val minX = minOf(x1, x2, x3, x4)
            val minY = minOf(y1, y2, y3, y4)
            val maxX = maxOf(x1, x2, x3, x4)
            val maxY = maxOf(y1, y2, y3, y4)

            shapeRenderer.rectangle(
                minX,
                minY,
                maxX - minX,
                maxY - minY,
                Angle.ZERO,
                1,
                color = debugColor.toFloatBits()
            )
        }
    }

    override fun onPositionChanged() {
        super.onPositionChanged()
        computeMargins()
        if (insideTree) {
            onSizeChanged()
        } else {
            // just in case we set the position before added to a scene - we want to preserve this position and not
            // recompute it to match the margins
            recomputeMargins = true
        }
    }

    override fun onAddedToScene() {
        super.onAddedToScene()
        val parent = parent
        if (parent is Control) {
            parent.onSizeChanged.connect(this, ::onSizeChanged)
        } else {
            canvas?.onSizeChanged?.connect(this, ::onSizeChanged)
            onSizeChanged()
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        dirty(SIZE_DIRTY)
    }

    override fun onDisabled() {
        super.onDisabled()
        val parent = parent
        if (parent is Control && parent.enabled && parent.visible) {
            parent.onSizeChanged()
        }
    }

    override fun onVisible() {
        super.onVisible()
        val parent = parent
        if (parent is Control && parent.enabled && parent.visible) {
            parent.onSizeChanged()
        }
    }

    override fun onInvisible() {
        super.onInvisible()
        val parent = parent
        if (parent is Control && parent.enabled && parent.visible) {
            parent.onSizeChanged()
        }
    }

    override fun onPostEnterScene() {
        super.onPostEnterScene()

        minSizeInvalid = true
        onSizeChanged()
    }

    override fun onRemovedFromScene() {
        val parent = parent
        if (parent is Control) {
            parent.onSizeChanged.disconnect(this)
        } else {
            canvas?.onSizeChanged?.disconnect(this)
        }
        super.onRemovedFromScene()
    }

    internal fun callUiInput(event: InputEvent<*>) {
        if (!enabled || !insideTree) return

        event.apply {
            val localCoords = toLocal(event.canvasX, event.canvasY, tempVec2f)
            localX = localCoords.x
            localY = localCoords.y
        }
        onUiInput.emit(event) // signal is first due to being able to handle the event
        if (event.handled) {
            return
        }
        uiInput(event)
    }

    /**
     * Open method that is to process and accept inputs on UI elements.
     */
    open fun uiInput(event: InputEvent<*>) = Unit

    private fun _onThemeChanged() {
        drawableCache.clear()
        fontCache.clear()
        colorCache.clear()
        constantCache.clear()
        _onThemeValueChanged()
    }

    private fun _onThemeValueChanged() {
        nodes.forEach {
            if (it is Control) {
                it._onThemeChanged()
            }
        }
        onThemeChanged()
        onMinimumSizeChanged()
    }

    /**
     * Open method that is to process and recalculate minimum size when a theme is changed.
     */
    open fun onThemeChanged() = Unit

    internal fun _onFocus() {
        onFocus()
        onFocus.emit()
    }

    internal fun _onFocusLost() {
        onFocusLost()
        onFocusLost.emit()
    }

    /**
     * Open method to process when a control is gain focus.
     */
    open fun onFocus() = Unit

    /**
     * Open method to process when a control loses focus.
     */
    open fun onFocusLost() = Unit

    /**
     * Set the control to a new size.
     * @param newWidth the new width of the bounding rectangle
     * @param newHeight the new height of the bounding rectangle
     */
    fun size(newWidth: Float, newHeight: Float) {
        if (width == newWidth && height == newHeight) {
            return
        }
        _width = if (newWidth < minWidth) {
            minWidth
        } else {
            newWidth
        }

        _height = if (newHeight < minHeight) {
            minHeight
        } else {
            newHeight
        }

        computeMargins()
        if (insideTree) {
            dirty(SIZE_DIRTY)
            onSizeChanged()
        }
    }

    /**
     * Anchor this node using an [AnchorLayout] preset.
     * @param layout the anchor layout preset
     */
    fun anchor(layout: AnchorLayout) {
        lastAnchorLayout = layout
        computeAnchorMarginLayout(layout)
        if (insideTree) {
            onSizeChanged()
        }
    }

    /**
     * Attempts to _hit_ a [Control] node. This will check any children [Control] nodes first and then itself.
     * This will return null if the control is not [enabled] or if [mouseFilter] is set to [MouseFilter.NONE].
     * @param hx the x coord in global
     * @param hy the y coord in global
     * @return a [Control] node that was hit
     */
    fun hit(hx: Float, hy: Float): Control? {
        if (!enabled || mouseFilter == MouseFilter.NONE || !visible) {
            return null
        }

        nodes.forEachReversed {
            val target = it.propagateHit(hx, hy)
            if (target != null) {
                return target
            }
        }
        if (mouseFilter == MouseFilter.IGNORE) return null

        toLocal(hx, hy, tempVec2f)
        val x = tempVec2f.x
        val y = tempVec2f.y

        return if (x >= 0f && x < width && y >= 0f && y < height) this else null
    }

    /**
     * Determines if the point is in the controls bounding rectangle.
     * @param px the x coord in global
     * @param py the y coord in global
     * @return true if it contains; false otherwise
     */
    fun hasPoint(px: Float, py: Float): Boolean {
        toLocal(px, py, tempVec2f)
        val x = tempVec2f.x
        val y = tempVec2f.y
        return x >= 0f && x < width && y >= 0f && y < height
    }

    /**
     * Set the bounding rectangle of this control.
     */
    fun setRect(tx: Float, ty: Float, tWidth: Float, tHeight: Float) {
        _anchorBottom = 0f
        _anchorLeft = 0f
        _anchorRight = 0f
        _anchorTop = 0f
        position(tx, ty)
        size(tWidth, tHeight)
    }

    private fun getParentAnchorableRect(): Rect {
        if (!insideTree) {
            tempRect.set(0f, 0f, 0f, 0f)
        }
        val parent = parent
        if (parent is Control) {
            tempRect.set(0f, 0f, parent.width, parent.height)
        } else {
            val width = canvas?.virtualWidth ?: 0f
            val height = canvas?.virtualHeight ?: 0f
            tempRect.set(
                canvas?.x?.toFloat() ?: 0f, canvas?.y?.toFloat() ?: 0f, width, height
            )
        }
        return tempRect
    }

    private fun computeAnchors() {
        val parentRect = getParentAnchorableRect()
        _anchorLeft = (x - marginLeft) / parentRect.width
        _anchorTop = (y - marginTop) / parentRect.height
        _anchorRight = (x + width - marginRight) / parentRect.width
        _anchorBottom = (y + height - marginBottom) / parentRect.height
    }

    internal fun computeMargins() {
        val parentRect = getParentAnchorableRect()
        _marginLeft = x - (anchorLeft * parentRect.width)
        _marginTop = y - (anchorTop * parentRect.height)
        _marginRight = x + width - (anchorRight * parentRect.width)
        _marginBottom = y + height - (anchorBottom * parentRect.height)
    }

    private fun computeAnchorMarginLayout(layout: AnchorLayout, triggerSizeChanged: Boolean = true) {
        computeAnchorLayout(layout, triggerSizeChanged = triggerSizeChanged)
        computeMarginLayout(layout)
    }

    private fun computeAnchorLayout(
        layout: AnchorLayout, keepMargins: Boolean = true, triggerSizeChanged: Boolean = true,
    ) {
        // LEFT
        when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, TOP_WIDE, BOTTOM_WIDE, LEFT_WIDE, HCENTER_WIDE, WIDE -> setAnchor(
                Side.LEFT, 0f, keepMargins, triggerSizeChanged
            )

            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> setAnchor(
                Side.LEFT, 0.5f, keepMargins, triggerSizeChanged
            )

            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, RIGHT_WIDE -> setAnchor(
                Side.LEFT, 1f, keepMargins, triggerSizeChanged
            )

            else -> {
                // anchors need set manually
            }
        }

        // TOP
        when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, LEFT_WIDE, RIGHT_WIDE, TOP_WIDE, VCENTER_WIDE, WIDE -> setAnchor(
                Side.TOP, 0f, keepMargins, triggerSizeChanged
            )

            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> setAnchor(
                Side.TOP, 0.5f, keepMargins, triggerSizeChanged
            )

            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, BOTTOM_WIDE -> setAnchor(
                Side.TOP, 1f, keepMargins, triggerSizeChanged
            )

            else -> {
                // anchors need set manually
            }
        }

        // RIGHT
        when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, LEFT_WIDE -> setAnchor(
                Side.RIGHT, 0f, keepMargins, triggerSizeChanged
            )

            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> setAnchor(
                Side.RIGHT, 0.5f, keepMargins, triggerSizeChanged
            )

            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, TOP_WIDE, RIGHT_WIDE, BOTTOM_WIDE, HCENTER_WIDE, WIDE -> setAnchor(
                Side.RIGHT, 1f, keepMargins, triggerSizeChanged
            )

            else -> {
                // anchors need set manually
            }
        }

        // BOTTOM
        when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, TOP_WIDE -> setAnchor(
                Side.BOTTOM, 0f, keepMargins, triggerSizeChanged
            )

            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> setAnchor(
                Side.BOTTOM, 0.5f, keepMargins, triggerSizeChanged
            )

            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, LEFT_WIDE, RIGHT_WIDE, BOTTOM_WIDE, VCENTER_WIDE, WIDE -> setAnchor(
                Side.BOTTOM, 1f, keepMargins, triggerSizeChanged
            )

            else -> {
                // anchors need set manually
            }
        }
    }

    private fun computeMarginLayout(
        layout: AnchorLayout,
    ) {
        val parentRect = getParentAnchorableRect()

        // LEFT
        _marginLeft = when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, TOP_WIDE, BOTTOM_WIDE, LEFT_WIDE, HCENTER_WIDE, WIDE -> parentRect.width * (0f - _anchorLeft) + parentRect.x
            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> parentRect.width * (0.5f - _anchorLeft) - combinedMinWidth / 2 + parentRect.x
            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, RIGHT_WIDE -> parentRect.width * (1f - _anchorLeft) - combinedMinWidth + parentRect.x
            else -> _marginLeft
        }

        // TOP
        _marginTop = when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, LEFT_WIDE, RIGHT_WIDE, TOP_WIDE, VCENTER_WIDE, WIDE -> parentRect.height * (0f - _anchorTop) + parentRect.y
            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> parentRect.height * (0.5f - _anchorTop) - combinedMinHeight / 2 + parentRect.y
            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, BOTTOM_WIDE -> parentRect.height * (1f - _anchorTop) - combinedMinHeight + parentRect.y
            else -> _marginTop
        }

        // RIGHT
        _marginRight = when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, LEFT_WIDE -> parentRect.width * (0f - _anchorRight) + combinedMinWidth + parentRect.x
            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> parentRect.width * (0.5f - _anchorRight) + combinedMinWidth / 2 + parentRect.x
            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, TOP_WIDE, RIGHT_WIDE, BOTTOM_WIDE, HCENTER_WIDE, WIDE -> parentRect.width * (1f - _anchorRight) + parentRect.x
            else -> _marginRight
        }

        // BOTTOM
        _marginBottom = when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, TOP_WIDE -> parentRect.height * (0f - _anchorBottom) + combinedMinHeight + parentRect.y
            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> parentRect.height * (0.5f - _anchorBottom) + combinedMinHeight / 2 + parentRect.y
            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, LEFT_WIDE, RIGHT_WIDE, BOTTOM_WIDE, VCENTER_WIDE, WIDE -> parentRect.height * (1f - _anchorBottom) + parentRect.y
            else -> _marginBottom

        }
    }

    fun setAnchor(
        side: Side, value: Float, keepMargins: Boolean = true, triggerSizeChanged: Boolean = true,
    ) {
        val parentRect = getParentAnchorableRect()
        val parentRange =
            if (side == Side.LEFT || side == Side.RIGHT) parentRect.width else parentRect.height
        val prevPos = side.margin + side.anchor * parentRange
        val prevOppositePos = side.oppositeMargin + side.oppositeAnchor * parentRange

        side.anchor = value

        if (((side == Side.LEFT || side == Side.TOP) && side.anchor > side.oppositeAnchor) || ((side == Side.RIGHT || side == Side.BOTTOM) && side.anchor < side.oppositeAnchor)) {
            // push the opposite anchor
            side.oppositeAnchor = side.anchor
        }

        if (!keepMargins) {
            side.margin = prevPos - side.anchor * parentRange
            // push the opposite margin
            side.oppositeMargin = prevOppositePos - side.oppositeAnchor * parentRange
        }

        if (triggerSizeChanged && insideTree) {
            onSizeChanged()
        }
    }

    private var Side.anchor: Float
        get() = when (this) {
            Side.LEFT -> _anchorLeft
            Side.BOTTOM -> _anchorBottom
            Side.RIGHT -> _anchorRight
            Side.TOP -> _anchorTop
        }
        set(value) = when (this) {
            Side.LEFT -> _anchorLeft = value
            Side.BOTTOM -> _anchorBottom = value
            Side.RIGHT -> _anchorRight = value
            Side.TOP -> _anchorTop = value
        }

    private var Side.oppositeAnchor: Float
        get() = when (this) {
            Side.LEFT -> _anchorRight
            Side.BOTTOM -> _anchorTop
            Side.RIGHT -> _anchorLeft
            Side.TOP -> _anchorBottom
        }
        set(value) = when (this) {
            Side.LEFT -> _anchorRight = value
            Side.BOTTOM -> _anchorTop = value
            Side.RIGHT -> _anchorLeft = value
            Side.TOP -> _anchorBottom = value
        }

    private var Side.margin: Float
        get() = when (this) {
            Side.LEFT -> _marginLeft
            Side.BOTTOM -> _marginBottom
            Side.RIGHT -> _marginRight
            Side.TOP -> _marginTop
        }
        set(value) = when (this) {
            Side.LEFT -> _marginLeft = value
            Side.BOTTOM -> _marginBottom = value
            Side.RIGHT -> _marginRight = value
            Side.TOP -> _marginTop = value
        }

    private var Side.oppositeMargin: Float
        get() = when (this) {
            Side.LEFT -> _marginRight
            Side.BOTTOM -> _marginTop
            Side.RIGHT -> _marginLeft
            Side.TOP -> _marginBottom
        }
        set(value) = when (this) {
            Side.LEFT -> _marginRight = value
            Side.BOTTOM -> _marginTop = value
            Side.RIGHT -> _marginLeft = value
            Side.TOP -> _marginBottom = value
        }

    fun grabFocus() {
        scene?.requestFocus(this)
    }

    fun releaseFocus() = scene?.releaseFocus()

    fun findNextValidFocus(): Control? {
        var from: Control = this

        while (true) {
            // if focusNext set manually, attempt to sue it first.
            focusNext?.let {
                if (it.enabled && it.focusMode != FocusMode.NONE) {
                    return it
                }
            }

            var nextChild: Control? = null
            for (i in 0 until from.nodes.size) {
                val child = from.nodes[i] as? Control
                if (child != null && child.enabled) {
                    nextChild = child
                    break
                }
            }

            if (nextChild == null) {
                nextChild = nextControl(from)
                if (nextChild == null) {
                    nextChild = this
                    while (nextChild != null && nextChild.parent is Control) {
                        nextChild = nextChild.parent as Control
                    }
                }
            }

            if (nextChild == this) return if (focusMode == FocusMode.ALL) nextChild else null

            if (nextChild != null) {
                if (nextChild.focusMode == FocusMode.ALL) {
                    return nextChild
                }
                from = nextChild
            } else {
                break
            }
        }
        return null
    }

    private fun nextControl(from: Control): Control? {
        val controlParent = from.parent as? Control ?: return null
        val next = from.index

        for (i in next + 1 until controlParent.nodes.size) {
            val child = controlParent.nodes[i] as? Control
            if (child != null && child.enabled) {
                return child
            }
        }
        return nextControl(controlParent)
    }

    fun findPreviousValidFocus(): Control? {
        var from: Control = this
        while (true) {
            focusPrev?.let {
                if (it.enabled && it.focusMode != FocusMode.NONE) return it
            }

            var prevChild: Control? = null

            if (from.parent !is Control) {
                prevChild = previousControl(from)
            } else {
                for (i in from.index - 1 downTo 0) {
                    val c = from.parent?.nodes?.get(i) as? Control
                    if (c != null && c.enabled) {
                        prevChild = c
                        break
                    }
                }

                prevChild = if (prevChild == null) {
                    from.parent as? Control
                } else {
                    previousControl(prevChild)
                }
            }
            if (prevChild == this) return if (focusMode == FocusMode.ALL) prevChild else null

            if (prevChild != null) {
                if (prevChild.focusMode == FocusMode.ALL) {
                    return prevChild
                }
                from = prevChild
            } else {
                break
            }
        }
        return null
    }

    private fun previousControl(from: Control): Control {
        var child: Control? = null
        for (i in from.nodes.size - 1 downTo 0) {
            val c = from.nodes[i] as? Control
            if (c != null && c.enabled) {
                child = c
                break
            }
        }
        if (child == null) return from
        return previousControl(child)
    }

    internal fun getFocusNeighbor(side: Side, count: Int = 0): Control? {
        if (count >= MAX_NEIGHBOR_SEARCH_COUNT) return null

        side.focusNeighbor?.let {
            if (it.enabled && it.focusMode != FocusMode.NONE) {
                return it
            }
            return it.getFocusNeighbor(side, count + 1)
        }

        val dir = when (side) {
            Side.LEFT -> tempVec2f.set(-1f, 0f)
            Side.TOP -> tempVec2f.set(0f, -1f)
            Side.RIGHT -> tempVec2f.set(1f, 0f)
            Side.BOTTOM -> tempVec2f.set(0f, 1f)
        }
        points2[0].set(globalX, globalY)
        points2[1].set(globalX + width, globalY)
        points2[2].set(globalX + width, globalY + height)
        points2[3].set(globalX, globalY + height)

        var maxDist = -1e7f
        points2.forEach {
            val dot = dir.dot(it)
            if (dot > maxDist) {
                maxDist = dot
            }
        }

        var base: Node? = this

        while (base != null && base.parent is Control) {
            base = base.parent
        }

        if (base == null) return null

        controlResult[0] = null
        floatResult[0] = 1e7f
        findFocusNeighbor(dir, base, points2, maxDist, floatResult, controlResult)
        return controlResult[0]
    }

    private fun findFocusNeighbor(
        dir: Vec2f,
        at: Node,
        fromPoints: Array<MutableVec2f>,
        fromMin: Float,
        closestDist: FloatArray,
        out: Array<Control?>,
    ) {
        val c = at as? Control

        if (c != null && c != this && c.focusMode == FocusMode.ALL && c.enabled) {
            points[0].set(c.globalX, c.globalY)
            points[1].set(c.globalX + c.width, c.globalY)
            points[2].set(c.globalX + c.width, c.globalY + c.height)
            points[3].set(c.globalX, c.globalY + c.height)

            var min = 1e7f

            points.forEach {
                val d = dir.dot(it)
                if (d < min) {
                    min = d
                }
            }

            if (min > (fromMin - FUZZY_EQ_F)) {
                for (i in 0 until 4) {
                    val la = fromPoints[i]
                    val lb = fromPoints[(i + 1) % 4]

                    for (j in 0 until 4) {
                        val fa = points[j]
                        val fb = points[(j + 1) % 4]
                        val d = closestPointsBetweenSegments(la, lb, fa, fb)
                        if (d < closestDist[0]) {
                            closestDist[0] = d
                            out[0] = c
                        }
                    }
                }
            }
        }
        c?.nodes?.forEach {
            findFocusNeighbor(dir, it, fromPoints, fromMin, closestDist, out)
        }
    }

    private val Side.focusNeighbor
        get() = when (this) {
            Side.LEFT -> focusNeighborLeft
            Side.BOTTOM -> focusNeighborBottom
            Side.RIGHT -> focusNeighborRight
            Side.TOP -> focusNeighborTop
        }

    protected fun applyTransform(batch: Batch) {
        tempMat4.set(batch.transformMatrix)
        batch.transformMatrix = localToGlobalTransformMat4
    }

    protected fun resetTransform(batch: Batch) {
        batch.transformMatrix = tempMat4
    }

    private fun onSizeChanged() {
        if (lastAnchorLayout != NONE) {
            computeAnchorMarginLayout(lastAnchorLayout, triggerSizeChanged = false)
        }
        if (recomputeMargins) {
            computeMargins()
            recomputeMargins = false
        }
        val parentRect = getParentAnchorableRect()

        val edgePosLeft = marginLeft + (anchorLeft * parentRect.width)
        val edgePosTop = marginTop + (anchorTop * parentRect.height)
        val edgePosRight = marginRight + (anchorRight * parentRect.width)
        val edgePosBottom = marginBottom + (anchorBottom * parentRect.height)

        var newX = edgePosLeft
        var newY = edgePosTop
        var newWidth = edgePosRight - edgePosLeft
        var newHeight = edgePosBottom - edgePosTop

        if (combinedMinWidth > newWidth) {
            if (horizontalGrowDirection == GrowDirection.BEGIN) {
                newX += newWidth - combinedMinWidth
            } else if (horizontalGrowDirection == GrowDirection.BOTH) {
                newX += 0.5f * (newWidth - combinedMinWidth)
            }
            newWidth = combinedMinWidth
        }

        if (combinedMinHeight > newHeight) {
            if (verticalGrowDirection == GrowDirection.BEGIN) {
                newY += newHeight - combinedMinHeight
            } else if (verticalGrowDirection == GrowDirection.BOTH) {
                newY += 0.5f * (newHeight - combinedMinHeight)
            }
            newHeight = combinedMinHeight
        }

        val posChanged = x != newX || y != newY
        val sizeChanged = _width != newWidth || _height != newHeight

        position(newX, newY, false)
        _width = newWidth
        _height = newHeight

        if (insideTree) {
            if (sizeChanged || posChanged) {
                computeMargins()
                dirty(SIZE_DIRTY)
                onResized()
                onSizeChanged.emit()
            }
        }
    }

    protected open fun onResized() = Unit

    protected open fun calculateMinSize() {
        minSizeInvalid = false
    }

    protected fun onMinimumSizeChanged() {
        minSizeInvalid = true
        updateMinimumSize()
    }

    private fun updateMinimumSize() {
        calculateMinSize()
        minSizeInvalid = false
        if (lastMinWidth != combinedMinWidth || lastMinHeight != combinedMinHeight) {
            lastMinWidth = combinedMinWidth
            lastMinHeight = combinedMinHeight
            if (insideTree) {
                (parent as? Control)?.onMinimumSizeChanged()
                onSizeChanged()
                onMinimumSizeChanged.emit()
            }
        }
    }

    /**
     * Checks if the control has a theme [Drawable].
     */
    fun hasThemeDrawable(name: String, type: String = this::class.simpleName ?: ""): Boolean {
        drawableOverrides[name]?.let { return true }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.drawables?.get(type)?.get(name)?.let { return true }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        return Theme.defaultTheme.drawables[type]?.get(name)?.let { return true } ?: return false
    }

    /**
     * @return a [Drawable] from the first matching [Theme] in the tree that has a [Drawable] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeDrawable(name: String, type: String = this::class.simpleName ?: ""): Drawable {
        drawableOverrides[name]?.let { return it }
        drawableCache[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.drawables?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        val drawable = Theme.defaultTheme.drawables[type]?.get(name) ?: Theme.FALLBACK_DRAWABLE
        drawableCache[name] = drawable
        return drawable
    }

    /**
     * @return a [Color] from the first matching [Theme] in the tree that has a [Color] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeColor(name: String, type: String = this::class.simpleName ?: ""): Color {
        colorOverrides[name]?.let { return it }
        colorCache[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.colors?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        val color = Theme.defaultTheme.colors[type]?.get(name) ?: Color.WHITE
        colorCache[name] = color
        return color
    }

    /**
     * @return a [BitmapFont] from the first matching [Theme] in the tree that has a [BitmapFont] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeFont(
        name: String,
        type: String = this::class.simpleName ?: "",
    ): com.lehaine.littlekt.graphics.g2d.font.BitmapFont {
        fontOverrides[name]?.let { return it }
        fontCache[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.fonts?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        val font = Theme.defaultTheme.fonts[type]?.get(name) ?: Theme.defaultTheme.defaultFont ?: Theme.FALLBACK_FONT
        fontCache[name] = font
        return font
    }

    /**
     * @return a [Int] from the first matching [Theme] in the tree that has a [Int] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeConstant(name: String, type: String = this::class.simpleName ?: ""): Int {
        constantOverrides[name]?.let { return it }
        constantCache[name]?.let { return it }

        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.constants?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        val constant = Theme.defaultTheme.constants[type]?.get(name) ?: 0
        constantCache[name] = constant
        return constant
    }

    /**
     * Clears the local theme [constantOverrides] map.
     */
    fun clearThemeConstantOverrides(): Control {
        constantOverrides.clear()
        _onThemeValueChanged()
        return this
    }

    /**
     * Clears the local theme [fontOverrides] map.
     */
    fun clearThemeFontOverrides(): Control {
        fontOverrides.clear()
        _onThemeValueChanged()
        return this
    }

    /**
     * Clears the local theme [fontOverrides] map.
     */
    fun clearThemeDrawableOverrides(): Control {
        drawableOverrides.clear()
        _onThemeValueChanged()
        return this
    }

    /**
     * Clears the local theme [color] map.
     */
    fun clearThemeColorOverrides(): Control {
        colorOverrides.clear()
        _onThemeValueChanged()
        return this
    }

    /**
     * Clears all the local theme override maps.
     * @see constantOverrides
     * @see fontOverrides
     * @see drawableOverrides
     * @see color
     */
    fun clearThemeOverrides(): Control {
        constantOverrides.clear()
        fontOverrides.clear()
        drawableOverrides.clear()
        colorOverrides.clear()
        _onThemeValueChanged()
        return this
    }

    /**
     * Clears all the local theme cache maps
     * @see constantCache
     * @see fontCache
     * @see drawableOverrides
     * @see colorOverrides
     */
    fun clearThemeCache(): Control {
        constantCache.clear()
        fontCache.clear()
        drawableCache.clear()
        colorOverrides.clear()
        _onThemeValueChanged()
        return this
    }


    override fun onDestroy() {
        super.onDestroy()
        onMinimumSizeChanged.clear()
        onFocus.clear()
        onFocusLost.clear()
        onSizeChanged.clear()
        onSizeFlagsChanged.clear()
        onUiInput.clear()
    }

    enum class AnchorLayout {
        /**
         * Snap all 4 anchors to the top-left of the parent control's bounds.
         */
        TOP_LEFT,

        /**
         * Snap all 4 anchors to the top-right of the parent control's bounds.
         */
        TOP_RIGHT,

        /**
         * Snap all 4 anchors to the bottom-left of the parent control's bounds.
         */
        BOTTOM_LEFT,

        /**
         * Snap all 4 anchors to the bottom-right of the parent control's bounds.
         */
        BOTTOM_RIGHT,

        /**
         * Snap all 4 anchors to the center of the left edge of the parent control's bounds.
         */
        CENTER_LEFT,

        /**
         * Snap all 4 anchors to the center of the top edge of the parent control's bounds.
         */
        CENTER_TOP,

        /**
         * Snap all 4 anchors to the center of the right edge of the parent control's bounds.
         */
        CENTER_RIGHT,

        /**
         * Snap all 4 anchors to the center of the bottom edge of the parent control's bounds.
         */
        CENTER_BOTTOM,

        /**
         * Snap all 4 anchors to the center of the parent control's bounds.
         */
        CENTER,

        /**
         * Snap all 4 anchors to the left edge of the parent control.
         * The left offset becomes relative to the left edge and the top offset relative to the top left corner
         * of the node's parent.
         */
        LEFT_WIDE,

        /**
         * Snap all 4 anchors to the top edge of the parent control. The left offset becomes relative to
         * the top left corner, the top offset relative to the top edge, and the right offset relative
         * to the top right corner of the node's parent
         */
        TOP_WIDE,

        /**
         * Snap all 4 anchors to the right edge of the parent control. The right offset becomes relative
         * to the right edge and the top offset relative to the top right corner of the node's parent.
         */
        RIGHT_WIDE,

        /**
         * Snap all 4 anchors to the bottom edge of the parent control. The left offset becomes relative
         * to the bottom left corner, the bottom offset relative to the bottom edge, and the right offset
         * relative to the bottom right corner of the node's parent.
         */
        BOTTOM_WIDE,

        /**
         * Snap all 4 anchors to a vertical line that cuts the parent control in half.
         */
        VCENTER_WIDE,

        /**
         * Snap all 4 anchors to a horizontal line that cuts the parent control in half.
         */
        HCENTER_WIDE,

        /**
         * Snap all 4 anchors to the respective corners of the parent control.
         * Set all 4 offsets to 0 after you applied this preset and the Control will fit its parent control.
         */
        WIDE,

        /**
         * Anchors will need to be set manually.
         */
        NONE
    }

    enum class FocusMode {
        /**
         * The node cannot grab focus.
         */
        NONE,

        /**
         * The node can only grab focus on mouse clicks.
         */
        CLICK,

        /**
         * The node can grab focus on mouse click or using the arrows and tab keys on keyboard.
         */
        ALL
    }

    enum class MouseFilter {
        /**
         * Stops at the current [Control] which triggers an input event and does not go any further up the graph.
         */
        STOP,

        /**
         * Receives no input events for the current [Control] and its children.
         */
        NONE,

        /**
         * Ignores input events on the current [Control] but still allows events to its children.
         */
        IGNORE
    }

    enum class GrowDirection {
        BEGIN, END, BOTH
    }

    /**
     * Flags that tell the parent [Container] how to expand/shrink the child. Use with [horizontalSizeFlags] and
     * [verticalSizeFlags]
     * @author Colt Daily
     */
    @JvmInline
    value class SizeFlag(val bit: Int) {

        fun isFlagSet(flag: SizeFlag) = bit.isFlagSet(flag.bit)

        infix fun or(flag: SizeFlag) = SizeFlag(bit.or(flag.bit))
        infix fun and(flag: SizeFlag) = SizeFlag(bit.and(flag.bit))

        companion object {
            /**
             * Useful when there is no need for a size flag. Same as using `SizeFlag(0)`.
             */
            val NONE = SizeFlag(0)

            /**
             * Tells the parent Container to expand the bounds of this node to fill all the available
             * space without pushing any other node.
             */
            val FILL = SizeFlag(1 shl 0)

            /**
             * Tells the parent Container to let this node take all the available space on the axis you flag.
             * If multiple neighboring nodes are set to expand, they'll share the space based on their stretch ratio.
             * @see stretchRatio
             */
            val EXPAND = SizeFlag(1 shl 1)

            /**
             * Tells the parent Container to center the node in itself. It centers the control based on its bounding box,
             * so it doesn't work with the fill or expand size flags.
             */
            val SHRINK_CENTER = SizeFlag(1 shl 2)

            /**
             * Tells the parent Container to align the node with its end, either the bottom or the right edge.
             * It doesn't work with the fill or expand size flags
             */
            val SHRINK_END = SizeFlag(1 shl 3)

            private val values =
                arrayOf(FILL, EXPAND, SHRINK_CENTER, SHRINK_END)

            fun values() = values

            fun set(flags: List<SizeFlag>): SizeFlag {
                var bit = flags[0].bit
                flags.forEachIndexed { index, sizeFlag ->
                    if (index != 0) {
                        bit = bit or sizeFlag.bit
                    }
                }
                return SizeFlag(bit)
            }

            operator fun invoke(str: String): SizeFlag = when (str) {
                "FILL" -> FILL
                "EXPAND" -> EXPAND
                "SHRINK_CENTER" -> SHRINK_CENTER
                "SHRINK_END" -> SHRINK_END
                else -> SizeFlag(str.substringAfter('(').substringBefore(')').toIntOrNull() ?: 0)
            }
        }


        override fun toString(): String = when (this) {
            FILL -> "FILL"
            EXPAND -> "EXPAND"
            SHRINK_CENTER -> "SHRINK_CENTER"
            SHRINK_END -> "SHRINK_END"
            else -> "SizeFlag($bit)"
        }
    }

    enum class Side {
        LEFT, BOTTOM, RIGHT, TOP
    }

    companion object {
        const val SIZE_DIRTY = 4

        private val tempMat4 = Mat4()
        private val tempVec2f = MutableVec2f()
        private val points = Array(4) { MutableVec2f() }
        private val points2 = Array(4) { MutableVec2f() }
        private val controlResult = arrayOfNulls<Control>(1)
        private val floatResult = floatArrayOf(0f)
        private const val MAX_NEIGHBOR_SEARCH_COUNT = 512
    }
}

