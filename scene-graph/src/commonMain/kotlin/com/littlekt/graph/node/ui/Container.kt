package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.util.Signal
import com.littlekt.graphics.Color
import com.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.math.floor
import kotlin.time.Duration

/**
 * Adds a [Container] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Container] context in order to initialize
 *   any values
 * @return the newly created [Container]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.container(callback: @SceneGraphDslMarker Container.() -> Unit = {}): Container {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Container().also(callback).addTo(this)
}

/**
 * Adds a [Container] to the current [SceneGraph.root] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Container] context in order to initialize
 *   any values
 * @return the newly created [Container]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.container(
    callback: @SceneGraphDslMarker Container.() -> Unit = {}
): Container {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.container(callback)
}

/**
 * A [Control] node that all containers inherit from.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Container : Control() {

    /** A [Signal] that is triggered before [sortChildren] is invoked. */
    val onPresortChildren = Signal()

    /** A [Signal] for when [sortChildren] is invoked. */
    @JsName("onSortChildrenSignal") val onSortChildren = Signal()

    private var pendingSort = false

    /**
     * Set any child [Control] nodes [Control.verticalSizing] to use this size flag when added to
     * this container. This will override any child size flags set. If individual size flags need to
     * be set on a per-node basis, then set to this `null`.
     *
     * Example:
     * ```
     * row {
     *   childrenVerticalSizing = SizeFlag.SHRINK_CENTER // set all the children to vertically align to the center
     *   button {
     *     text = "I am a button"
     *   }
     *   label {
     *     text = "I am a label"
     *   }
     * }
     * ```
     */
    var childrenVerticalSizing: SizeFlag? = null
        set(value) {
            if (field == value) return
            nodes.forEach { child ->
                if (child is Control) {
                    if (value != null) {
                        child.verticalSizing = value
                    } else {
                        child.verticalSizing = SizeFlag.FILL
                    }
                }
            }
            field = value
        }

    /**
     * Set any child [Control] nodes [Control.horizontalSizing] to use this size flag when added to
     * this container. This will override any child size flags set. If individual size flags need to
     * be set on a per-node basis, then set to this `null`.
     *
     * Example:
     * ```
     * column {
     *   childrenHorizontalSizing = SizeFlag.SHRINK_CENTER // set all the children to horizontally align to the center
     *   button {
     *     text = "I am a button"
     *   }
     *   label {
     *     text = "I am a label"
     *   }
     * }
     * ```
     */
    var childrenHorizontalSizing: SizeFlag? = null
        set(value) {
            if (field == value) return
            nodes.forEach { child ->
                if (child is Control) {
                    if (value != null) {
                        child.horizontalSizing = value
                    } else {
                        child.horizontalSizing = SizeFlag.FILL
                    }
                }
            }
            field = value
        }

    init {
        mouseFilter = MouseFilter.IGNORE
        debugColor = Color.RED
    }

    override fun onAddedToScene() {
        super.onAddedToScene()
        sortChildren()
    }

    override fun onChildAdded(child: Node) {
        super.onChildAdded(child)
        if (child !is Control) return
        child.onVisible.connect(this, ::onChildMinimumSizeChanged)
        child.onInvisible.connect(this, ::onChildMinimumSizeChanged)
        child.onEnabled.connect(this, ::onChildMinimumSizeChanged)
        child.onDisabled.connect(this, ::onChildMinimumSizeChanged)
        child.onSizeFlagsChanged.connect(this, ::queueSort)
        child.onMinimumSizeChanged.connect(this, ::onChildMinimumSizeChanged)
        childrenVerticalSizing?.let { child.verticalSizing = it }
        childrenHorizontalSizing?.let { child.horizontalSizing = it }

        onMinimumSizeChanged()
        queueSort()
    }

    override fun onChildRemoved(child: Node) {
        super.onChildRemoved(child)
        if (child !is Control) return
        child.onVisible.disconnect(this)
        child.onInvisible.disconnect(this)
        child.onEnabled.disconnect(this)
        child.onDisabled.disconnect(this)
        child.onSizeFlagsChanged.disconnect(this)
        child.onMinimumSizeChanged.disconnect(this)

        childrenVerticalSizing?.let { child.verticalSizing = SizeFlag.FILL }
        childrenHorizontalSizing?.let { child.horizontalSizing = SizeFlag.FILL }

        onMinimumSizeChanged()
        queueSort()
    }

    override fun onHierarchyChanged(flag: Int) {
        super.onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY) {
            queueSort()
        }
    }

    override fun update(dt: Duration) {
        super.update(dt)

        if (pendingSort) {
            sortChildren()
        }
    }

    /** Enqueue this container to sort it's children on the next [update]. */
    fun queueSort() {
        if (!insideTree) return
        if (pendingSort) return

        pendingSort = true
    }

    private fun sortChildren() {
        onPresortChildren.emit()
        onSortChildren.emit()
        onSortChildren()
        pendingSort = false
    }

    /** Sort children nodes within the container. */
    protected open fun onSortChildren() {}

    /** Triggered when a child [Control] minimum size has changed. */
    protected open fun onChildMinimumSizeChanged() {
        onMinimumSizeChanged()
        queueSort()
    }

    /**
     * Calculate a child [Control] to fit within the container.
     *
     * @param child the child node to fit
     * @param tx the child's target x-position
     * @param ty the child's target y-position
     * @param tWidth the child's target width
     * @param tHeight the child's target height
     */
    protected open fun fitChild(
        child: Control,
        tx: Float,
        ty: Float,
        tWidth: Float,
        tHeight: Float
    ) {
        check(child.parent == this) {
            "Trying to fit a Control in a container that isn't it's child!"
        }
        childrenVerticalSizing?.let { child.verticalSizing = it }
        childrenHorizontalSizing?.let { child.horizontalSizing = it }

        val minWidth = child.combinedMinWidth
        val minHeight = child.combinedMinHeight

        var newWidth = tWidth
        var newHeight = tHeight
        var newX = tx
        var newY = ty

        if (!child.horizontalSizing.isFlagSet(SizeFlag.FILL)) {
            newWidth = minWidth
            if (child.horizontalSizing.isFlagSet(SizeFlag.SHRINK_END)) {
                newX += tWidth - minWidth
            } else if (child.horizontalSizing.isFlagSet(SizeFlag.SHRINK_CENTER)) {
                newX += floor((tWidth - minWidth) * 0.5f)
            }
        }

        if (!child.verticalSizing.isFlagSet(SizeFlag.FILL)) {
            newHeight = minHeight
            newY = ty
            if (child.verticalSizing.isFlagSet(SizeFlag.SHRINK_END)) {
                newY += tHeight - minHeight
            } else if (child.verticalSizing.isFlagSet(SizeFlag.SHRINK_CENTER)) {
                newY += tHeight - minHeight
                newY -= floor((tHeight - minHeight) * 0.5f)
            }
        }

        child.setRect(newX, newY, newWidth, newHeight)
        child.rotation = Angle.ZERO
        child.scale(1f, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPresortChildren.clear()
        onSortChildren.clear()
    }
}
