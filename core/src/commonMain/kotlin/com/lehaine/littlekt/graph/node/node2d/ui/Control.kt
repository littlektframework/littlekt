package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.component.OverrideMap
import com.lehaine.littlekt.graph.node.component.Theme
import com.lehaine.littlekt.graph.node.node2d.Node2D
import com.lehaine.littlekt.graph.node.node2d.ui.Control.AnchorLayout.*
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.SingleSignal
import com.lehaine.littlekt.util.internal.isFlagSet
import kotlin.js.JsName
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * Adds a [Control] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Control] context in order to initialize any values
 * @return the newly created [Control]
 */
inline fun Node.control(callback: @SceneGraphDslMarker Control.() -> Unit = {}) = Control().also(callback).addTo(this)

/**
 * Adds a [Control] to the current [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Control] context in order to initialize any values
 * @return the newly created [Control]
 */
inline fun SceneGraph.control(callback: @SceneGraphDslMarker Control.() -> Unit = {}) = root.control(callback)


/**
 * The base [Node] for deriving ui element nodes. Handles size changes, anchoring, and margins.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Control : Node2D() {

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
    val onUiInput: SingleSignal<InputEvent> = SingleSignal()

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
            setAnchor(AnchorSide.LEFT, value)
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
            setAnchor(AnchorSide.RIGHT, value)
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
            setAnchor(AnchorSide.TOP, value)
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
            setAnchor(AnchorSide.BOTTOM, value)
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
    val drawableOverrides by lazy { OverrideMap<String, Drawable>(::_onThemeChanged) }

    /**
     * The map of overrides for a theme [BitmapFont]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val fontOverrides by lazy { OverrideMap<String, BitmapFont>(::_onThemeChanged) }

    /**
     * The map of overrides for a theme [Color]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val colorOverrides by lazy { OverrideMap<String, Color>(::_onThemeChanged) }

    /**
     * The map of overrides for a theme [Int]. Local overrides always take precedence when fetching theme items
     * for the control. An override can be removed with [remove].
     */
    val constantOverrides by lazy { OverrideMap<String, Int>(::_onThemeChanged) }

    /**
     * Controls when the control will be able to receive mouse button input events through [onUiInput] and how these
     * events are handled.
     */
    var mouseFilter = MouseFilter.STOP

    private val tempRect = Rect()

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, anchorLeft=$anchorLeft, anchorRight=$anchorRight, anchorTop=$anchorTop, anchorBottom=$anchorBottom, verticalSizeFlags=$verticalSizeFlags, horizontalSizeFlags=$horizontalSizeFlags, marginLeft=$marginLeft, marginRight=$marginRight, marginTop=$marginTop, marginBottom=$marginBottom, horizontalGrowDirection=$horizontalGrowDirection, verticalGrowDirection=$verticalGrowDirection, width=$width, height=$height, minWidth=$minWidth, minHeight=$minHeight, combinedMinWidth=$combinedMinWidth, combinedMinHeight=$combinedMinHeight, color=$color, debugColor=$debugColor"

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
            parent.onSizeChanged.connect(this) {
                onSizeChanged()
            }
        } else {
            viewport?.onSizeChanged?.connect(this, ::onSizeChanged)
            onSizeChanged()
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
            viewport?.onSizeChanged?.disconnect(this)
        }
        super.onRemovedFromScene()
    }

    internal fun _uiInput(event: InputEvent) {
        if (!enabled) return
        onUiInput.emit(event) // signal is first due to being able to handle the event
        if (!insideTree || event.handled) {
            return
        }
        uiInput(event)
    }

    /**
     * Open method that is to process and accept inputs on UI elements.
     */
    open fun uiInput(event: InputEvent) = Unit

    private fun _onThemeChanged() {
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
     * @param hx the x coord
     * @param hy the y coord
     * @return a [Control] node that was hit
     */
    fun hit(hx: Float, hy: Float): Control? {
        if (!enabled || mouseFilter == MouseFilter.NONE) {
            return null
        }
        nodes.forEachReversed {
            if (it !is Control) return@forEachReversed
            val target = it.hit(hx, hy)
            if (target != null) {
                return target
            }
        }
        if (mouseFilter == MouseFilter.IGNORE) return null

        if (globalRotation == Angle.ZERO) {
            return if (hx >= globalX && hx < globalX + width && hy >= globalY && hy < globalY + height) this else null
        }
        // TODO determine hit target when rotated

        return null
    }

    /**
     * Determines if the point is in the controls bounding rectangle.
     * @return true if it contains; false otherwise
     */
    fun hasPoint(px: Float, py: Float): Boolean {
        if (globalRotation == Angle.ZERO) {
            return px >= globalX && px < globalX + width && py >= globalY && py < globalY + height
        }
        return false //TODO determine has point when rotated
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
            val width = viewport?.virtualWidth?.toFloat() ?: 0f
            val height = viewport?.virtualHeight?.toFloat() ?: 0f
            tempRect.set(
                viewport?.x?.toFloat() ?: 0f, viewport?.y?.toFloat() ?: 0f, width, height
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
        layout: AnchorLayout, keepMargins: Boolean = true, triggerSizeChanged: Boolean = true
    ) {
        // LEFT
        when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, TOP_WIDE, BOTTOM_WIDE, LEFT_WIDE, HCENTER_WIDE, WIDE -> setAnchor(
                AnchorSide.LEFT, 0f, keepMargins, triggerSizeChanged
            )
            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> setAnchor(
                AnchorSide.LEFT, 0.5f, keepMargins, triggerSizeChanged
            )
            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, RIGHT_WIDE -> setAnchor(
                AnchorSide.LEFT, 1f, keepMargins, triggerSizeChanged
            )
            else -> {
                // anchors need set manually
            }
        }

        // TOP
        when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, LEFT_WIDE, RIGHT_WIDE, TOP_WIDE, VCENTER_WIDE, WIDE -> setAnchor(
                AnchorSide.TOP, 0f, keepMargins, triggerSizeChanged
            )
            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> setAnchor(
                AnchorSide.TOP, 0.5f, keepMargins, triggerSizeChanged
            )
            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, BOTTOM_WIDE -> setAnchor(
                AnchorSide.TOP, 1f, keepMargins, triggerSizeChanged
            )
            else -> {
                // anchors need set manually
            }
        }

        // RIGHT
        when (layout) {
            TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT, LEFT_WIDE -> setAnchor(
                AnchorSide.RIGHT, 0f, keepMargins, triggerSizeChanged
            )
            CENTER_TOP, CENTER_BOTTOM, CENTER, VCENTER_WIDE -> setAnchor(
                AnchorSide.RIGHT, 0.5f, keepMargins, triggerSizeChanged
            )
            TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT, TOP_WIDE, RIGHT_WIDE, BOTTOM_WIDE, HCENTER_WIDE, WIDE -> setAnchor(
                AnchorSide.RIGHT, 1f, keepMargins, triggerSizeChanged
            )
            else -> {
                // anchors need set manually
            }
        }

        // BOTTOM
        when (layout) {
            TOP_LEFT, TOP_RIGHT, CENTER_TOP, TOP_WIDE -> setAnchor(
                AnchorSide.BOTTOM, 0f, keepMargins, triggerSizeChanged
            )
            CENTER_LEFT, CENTER_RIGHT, CENTER, HCENTER_WIDE -> setAnchor(
                AnchorSide.BOTTOM, 0.5f, keepMargins, triggerSizeChanged
            )
            BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM, LEFT_WIDE, RIGHT_WIDE, BOTTOM_WIDE, VCENTER_WIDE, WIDE -> setAnchor(
                AnchorSide.BOTTOM, 1f, keepMargins, triggerSizeChanged
            )
            else -> {
                // anchors need set manually
            }
        }
    }

    private fun computeMarginLayout(
        layout: AnchorLayout
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

    private fun setAnchor(
        side: AnchorSide, value: Float, keepMargins: Boolean = true, triggerSizeChanged: Boolean = true
    ) {
        val parentRect = getParentAnchorableRect()
        val parentRange =
            if (side == AnchorSide.LEFT || side == AnchorSide.RIGHT) parentRect.width else parentRect.height
        val prevPos = side.margin + side.anchor * parentRange
        val prevOppositePos = side.oppositeMargin + side.oppositeAnchor * parentRange

        side.anchor = value

        if (((side == AnchorSide.LEFT || side == AnchorSide.TOP) && side.anchor > side.oppositeAnchor) || ((side == AnchorSide.RIGHT || side == AnchorSide.BOTTOM) && side.anchor < side.oppositeAnchor)) {
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

    private var AnchorSide.anchor: Float
        get() = when (this) {
            AnchorSide.LEFT -> _anchorLeft
            AnchorSide.BOTTOM -> _anchorBottom
            AnchorSide.RIGHT -> _anchorRight
            AnchorSide.TOP -> _anchorTop
        }
        set(value) = when (this) {
            AnchorSide.LEFT -> _anchorLeft = value
            AnchorSide.BOTTOM -> _anchorBottom = value
            AnchorSide.RIGHT -> _anchorRight = value
            AnchorSide.TOP -> _anchorTop = value
        }

    private var AnchorSide.oppositeAnchor: Float
        get() = when (this) {
            AnchorSide.LEFT -> _anchorRight
            AnchorSide.BOTTOM -> _anchorTop
            AnchorSide.RIGHT -> _anchorLeft
            AnchorSide.TOP -> _anchorBottom
        }
        set(value) = when (this) {
            AnchorSide.LEFT -> _anchorRight = value
            AnchorSide.BOTTOM -> _anchorTop = value
            AnchorSide.RIGHT -> _anchorLeft = value
            AnchorSide.TOP -> _anchorBottom = value
        }

    private var AnchorSide.margin: Float
        get() = when (this) {
            AnchorSide.LEFT -> _marginLeft
            AnchorSide.BOTTOM -> _marginBottom
            AnchorSide.RIGHT -> _marginRight
            AnchorSide.TOP -> _marginTop
        }
        set(value) = when (this) {
            AnchorSide.LEFT -> _marginLeft = value
            AnchorSide.BOTTOM -> _marginBottom = value
            AnchorSide.RIGHT -> _marginRight = value
            AnchorSide.TOP -> _marginTop = value
        }

    private var AnchorSide.oppositeMargin: Float
        get() = when (this) {
            AnchorSide.LEFT -> _marginRight
            AnchorSide.BOTTOM -> _marginTop
            AnchorSide.RIGHT -> _marginLeft
            AnchorSide.TOP -> _marginBottom
        }
        set(value) = when (this) {
            AnchorSide.LEFT -> _marginRight = value
            AnchorSide.BOTTOM -> _marginTop = value
            AnchorSide.RIGHT -> _marginLeft = value
            AnchorSide.TOP -> _marginBottom = value
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
                onSizeChanged.emit()
            }
        }
    }

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
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.drawables?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        return Theme.defaultTheme.drawables[type]?.get(name) ?: Theme.FALLBACK_DRAWABLE
    }

    /**
     * @return a [Color] from the first matching [Theme] in the tree that has a [Color] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeColor(name: String, type: String = this::class.simpleName ?: ""): Color {
        colorOverrides[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.colors?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        return Theme.defaultTheme.colors[type]?.get(name) ?: Color.WHITE
    }

    /**
     * @return a [BitmapFont] from the first matching [Theme] in the tree that has a [BitmapFont] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeFont(name: String, type: String = this::class.simpleName ?: ""): BitmapFont {
        fontOverrides[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.fonts?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        return Theme.defaultTheme.fonts[type]?.get(name) ?: Theme.defaultTheme.defaultFont ?: Theme.FALLBACK_FONT
    }

    /**
     * @return a [Int] from the first matching [Theme] in the tree that has a [Int] with the specified
     * [name] and [type]. If [type] is omitted the class name of the current control is used as the type.
     */
    fun getThemeConstant(name: String, type: String = this::class.simpleName ?: ""): Int {
        constantOverrides[name]?.let { return it }
        var themeOwner: Control? = this
        while (themeOwner != null) {
            themeOwner.theme?.constants?.get(type)?.get(name)?.let { return it }
            themeOwner = if (themeOwner.parent is Control) {
                themeOwner.parent as Control
            } else {
                null
            }
        }
        return Theme.defaultTheme.constants[type]?.get(name) ?: 0
    }

    /**
     * Clears the local theme [constantOverrides] map.
     */
    fun clearThemeConstantOverrides(): Control {
        constantOverrides.clear()
        _onThemeChanged()
        return this
    }

    /**
     * Clears the local theme [fontOverrides] map.
     */
    fun clearThemeFontOverrides(): Control {
        fontOverrides.clear()
        _onThemeChanged()
        return this
    }

    /**
     * Clears the local theme [fontOverrides] map.
     */
    fun clearThemeDrawableOverrides(): Control {
        drawableOverrides.clear()
        _onThemeChanged()
        return this
    }

    /**
     * Clears the local theme [color] map.
     */
    fun clearThemeColorOverrides(): Control {
        colorOverrides.clear()
        _onThemeChanged()
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
        _onThemeChanged()
        return this
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
     * Flags the tell the parent [Container] how to expand/shrink the child. Use with [horizontalSizeFlags] and
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

    private enum class AnchorSide {
        LEFT, BOTTOM, RIGHT, TOP
    }

    companion object {
        const val SIZE_DIRTY = 4
        private val tempMat4 = Mat4()
    }
}

