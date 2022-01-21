package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.*
import com.lehaine.littlekt.graph.node.component.AnchorLayout.*
import com.lehaine.littlekt.graph.node.node2d.Node2D
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.SingleSignal
import kotlin.js.JsName
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

enum class GrowDirection {
    BEGIN, END, BOTH
}

private enum class AnchorSide {
    LEFT, BOTTOM, RIGHT, TOP
}

/**
 * The base [Node] for deriving ui element nodes. Handles size changes, anchoring, and margins.
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Control : Node2D() {

    companion object {
        const val SIZE_DIRTY = 4
        private val tempMat4 = Mat4()
    }

    val onSizeFlagsChanged: Signal = Signal()

    @JsName("onSizeChangedSignal")
    val onSizeChanged: Signal = Signal()

    @JsName("onMinimumSizeChangedSignal")
    val onMinimumSizeChanged: Signal = Signal()
    val onUiInput: SingleSignal<InputEvent> = SingleSignal()

    private var lastAnchorLayout: AnchorLayout = NONE

    private var _anchorLeft = 0f
    private var _anchorRight = 0f
    private var _anchorBottom = 0f
    private var _anchorTop = 0f

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

    var verticalSizeFlags = SizeFlag.FILL
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }
    var horizontalSizeFlags = SizeFlag.FILL
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }

    var stretchRatio: Float = 1f
        set(value) {
            if (value == field) return
            field = value
            onSizeFlagsChanged.emit()
        }

    var marginLeft: Float
        get() = _marginLeft
        set(value) {
            if (value == _marginLeft) return
            _marginLeft = value
            if (insideTree) {
                onSizeChanged()
            }
        }
    var marginRight: Float
        get() = _marginRight
        set(value) {
            if (value == _marginRight) return
            _marginRight = value
            if (insideTree) {
                onSizeChanged()
            }
        }
    var marginTop: Float
        get() = _marginTop
        set(value) {
            if (value == _marginBottom) return
            _marginTop = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    var marginBottom: Float
        get() = _marginBottom
        set(value) {
            if (value == _marginBottom) return
            _marginBottom = value
            if (insideTree) {
                onSizeChanged()
            }
        }

    var horizontalGrowDirection: GrowDirection = GrowDirection.END
        set(value) {
            if (value == field) return
            field = value
            onSizeChanged()
        }
    var verticalGrowDirection: GrowDirection = GrowDirection.END
        set(value) {
            if (value == field) return
            field = value
            onSizeChanged()
        }

    private var _width = 0f
    private var _height = 0f

    var width: Float
        get() = _width
        set(value) {
            if (value == _width) return
            _width = if (value < minWidth) {
                minWidth
            } else {
                value
            }
            if (_marginRight < value) {
                _marginRight = value
            }
            computeAnchors()
            onSizeChanged()
        }
    var height: Float
        get() = _height
        set(value) {
            if (value == _height) return
            _height = if (value < minHeight) {
                minHeight
            } else {
                value
            }
            if (_marginTop < value) {
                _marginTop = value
            }
            computeAnchors()
            onSizeChanged()
        }


    var minWidth: Float = 0f
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }
    var minHeight: Float = 0f
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }


    protected var minSizeInvalid = false

    protected var _internalMinWidth = 0f
    protected var _internalMinHeight = 0f

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

    val combinedMinWidth get() = max(minWidth, internalMinWidth)
    val combinedMinHeight get() = max(minHeight, internalMinHeight)

    private var lastMinWidth = 0f
    private var lastMinHeight = 0f

    var color = Color.WHITE
    var debugColor = Color.GREEN

    var theme: Theme? = null

    val drawableOverrides by lazy { mutableMapOf<String, Drawable>() }
    val fontOverrides by lazy { mutableMapOf<String, BitmapFont>() }
    val colorOverrides by lazy { mutableMapOf<String, Color>() }
    val constantOverrides by lazy { mutableMapOf<String, Int>() }

    private val tempRect = Rect()

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, anchorLeft=$anchorLeft, anchorRight=$anchorRight, anchorTop=$anchorTop, anchorBottom=$anchorBottom, verticalSizeFlags=$verticalSizeFlags, horizontalSizeFlags=$horizontalSizeFlags, marginLeft=$marginLeft, marginRight=$marginRight, marginTop=$marginTop, marginBottom=$marginBottom, horizontalGrowDirection=$horizontalGrowDirection, verticalGrowDirection=$verticalGrowDirection, width=$width, height=$height, minWidth=$minWidth, minHeight=$minHeight, combinedMinWidth=$combinedMinWidth, combinedMinHeight=$combinedMinHeight, color=$color, debugColor=$debugColor"

    override fun _onAddedToScene() {
        super._onAddedToScene()
        val parent = parent
        if (parent is Control) {
            parent.onSizeChanged.connect(this, ::onSizeChanged)
        } else {
            viewport?.onSizeChanged?.connect(this, ::onSizeChanged)
        }
    }

    override fun _onPostEnterScene() {
        super._onPostEnterScene()

        minSizeInvalid = true
        onSizeChanged()
    }

    override fun _onRemovedFromScene() {
        val parent = parent
        if (parent is Control) {
            parent.onSizeChanged.disconnect(this)
        } else {
            viewport?.onSizeChanged?.disconnect(this)
        }
        super._onRemovedFromScene()
    }

    internal open fun _uiInput(event: InputEvent) {
        if (!enabled) return
        onUiInput.emit(event) // signal is first due to being able to handle the event
        if (!insideTree || event.handled) {
            return
        }
        uiInput(event)
    }

    open fun uiInput(event: InputEvent) {}

    fun size(newWidth: Float, newHeight: Float) {
        println("$name update wh $width,$height to $newWidth,$newHeight")
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

        computeAnchors()
        onSizeChanged()
    }

    fun anchor(layout: AnchorLayout) {
        lastAnchorLayout = layout
        computeAnchorMarginLayout(layout)
        if (insideTree) {
            onSizeChanged()
        }
    }

    fun hit(hx: Float, hy: Float): Control? {
        if (!enabled) {
            return null
        }
        nodes.forEachReversed {
            if (it !is Control) return@forEachReversed
            val target = it.hit(hx, hy)
            if (target != null) {
                return target
            }
        }
        if (globalRotation == Angle.ZERO) {
            return if (hx >= globalX && hx < globalX + width && hy >= globalY && hy < globalY + height) this else null
        }
        // TODO determine hit target when rotated
        return null
    }

    fun hasPoint(px: Float, py: Float): Boolean {
        if (globalRotation == Angle.ZERO) {
            return px >= globalX && px < globalX + width && py >= globalY && py < globalY + height
        }
        return false //TODO determine has point when rotated
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
        _anchorRight = (x + width - marginLeft) / parentRect.width
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

    protected fun applyTransform(batch: SpriteBatch) {
        tempMat4.set(batch.transformMatrix)
        batch.transformMatrix = localToGlobalTransformMat4
    }

    protected fun resetTransform(batch: SpriteBatch) {
        batch.transformMatrix = tempMat4
    }

    private fun onSizeChanged() {
        if (lastAnchorLayout != NONE) {
            computeAnchorMarginLayout(lastAnchorLayout, triggerSizeChanged = false)
        }
        val parentRect = getParentAnchorableRect()

        val edgePosLeft = marginLeft + (anchorLeft * parentRect.width)
        val edgePosTop = marginTop + (anchorTop * parentRect.height)
        val edgePosRight = (anchorRight * parentRect.width) - marginRight
        val edgePosBottom = (anchorBottom * parentRect.height) - marginBottom

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

        position(newX, newY)
        _width = newWidth
        _height = newHeight

        if (insideTree) {
            if (sizeChanged || posChanged) {
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
        (parent as? Control)?.onMinimumSizeChanged()
    }

    private fun updateMinimumSize() {
        calculateMinSize()
        minSizeInvalid = false
        if (lastMinWidth != combinedMinWidth || lastMinHeight != combinedMinHeight) {
            lastMinWidth = combinedMinWidth
            lastMinHeight = combinedMinHeight
            onSizeChanged()
            onMinimumSizeChanged.emit()
        }
    }

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

    fun clearThemeConstantOverrides(): Control {
        constantOverrides.clear()
        return this
    }

    fun clearThemeFontOverrides(): Control {
        fontOverrides.clear()
        return this
    }

    fun clearThemeDrawableOverrides(): Control {
        drawableOverrides.clear()
        return this
    }

    fun clearThemeColorOverrides(): Control {
        colorOverrides.clear()
        return this
    }

    fun clearThemeOverrides(): Control {
        clearThemeFontOverrides()
        clearThemeConstantOverrides()
        clearThemeDrawableOverrides()
        clearThemeColorOverrides()
        return this
    }
}

