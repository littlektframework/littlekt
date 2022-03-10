package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.internal.NodeList
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.util.*
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.js.JsName
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Duration

/**
 * Adds a [Node] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
inline fun Node.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}) =
    Node().also(callback).addTo(this)

/**
 * Adds the specified [Node] to the current [Node] as a child and then triggers the [callback]. This can be used
 * for classes that extend node that perhaps they don't have a DSL method to initialize it.
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
inline fun <T : Node> Node.node(node: T, callback: @SceneGraphDslMarker T.() -> Unit = {}) =
    node.also(callback).addTo(this)

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
inline fun SceneGraph<*>.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}) = root.node(callback)

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]. This can be used
 * for classes that extend node that perhaps they don't have a DSL method to initialize it.
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any values
 * @return the newly created [Node]
 */
inline fun <T : Node> SceneGraph<*>.node(node: T, callback: @SceneGraphDslMarker T.() -> Unit = {}) =
    root.node(node, callback)

/**
 * The base node class that all other node's inherit from.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class Node : Comparable<Node> {

    @ThreadLocal
    companion object {
        private var idGenerator = 0
        internal const val CLEAN = 0
    }

    open val membersAndPropertiesString get() = "name=$name, id=$id,  enabled=$enabled"

    /**
     * Node name. useful for doing scene-wide searches for an node
     */
    var name: String = this::class.simpleName ?: "Node"

    /**
     * The scene this node belongs to.
     */
    var scene: SceneGraph<*>? = null
        set(value) {
            if (value == field) return
            field = value
            if (value == null) {
                nodes.forEach {
                    it.scene = null
                }
                _onRemovedFromScene()
            } else {
                _onAddedToScene()
                nodes.forEach {
                    it.scene = value
                }
            }
        }

    /**
     * The [Viewport] that this [Node] belongs to. The closest parent [Viewport] will be selected.
     * If none are found, the scene root viewport will be used.
     */
    var viewport: Viewport? = null
        internal set

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
     * Shows/hides the node if it is renderable.
     */
    var visible: Boolean
        get() = _visible
        set(value) {
            visible(value)
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
    private var _visible = true
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
    protected var hierarchyDirty: Int = CLEAN

    /**
     * The current child count for this [Node]
     */
    val childCount get() = nodes.size

    /**
     * The list of [Node]s in this scene.
     */
    @PublishedApi
    internal val nodes by lazy { NodeList() }

    /**
     * The children of this [Node].
     *
     * **WARNING**: This possibly allocates a new list on read, depending if there are new nodes to add.
     */
    val children get() = nodes.toList()

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
     * List of 'render' callbacks called when [render] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onRender += { batch, camera ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onRender: DoubleSignal<Batch, Camera> = signal2v()

    /**
     * List of 'debugRender' callbacks called when [debugRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onDebugRender += { batch ->
     *         // handle debug render logic
     *     }
     * }
     * ```
     */
    val onDebugRender: SingleSignal<Batch> = signal1v()

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
     * Sets the parent [Node] of this [Node].
     * @param parent this Nodes parent
     */
    open fun parent(parent: Node?): Node {
        if (_parent == parent) {
            return this
        }

        _parent?.nodes?.remove(this)
        _parent?._onChildRemoved(this)
        parent?.nodes?.add(this)
        pos = parent?.childCount ?: -1

        _parent = parent
        _parent?._onChildAdded(this)
        return this
    }

    /**
     * Sets the parent of the child to this [Node].
     * @param child the child to add
     */
    open fun addChild(child: Node): Node {
        child.parent(this)
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

    open fun removeChild(child: Node): Node {
        if (child.parent != this) return this
        child.parent(null)
        return this
    }

    open fun removeChildAt(idx: Int): Node {
        if (idx >= childCount) return this
        nodes[idx]?.also { removeChild(it) }
        return this
    }

    /**
     * Updates the current [Node] hierarchy if it is dirty.
     */
    open fun updateHierarchy() {
        if (hierarchyDirty != CLEAN) {
            parent?.updateHierarchy()
            hierarchyDirty = CLEAN
        }
    }

    /**
     * Internal rendering that needs to be done on the node that shouldn't be overridden. Calls [render] method.
     */
    internal fun _render(batch: Batch, camera: Camera) {
        if (!enabled || !visible) return
        render(batch, camera)
        onRender.emit(batch, camera)
        nodes.forEach {
            it._render(batch, camera)
        }
    }

    /**
     * Internal debug render method. Calls the [debugRender] method.
     */
    private fun _debugRender(batch: Batch) {
        debugRender(batch)
        onDebugRender.emit(batch)
        nodes.forEach {
            it._debugRender(batch)
        }
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
     * Shows/hides the [Node]. When disabled [render] is no longer called.
     * @param value true to enable this node; false otherwise
     */
    fun visible(value: Boolean): Node {
        if (_visible != value) {
            _visible = value
            nodes.forEach {
                it._visible = value
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
        onRender.clear()
        onDebugRender.clear()
        onDestroy.clear()
    }

    /**
     * Called when [destroy] is invoked and all of its children have been destroyed.
     */
    open fun onDestroy() = Unit

    /**
     * The internal lifecycle method for when a child is added to this [Node].
     * Calls the open [onChildAdded] lifecycle method.
     * @param child - the child node being added to this node.
     */
    private fun _onChildAdded(child: Node) {
        child.scene = scene
        parent?._onDescendantAdded(child)
        onChildAdded(child)
    }

    private fun _onDescendantAdded(child: Node) {
        parent?._onDescendantAdded(child)
    }

    private fun _onChildRemoved(child: Node) {
        child.scene = null
        nodes.forEachIndexed { index, node ->
            node.pos = index
        }
        onChildRemoved(child)
    }


    /**
     * Called when this [Node] is added to a [SceneGraph] after all pending [Node] changes are committed.
     */
    private fun _onAddedToScene() {
        depth = parent?.depth?.plus(1) ?: 1

        viewport = parent?.viewport

        onAddedToScene()
    }

    internal fun _onPostEnterScene() {
        nodes.forEach {
            it._onPostEnterScene()
        }
        onPostEnterScene()
        ready()
        onReady.emit()
    }

    /**
     * Called when this [Node] is removed from a [SceneGraph]. Called bottom-to-top of tree.
     */
    private fun _onRemovedFromScene() {
        onRemovedFromScene()
        viewport = null
        depth = -1
    }

    /**
     * Internal update lifecycle method for updating the node and its children.
     * Calls the [update] lifecycle method.
     */
    internal fun _update(dt: Duration) {
        update(dt)
        onUpdate.emit(dt)
        nodes.updateLists()
        nodes.update(dt)
    }

    /**
     * Dirties the hierarchy for the current [Node] and all of it's [children].
     */
    open fun dirty(dirtyFlag: Int) {
        if ((hierarchyDirty and dirtyFlag) == 0) {
            hierarchyDirty = hierarchyDirty or dirtyFlag

            nodes.forEach {
                it.dirty(dirtyFlag)
            }
            _onHierarchyChanged(dirtyFlag)
        }
    }

    /**
     * Internal. Called when the hierarchy of this [Node] is changed.
     * Example changes that can trigger this include: `position`, `rotation`, and `scale`
     */
    private fun _onHierarchyChanged(flag: Int) {
        onHierarchyChanged(flag)
    }

    /**
     * Called when this [Node] and all of it's children are added to the scene and active
     */
    open fun ready() {}

    /**
     * Called when this [Node] and all of it's children are added to the scene and have themselves called [onPostEnterScene]
     */
    open fun onPostEnterScene() {}

    /**
     * The main render method. The [Camera] can be used for culling and the [Batch] instance to draw with.
     * @param batch the batcher
     * @param camera the Camera2D node
     */
    open fun render(batch: Batch, camera: Camera) {}

    /**
     * Draw any debug related items here.
     * @param batch the sprite batch to draw with
     */
    open fun debugRender(batch: Batch) {}

    /**
     * Called when this [Node] is added to a [SceneGraph] after all pending [Node] changes are committed.
     * Called from top-to-bottom of tree.
     */
    open fun onAddedToScene() {}

    /**
     * Called when this [Node] is removed from a [SceneGraph].
     * Called bottom-to-top of tree.
     */
    open fun onRemovedFromScene() {}

    /**
     * Called when a [Node] is added as a child to this node.
     * @param child - the child node being added
     */
    open fun onChildAdded(child: Node) {}

    /**
     * Called when a [Node] is removed as a child from this node.
     * @param child - the child node being removed
     */
    open fun onChildRemoved(child: Node) {}

    /**
     * Called each frame as long as the [Node] is [enabled].
     */
    open fun update(dt: Duration) {}

    /**
     * Called when the hierarchy of this [Node] is changed.
     * Example changes that can trigger this include: `position`, `rotation`, and `scale`
     */
    open fun onHierarchyChanged(flag: Int) {}

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
        showProps: Boolean = false
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