package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.SizeFlag
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.Signal
import kotlin.math.floor
import kotlin.time.Duration

/**
 * Adds a [Container] to the current [Node] as a child and then triggers the [callback]
 */
inline fun Node.container(callback: @SceneGraphDslMarker Container.() -> Unit = {}) =
    Container().also(callback).addTo(this)

/**
 * Adds a [Container] to the current [SceneGraph.root] as a child and then triggers the [callback]
 */
inline fun SceneGraph.container(callback: @SceneGraphDslMarker Container.() -> Unit = {}) = root.container(callback)

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Container : Control() {

    val presortChildrenSignal = Signal()
    val sortChildrenSignal = Signal()

    private var pendingSort = false

    init {
        debugColor = Color.RED
    }

    override fun _onAddedToScene() {
        super._onAddedToScene()
        pendingSort = false
        queueSort()
    }

    override fun _onChildAdded(child: Node) {
        super._onChildAdded(child)
        if (child !is Control) return

        child.sizeFlagsChangedSignal.connect(this, ::queueSort)
        child.minimumSizeChangedSignal.connect(this, ::onChildMinimumSizeChanged)

        onMinimumSizeChanged()
        queueSort()
    }

    override fun _onChildRemoved(child: Node) {
        super._onChildRemoved(child)
        if (child !is Control) return
        child.sizeFlagsChangedSignal.disconnect(this)
        child.minimumSizeChangedSignal.disconnect(this)

        onMinimumSizeChanged()
        queueSort()
    }

    override fun _onHierarchyChanged(flag: Int) {
        super._onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY) {
            queueSort()
        }
    }

    override fun _update(dt: Duration) {
        super._update(dt)

        if (pendingSort) {
            sortChildren()
        }
    }

    fun queueSort() {
        if (!insideTree) return
        if (pendingSort) return

        pendingSort = true
    }

    private fun sortChildren() {
        presortChildrenSignal.emit()
        sortChildrenSignal.emit()
        onSortChildren()
        pendingSort = false
    }

    protected open fun onSortChildren() {

    }

    protected open fun onChildMinimumSizeChanged() {
        onMinimumSizeChanged()
        queueSort()
    }

    protected open fun fitChild(child: Control, tx: Float, ty: Float, tWidth: Float, tHeight: Float) {
        val minWidth = child.combinedMinWidth
        val minHeight = child.combinedMinHeight

        var newWidth = tWidth
        var newHeight = tHeight
        var newX = tx
        var newY = ty

        if (!child.horizontalSizeFlags.isFlagSet(SizeFlag.FILL)) {
            newWidth = minWidth
            if (child.horizontalSizeFlags.isFlagSet(SizeFlag.SHRINK_END)) {
                newX += tWidth - minWidth
            } else if (child.horizontalSizeFlags.isFlagSet(SizeFlag.SHRINK_CENTER)) {
                newX += floor((tWidth - minWidth) * 0.5f)
            }
        }

        if (!child.verticalSizeFlags.isFlagSet(SizeFlag.FILL)) {
            newHeight = minHeight
            if (child.verticalSizeFlags.isFlagSet(SizeFlag.SHRINK_END)) {
                newY += tHeight - minHeight
            } else if (child.verticalSizeFlags.isFlagSet(SizeFlag.SHRINK_CENTER)) {
                newY += floor((tHeight - minHeight) * 0.5f)
            }
        }

        child.position(newX, newY)
        child.size(newWidth, newHeight)
        child.rotation = Angle.ZERO
        child.scale(1f, 1f)
    }

}