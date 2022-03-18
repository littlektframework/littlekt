package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.internal.NodeList
import com.lehaine.littlekt.graph.node.ui.Control
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.util.*
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Duration

/**
 * Adds a [Node] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}): Node {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Node().also(callback).addTo(this)
}

/**
 * Adds the specified [Node] to the current [Node] as a child and then triggers the [callback]. This can be used
 * for classes that extend node that perhaps they don't have a DSL method to initialize it.
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Node> Node.node(node: T, callback: @SceneGraphDslMarker T.() -> Unit = {}): T {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return node.also(callback).addTo(this)
}

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}): Node {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.node(callback)
}

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]. This can be used
 * for classes that extend node that perhaps they don't have a DSL method to initialize it.
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Node> SceneGraph<*>.node(node: T, callback: @SceneGraphDslMarker T.() -> Unit = {}): T {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.node(node, callback)
}

/**
 * The base node class that all other node's inherit from.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class Node : Comparable<Node> {

    @ThreadLocal
    companion object {
        private var idGenerator = 0
    }

    open val membersAndPropertiesString get() = "name=$name, id=$id,  enabled=$enabled"

    private var readyNotified = false
    private var readyFirst = true

    val dt: Duration get() = scene?.dt ?: Duration.ZERO

    /**
     * Node name. useful for doing scene-wide searches for an node
     */
    var name: String = this::class.simpleName ?: "Node"

    internal var _scene: SceneGraph<*>? = null

    /**
     * The scene this node belongs to.
     */
    var scene: SceneGraph<*>?
        get() = _scene
        set(value) {
            if (value == _scene) return

            if (_scene != null) {
                propagateExitTree()
            }

            _scene = value
            if (value != null) {
                propagateEnterTree()
                if (parent == null || parent?.readyNotified == true) {
                    propagateReady()
                }
            }
        }

    /**
     * The [Viewport] that this [Node] belongs to. The closest parent [Viewport] will be selected.
     * If none are found, the scene root viewport will be used.
     */
    var viewport: Viewport? = null
        internal set(value) {
            field = value
        }

    /**
     * Unique identifier for this node.
     */
    val id = idGenerator++

    /**
     * Specifies how often this node's update method should be called. 1 means every frame, 2 is every other, etc
     */
    var updateInterval = 1

    /**
     * Enables/disables the node.
     */
    var enabled: Boolean
        get() = _enabled
        set(value) {
            enabled(value)
        }

    /**
     * If destroy was called, this will be true until the next time node's are processed.
     */
    val isDestroyed get() = _isDestroyed

    /**
     * Check if this [Node] is in a [SceneGraph]
     */
    val insideTree get() = scene != null

    private var _tag = 0
    private var _enabled = true
    private var _updateOrder = 0
    private var _isDestroyed = false

    val index: Int get() = pos - 1

    private var pos = -1
    private var depth = -1

    /**
     * The parent [Node], if any.
     */
    var parent: Node?
        get() = _parent
        set(value) {
            parent(value)
        }


    protected var _parent: Node? = null

    /**
     * The current child count for this [Node]. Alias for [NodeList.size].
     */
    val childCount get() = nodes.size

    /**
     * The list of [Node]s in this scene.
     */
    val nodes by lazy { NodeList() }

    /**
     * The children of this [Node]. Alias for [nodes].
     */
    val children get() = nodes

    /**
     * List of 'ready' callbacks called when [ready] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onReady += {
     *         // handle ready logic
     *     }
     * }
     * ```
     */
    val onReady: Signal = signal()

    /**
     * List of 'update' callbacks called when [update] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onUpdate += { dt ->
     *         // handle update logic
     *     }
     * }
     * ```
     */
    val onUpdate: SingleSignal<Duration> = signal1v()

    /**
     * List of 'destroy' callbacks called when [destroy] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onDestroy += {
     *         // handle extra destroy logic
     *     }
     * }
     * ```
     */
    @JsName("onDestroySignal")
    val onDestroy: Signal = signal()

    /**
     * List of 'resize' callbacks called when [resize] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onResize += { width, height ->
     *         // handle extra resize logic
     *     }
     * }
     * ```
     */
    val onResize: DoubleSignal<Int, Int> = signal2v()

    /**
     * List of 'input' callbacks called when [input] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onInput += { event ->
     *         // handle extra input logic
     *     }
     * }
     * ```
     */
    val onInput: SingleSignal<InputEvent<*>> = signal1v()

    /**
     * List of 'unhandledInput' callbacks called when [unhandledInput] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onUnhandledInput += { event ->
     *         // handle extra unhandled input logic
     *     }
     * }
     * ```
     */
    val onUnhandledInput: SingleSignal<InputEvent<*>> = signal1v()


    @JsName("onRemovingFromSceneSignal")
    val onRemovingFromScene: Signal = signal()

    /**
     * List of 'removedFromScene' callbacks called when [onRemovedFromScene] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onUnhandledInput += { event ->
     *         // handle extra unhandled input logic
     *     }
     * }
     * ```
     */
    @JsName("onRemovedFromSceneSignal")
    val onRemovedFromScene: Signal = signal()

    /**
     * List of 'addedToScene' callbacks called when [onAddedToScene] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onUnhandledInput += { event ->
     *         // handle extra unhandled input logic
     *     }
     * }
     * ```
     */
    @JsName("onAddedToSceneSignal")
    val onAddedToScene: Signal = signal()

    val onChildExitedTree: SingleSignal<Node> = signal1v()

    val onChildEnteredTree: SingleSignal<Node> = signal1v()

    private fun propagateExitTree() {
        nodes.forEach {
            it.propagateExitTree()
        }
        onRemovingFromScene()
        onRemovingFromScene.emit()

        parent?.onChildExitedTree?.emit(this)
        readyNotified = false
        depth = -1
    }

    private fun propagateAfterExitTree() {
        nodes.forEach {
            it.propagateAfterExitTree()
        }

        onRemovedFromScene()
        onRemovedFromScene.emit()
    }

    private fun propagateEnterTree() {
        depth = 1
        parent?.let {
            _scene = it.scene
            depth = it.depth + 1
        }

        viewport = parent?.viewport

        onAddedToScene()
        onAddedToScene.emit()

        parent?.onChildEnteredTree?.emit(this)

        nodes.forEach {
            if (!it.insideTree) {
                it.propagateEnterTree()
            }
        }
    }

    private fun propagateReady() {
        readyNotified = true
        nodes.forEach {
            it.onPostEnterScene()
            it.propagateReady()
        }

        if (readyFirst) {
            readyFirst = false
            ready()
            onReady.emit()
        }
    }

    fun propagateUpdate() {
        update(dt)
        onUpdate.emit(dt)
        nodes.updateLists()
        nodes.update()
    }

    fun propagateResize(width: Int, height: Int, center: Boolean) {
        if (!enabled) return
        nodes.forEach {
            it.propagateResize(width, height, center)
        }
        resize(width, height)
        onResize.emit(width, height)
    }

    internal open fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        renderCallback: ((Node, Batch, Camera) -> Unit)?,
    ) {
        nodes.forEach { it.propagateInternalRender(batch, camera, renderCallback) }
    }

    internal open fun propagateHit(hx: Float, hy: Float): Control? {
        nodes.forEachReversed {
            val target = it.propagateHit(hx, hy)
            if (target != null) {
                return target
            }
        }
        if (this is Control) {
            return hit(hx, hy)
        }
        return null
    }

    internal open fun callInput(event: InputEvent<*>) {
        if (!enabled || !insideTree) return
        onInput.emit(event)
        if (event.handled) {
            return
        }
        input(event)
    }

    internal open fun callUnhandledInput(event: InputEvent<*>) {
        if (!enabled || !insideTree) return
        onUnhandledInput.emit(event)
        if (event.handled) {
            return
        }
        unhandledInput(event)
    }

    /**
     * Sets the parent [Node] of this [Node].
     * @param parent this Nodes parent
     */
    open fun parent(parent: Node?): Node {
        if (_parent == parent) {
            return this
        }
        _parent?.removeChild(this)
        parent?.addChild(this)
        return this
    }

    /**
     * Sets the parent of the child to this [Node].
     * @param child the child to add
     */
    open fun addChild(child: Node): Node {
        nodes.add(child)
        child.pos = childCount
        child._parent = this

        child.scene = scene
        onChildAdded(child)

        parent?.onDescendantAdded(child)
        return this
    }

    /**
     * Sets the parent of the children to this [Node].
     * @param children the children to add
     */
    open fun addChildren(vararg children: Node): Node {
        children.forEach {
            addChild(it)
        }
        return this
    }

    fun removeChild(child: Node): Node {
        if (child.parent != this) return this

        child._parent = null
        nodes.remove(child)

        child.scene = null
        child.pos = -1
        nodes.forEachIndexed { index, node ->
            node.pos = index
        }

        onChildRemoved(child)

        parent?.onDescendantAdded(child)
        if (insideTree) {
            child.propagateAfterExitTree()
        }
        return this
    }

    open fun removeChildAt(idx: Int): Node {
        if (idx >= childCount) return this
        nodes[idx]?.also { removeChild(it) }
        return this
    }

    /**
     * Enables/disables the [Node]. When disabled colliders are removed from the Physics system and will not be called.
     * @param value true to enable this node; false otherwise
     */
    fun enabled(value: Boolean): Node {
        if (_enabled != value) {
            _enabled = value
            nodes.forEach {
                it._enabled = value
            }
        }
        return this
    }

    /**
     * Remove the [Node] from the [SceneGraph] and destroys all children.
     */
    fun destroy() {
        _isDestroyed = true
        parent = null
        nodes.forEach {
            it.destroy()
        }
        onDestroy()
        onDestroy.emit()
        onUpdate.clear()
        onReady.clear()
        onDestroy.clear()
        onResize.clear()
        onRemovedFromScene.clear()
        onRemovingFromScene.clear()
        onAddedToScene.clear()
        onChildEnteredTree.clear()
        onChildExitedTree.clear()
    }

    /**
     * Called when [destroy] is invoked and all of its children have been destroyed.
     */
    open fun onDestroy() = Unit


    open fun onDescendantAdded(child: Node) {
        parent?.onDescendantAdded(child)
    }

    open fun onDescendantRemoved(child: Node) {
        parent?.onDescendantRemoved(child)
    }

    /**
     * Called when this [Node] and all of it's children are added to the scene and active
     */
    protected open fun ready() {}

    /**
     * Called when this [Node] and all of its children are added to the scene and have themselves called [onPostEnterScene]
     */
    protected open fun onPostEnterScene() {}

    /**
     * Called when the [scene] is resized.
     */
    protected open fun resize(width: Int, height: Int) {}

    /**
     * Called when this [Node] is added to a [SceneGraph] after all pending [Node] changes are committed.
     * Called from top-to-bottom of tree.
     */
    protected open fun onAddedToScene() {}

    /**
     * Called when this [Node] is removed from a [SceneGraph].
     * Called bottom-to-top of tree.
     */
    protected open fun onRemovedFromScene() {}

    /**
     * Called when this [Node] is removed from a [SceneGraph].
     * Called bottom-to-top of tree.
     */
    protected open fun onRemovingFromScene() {}

    /**
     * Called when a [Node] is added as a child to this node.
     * @param child - the child node being added
     */
    protected open fun onChildAdded(child: Node) {}

    /**
     * Called when a [Node] is removed as a child from this node.
     * @param child - the child node being removed
     */
    protected open fun onChildRemoved(child: Node) {}

    /**
     * Called each frame as long as the [Node] is [enabled].
     */
    protected open fun update(dt: Duration) {}

    /**
     * Called when there is an [InputEvent]. The input event propagates up through the node
     * tree until a node consumes it.
     */
    protected open fun input(event: InputEvent<*>) = Unit

    /**
     * Called when an [InputEvent] isn't consumed by [input] or the UI. The input event propagates up through the node
     * tree until a node consumes it.
     */
    protected open fun unhandledInput(event: InputEvent<*>) = Unit

    /**
     * @return a tree string for all the child nodes under this [Node].
     */
    fun treeString(): String {
        val builder = StringBuilder()
        internalToTreeString(builder, "", "")
        return builder.toString()
    }

    fun treeStringWithProperties(): String {
        val builder = StringBuilder()
        internalToTreeString(builder, "", "", true)
        return builder.toString()
    }

    private fun internalToTreeString(
        builder: StringBuilder,
        prefix: String,
        childrenPrefix: String,
        showProps: Boolean = false,
    ) {
        builder.run {
            append(prefix)
            append(name)
            if (name != this@Node::class.simpleName) {
                append(" (${this@Node::class.simpleName})")
            }
            if (showProps) {
                append("[$membersAndPropertiesString]")
            }
            appendLine()
            nodes.forEachIndexed { index, node ->
                if (index < nodes.size - 1) {
                    node.internalToTreeString(builder, "$childrenPrefix├── ", "$childrenPrefix│   ", showProps)
                } else {
                    node.internalToTreeString(builder, "$childrenPrefix└── ", "$childrenPrefix    ", showProps)
                }
            }
        }
    }

    /**
     * Compares [Node] by depth and position at each depth.
     * @return a negative value if depth is less than the other node depth or if depths are the same and position is less than
     * the other nodes position. If the depth and position are the same (which should not happen) it will compare by ids.
     * Otherwise it will return a positive value.
     */
    override fun compareTo(other: Node): Int {
        if (depth < other.depth) {
            return -1
        }
        if (depth == other.depth && pos < other.pos) {
            return -1
        }

        if (depth == other.depth && pos == other.pos) {
            return id.compareTo(other.id)
        }
        return 1
    }

    override fun toString() = "${this::class.simpleName}($membersAndPropertiesString)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Node

        if (name != other.name) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id
        return result
    }

    protected fun MutableList<() -> Unit>.emit() {
        fastForEach { it.invoke() }
    }

    protected fun <T> MutableList<(T) -> Unit>.emit(value: T) {
        fastForEach { it.invoke(value) }
    }
}

fun <T : Node> T.addTo(parent: Node): T {
    parent(parent)
    return this
}

inline fun <reified T : Node> Node.findFirstNodeOfType() = nodes.findFirstNodeOfType(T::class)