package com.littlekt.graph.node

import com.littlekt.Context
import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.internal.NodeList
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.ui.Control
import com.littlekt.graph.util.*
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.util.datastructure.fastForEach
import io.ygdrasil.wgpu.RenderPassEncoder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.JsName
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Duration

/**
 * Adds a [Node] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Node] context in order to initialize any
 *   values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}): Node {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Node().also(callback).addTo(this)
}

/**
 * Adds the specified [Node] to the current [Node] as a child and then triggers the [callback]. This
 * can be used for classes that extend node that perhaps they don't have a DSL method to initialize
 * it.
 *
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any
 *   values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Node> Node.node(node: T, callback: @SceneGraphDslMarker T.() -> Unit = {}): T {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return node.also(callback).addTo(this)
}

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]
 *
 * @param callback the callback that is invoked with a [Node] context in order to initialize any
 *   values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.node(callback: @SceneGraphDslMarker Node.() -> Unit = {}): Node {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.node(callback)
}

/**
 * Adds a [Node] to the current [SceneGraph.root] as a child and then triggers the [Node]. This can
 * be used for classes that extend node that perhaps they don't have a DSL method to initialize it.
 *
 * @param node the node to add
 * @param callback the callback that is invoked with a [Node] context in order to initialize any
 *   values
 * @return the newly created [Node]
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Node> SceneGraph<*>.node(
    node: T,
    callback: @SceneGraphDslMarker T.() -> Unit = {}
): T {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.node(node, callback)
}

/**
 * The base node class that all other node's inherit from.
 *
 * @author Colton Daily
 * @date 1/1/2022
 */
open class Node : Comparable<Node> {

    @ThreadLocal
    companion object {
        private var idGenerator = 0
    }

    open val membersAndPropertiesString
        get() = "name=$name, id=$id,  enabled=$enabled"

    private var readyNotified = false
    private var readyFirst = true

    /** The [Context] this node belongs to. Throws an error if context does not exist. */
    val context: Context
        get() =
            scene?.context
                ?: error(
                    "This Node could not get a context. Check to see if it is added to a Scene and if that Scene is the active scene!"
                )

    /** The [Context] this node belongs to, or null, if it doesn't exist. */
    val contextOrNull: Context?
        get() = scene?.context

    /** The current [RenderPassEncoder] of the [canvas]. */
    val canvasRenderPass: RenderPassEncoder
        get() =
            canvas?.renderPass
                ?: error(
                    "This Node could not get a render pass. Check to see if it is added to a Scene and if that Scene is the active scene!"
                )

    /** The current [RenderPassEncoder] of the [canvas], or null, if it doesn't exist. */
    val canvasRenderPassOrNull: RenderPassEncoder?
        get() = canvas?.renderPassOrNull

    /** The current scene delta time. */
    val dt: Duration
        get() = scene?.dt ?: Duration.ZERO

    /** Node name. useful for doing scene-wide searches for an node */
    var name: String = this::class.simpleName ?: "Node"

    internal var _scene: SceneGraph<*>? = null

    /** The scene this node belongs to. */
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
     * The [CanvasLayer] that this [Node] belongs to. The closest parent [CanvasLayer] will be
     * selected. If none are found, the scene root canvas will be used. If this [Node] is the scene
     * root canvas, then this will be `null`.
     */
    var canvas: CanvasLayer? = null
        internal set

    /** Unique identifier for this node. */
    val id = idGenerator++

    /**
     * Specifies how often this node's update method should be called. 1 means every frame, 2 is
     * every other, etc
     */
    var updateInterval = 1

    /**
     * Enables/disables the node. This node will receive no updates and is hidden, if applicable.
     */
    var enabled: Boolean
        get() = _enabled
        set(value) {
            enabled(value)
        }

    /** If destroy was called, this will be true until the next time node's are processed. */
    val isDestroyed
        get() = _isDestroyed

    /** Check if this [Node] is in a [SceneGraph] */
    val insideTree
        get() = scene != null

    /** Attempts to grab the [SceneGraph.fixedProgressionRatio]. Defaults to `1` if not. */
    val fixedProgressionRatio: Float
        get() = scene?.fixedProgressionRatio ?: 1f

    /** The index of this [Node] as a child in it's [parent]. */
    val index: Int
        get() = pos - 1

    /**
     * Pixels per unit. Mainly used when rendering a [Node]. This is based off of the
     * [SceneGraph.ppu].
     */
    val ppu: Float
        get() = scene?.ppu ?: 1f

    /** The inverse of [ppu]. This is based off the [SceneGraph.ppuInv]. */
    val ppuInv: Float
        get() = scene?.ppuInv ?: 1f

    private var _tag = 0
    private var _enabled = true
    private var _updateOrder = 0
    private var _isDestroyed = false

    private var pos = -1
        set(value) {
            check(value != 0 && value >= -1) { "Node position can never be 0 or less than -1!" }
            field = value
        }

    private var depth = -1
        set(value) {
            check(value != 0 && value >= -1) { "Node depth can never be 0 or less than -1!" }
            field = value
        }

    /** The parent [Node], if any. */
    var parent: Node?
        get() = _parent
        set(value) {
            parent(value)
        }

    protected var _parent: Node? = null

    /** The current child count for this [Node]. Alias for [NodeList.size]. */
    val childCount: Int
        get() = nodes.size

    /** The list of [Node]s in this scene. */
    val nodes by lazy { NodeList() }

    /** The children of this [Node]. Alias for [nodes]. */
    val children: NodeList
        get() = nodes

    /**
     * List of 'ready' callbacks called when [ready] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
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
     * List of 'pre-update' callbacks called when [preUpdate] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onPreUpdate += { dt ->
     *         // handle pre-update logic
     *     }
     * }
     * ```
     */
    val onPreUpdate: SingleSignal<Duration> = signal1v()

    /**
     * List of 'update' callbacks called when [update] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
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
     * List of 'post-update' callbacks called when [postUpdate] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onPostUpdate += { dt ->
     *         // handle post-update logic
     *     }
     * }
     * ```
     */
    val onPostUpdate: SingleSignal<Duration> = signal1v()

    /**
     * List of 'fixed-update' callbacks called when [fixedUpdate] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onFixedUpdate += {
     *         // handle fixed-update logic
     *     }
     * }
     * ```
     */
    val onFixedUpdate: Signal = signal()

    /**
     * List of 'destroy' callbacks called when [destroy] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onDestroy += {
     *         // handle extra destroy logic
     *     }
     * }
     * ```
     */
    @JsName("onDestroySignal") val onDestroy: Signal = signal()

    /**
     * List of 'resize' callbacks called when [resize] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
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
     * List of 'input' callbacks called when [input] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
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
     * List of 'unhandledInput' callbacks called when [unhandledInput] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
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

    @JsName("onRemovingFromSceneSignal") val onRemovingFromScene: Signal = signal()

    /**
     * List of 'removedFromScene' callbacks called when [onRemovedFromScene] is called. Add any
     * additional callbacks directly to this list. The main use is to add callbacks directly to
     * nodes inline when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onUnhandledInput += { event ->
     *         // handle extra unhandled input logic
     *     }
     * }
     * ```
     */
    @JsName("onRemovedFromSceneSignal") val onRemovedFromScene: Signal = signal()

    /**
     * List of 'addedToScene' callbacks called when [onAddedToScene] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onUnhandledInput += { event ->
     *         // handle extra unhandled input logic
     *     }
     * }
     * ```
     */
    @JsName("onAddedToSceneSignal") val onAddedToScene: Signal = signal()

    val onChildExitedTree: SingleSignal<Node> = signal1v()

    val onChildEnteredTree: SingleSignal<Node> = signal1v()

    /**
     * List of 'onEnabled' callbacks called when [onEnabled] is called. Add any additional callbacks
     * directly to this list. The main use is to add callbacks directly to nodes inline when
     * building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onEnable += {
     *         // enable logic
     *     }
     * }
     * ```
     */
    @JsName("onEnabledSignal") val onEnabled: Signal = signal()

    /**
     * List of 'onDisabled' callbacks called when [onDisabled] is called. Add any additional
     * callbacks directly to this list. The main use is to add callbacks directly to nodes inline
     * when building a [SceneGraph] vs having to extend a class directly.
     *
     * ```
     * node {
     *     onDisabled += {
     *         // disable logic
     *     }
     * }
     * ```
     */
    @JsName("onDisabledSignal") val onDisabled: Signal = signal()

    private fun propagateExitTree() {
        nodes.forEach { it.propagateExitTree() }
        onRemovingFromScene()
        onRemovingFromScene.emit()

        parent?.onChildExitedTree?.emit(this)
        readyNotified = false
        depth = -1
    }

    private fun propagateAfterExitTree() {
        nodes.forEach { it.propagateAfterExitTree() }

        onRemovedFromScene()
        onRemovedFromScene.emit()
    }

    private fun propagateEnterTree() {
        depth = 1
        parent?.let {
            _scene = it.scene
            depth = it.depth + 1
        }

        canvas =
            parent?.let {
                if (it is CanvasLayer) {
                    it
                } else {
                    it.canvas
                }
            } ?: if (scene?.sceneCanvas != this) scene?.sceneCanvas else null

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
        // if node was added to a parent after the parent propagated through ready, then we want to
        // make sure
        // this child receives the onPostEnterScene
        if (_parent?.readyNotified == true) {
            onPostEnterScene()
        }
        nodes.forEach {
            it.onPostEnterScene()
            it.propagateReady()
        }
        readyNotified = true

        if (readyFirst) {
            readyFirst = false
            ready()
            onReady.emit()
        }
    }

    fun propagatePreUpdate() {
        preUpdate(dt)
        onPreUpdate.emit(dt)
        nodes.preUpdate()
    }

    fun propagateUpdate() {
        update(dt)
        onUpdate.emit(dt)
        nodes.updateLists()
        nodes.update()
    }

    fun propagatePostUpdate() {
        postUpdate(dt)
        onPostUpdate.emit(dt)
        nodes.postUpdate()
    }

    fun propagateFixedUpdate() {
        fixedUpdate()
        onFixedUpdate.emit()
        nodes.fixedUpdate()
    }

    fun propagateResize(width: Int, height: Int, center: Boolean) {
        if (!enabled || isDestroyed) return
        nodes.forEach { it.propagateResize(width, height, center) }
        resize(width, height)
        onResize.emit(width, height)
    }

    open fun propagateInternalDebugRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        nodes.forEach {
            it.propagateInternalDebugRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        }
    }

    open fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || isDestroyed) return
        nodes.forEach {
            it.propagateInternalRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        }
    }

    open fun propagateHit(hx: Float, hy: Float): Control? {
        if (!enabled || isDestroyed) return null

        if (this is Control) {
            return hit(hx, hy)
        }
        nodes.forEachReversed {
            val target = it.propagateHit(hx, hy)
            if (target != null) {
                return target
            }
        }
        return null
    }

    open fun propagateInput(event: InputEvent<*>): Boolean {
        nodes.forEachReversed {
            it.propagateInput(event)
            if (event.handled) {
                return true
            }
        }
        callInput(event)
        return event.handled
    }

    open fun propagateUnhandledInput(event: InputEvent<*>): Boolean {
        nodes.forEachReversed {
            it.propagateUnhandledInput(event)
            if (event.handled) {
                return true
            }
        }
        callUnhandledInput(event)
        return event.handled
    }

    open fun callInput(event: InputEvent<*>) {
        if (!enabled || !insideTree || isDestroyed) return
        onInput.emit(event)
        if (event.handled) {
            return
        }
        input(event)
    }

    open fun callUnhandledInput(event: InputEvent<*>) {
        if (!enabled || !insideTree || isDestroyed) return
        onUnhandledInput.emit(event)
        if (event.handled) {
            return
        }
        unhandledInput(event)
    }

    /**
     * Sets the parent [Node] of this [Node].
     *
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
     *
     * @param child the child to add
     */
    fun addChild(child: Node): Node {
        nodes.add(child)
        child.pos = childCount
        child._parent = this

        child.scene = scene
        onChildAdded(child)

        parent?.onDescendantAdded(child)
        return this
    }

    /**
     * Sets the parent of the child to this [Node] sets it at the specified index.
     *
     * @param child the child to add
     * @param index the index of the child to be added at
     */
    fun addChildAt(child: Node, index: Int): Node {
        nodes.addAt(child, index)
        nodes.forEachIndexed { idx, node -> node.pos = idx + 1 }
        child._parent = this

        child.scene = scene
        onChildAdded(child)

        parent?.onDescendantAdded(child)
        return this
    }

    /**
     * Moves a child node to the specified index.
     *
     * @param child the child node to move
     * @param index the index to move the child to
     */
    fun moveChild(child: Node, index: Int): Node {
        if (child.parent != this) return this

        val moved = nodes.moveTo(child, index)
        if (moved) {
            nodes.forEachIndexed { idx, node -> node.pos = idx + 1 }
        }
        return this
    }

    /**
     * Sets the parent of the children to this [Node].
     *
     * @param children the children to add
     */
    fun addChildren(vararg children: Node): Node {
        children.forEach { addChild(it) }
        return this
    }

    /**
     * Removes the node if this node is its parent.
     *
     * @param child the child node to remove
     */
    fun removeChild(child: Node): Node {
        if (child.parent != this) return this

        child._parent = null
        nodes.remove(child)

        child.scene = null
        child.pos = -1
        nodes.forEachIndexed { index, node -> node.pos = index + 1 }

        onChildRemoved(child)

        parent?.onDescendantRemoved(child)
        if (insideTree) {
            child.propagateAfterExitTree()
        }
        return this
    }

    /**
     * Removes the child at the specified index.
     *
     * @param idx the index of the child to remove
     */
    fun removeChildAt(idx: Int): Node {
        if (idx >= childCount) return this
        nodes[idx].also { removeChild(it) }
        return this
    }

    /** Removes & destroys all children nodes. */
    fun destroyAllChildren(): Node {
        nodes.removeAndDestroyAllNodes()
        return this
    }

    /**
     * Swaps the two children positions within the parents list
     *
     * @param child the first child to swap
     * @param child2 the second child to swap
     */
    fun swapChildren(child: Node, child2: Node): Node {
        if (child.parent != this || child2.parent != this) return this
        nodes.swap(child, child2)
        nodes.forEachIndexed { index, node -> node.pos = index + 1 }
        return this
    }

    /**
     * Swaps children positions at the specified indices.
     *
     * @param idx the index of the first child
     * @param idx2 the index of the second child
     */
    fun swapChildrenAt(idx: Int, idx2: Int): Node {
        if (idx >= childCount || idx2 >= childCount) return this
        return swapChildren(nodes[idx], nodes[idx2])
    }

    /**
     * Sends the child node to the bottom of this list which will be rendered on top.
     *
     * @param child to send to the front
     */
    fun sendChildToBottom(child: Node): Node {
        if (child.parent != this) return this
        nodes.sendToBottom(child)
        nodes.forEachIndexed { index, node -> node.pos = index + 1 }
        return this
    }

    /**
     * Sends the child node at the specified index to the bottom of this list which will be rendered
     * on top.
     *
     * @param idx the index of the child to send to the front
     */
    fun sendChildAtToBottom(idx: Int): Node {
        if (idx >= childCount) return this
        return sendChildToBottom(nodes[idx])
    }

    /**
     * Sends the child node to the top of this list which will be rendered behind.
     *
     * @param child to send to the back
     */
    fun sendChildToTop(child: Node): Node {
        if (child.parent != this) return this
        nodes.sendToTop(child)
        nodes.forEachIndexed { index, node -> node.pos = index + 1 }
        return this
    }

    /**
     * Sends the child node at the specified index to the top of this list which will be rendered
     * behind.
     *
     * @param idx the index of the child to send to the back
     */
    fun sendChildAtToTop(idx: Int): Node {
        if (idx >= childCount) return this
        return sendChildToTop(nodes[idx])
    }

    /**
     * Enables/disables the [Node]. When disabled colliders are removed from the Physics system and
     * will not be called.
     *
     * @param value true to enable this node; false otherwise
     */
    fun enabled(value: Boolean): Node {
        if (_enabled != value) {
            _enabled = value
            nodes.forEach { it.enabled(value) }
            if (_enabled) {
                onEnabled()
                onEnabled.emit()
            } else {
                onDisabled()
                onDisabled.emit()
            }
        }
        return this
    }

    /**
     * Determines if this [Node] has zero children nodes.
     *
     * @return true if contains no children nodes
     */
    fun isEmpty(): Boolean {
        return nodes.size == 0
    }

    /**
     * Determines if this [Node] contains any children.
     *
     * @return true if contains 1 or more children nodes
     */
    fun isNotEmpty() = !isEmpty()

    /** Remove the [Node] from the [SceneGraph] and destroys all children. */
    fun destroy() {
        _enabled = false
        _isDestroyed = true
        while (nodes.isNotEmpty()) {
            nodes[0].destroy()
        }
        onDestroy()
        onDestroy.emit()
        parent = null
        onUpdate.clear()
        onReady.clear()
        onDestroy.clear()
        onResize.clear()
        onRemovedFromScene.clear()
        onRemovingFromScene.clear()
        onAddedToScene.clear()
        onChildEnteredTree.clear()
        onChildExitedTree.clear()
        onEnabled.clear()
        onDisabled.clear()
    }

    /** Called when [destroy] is invoked and all of its children have been destroyed. */
    open fun onDestroy() = Unit

    open fun onDescendantAdded(child: Node) {
        parent?.onDescendantAdded(child)
    }

    open fun onDescendantRemoved(child: Node) {
        parent?.onDescendantRemoved(child)
    }

    /** Called when this [Node] and all of its children are added to the scene and active */
    protected open fun ready() {}

    /**
     * Called when this [Node] and all of its children are added to the scene and have themselves
     * called [onPostEnterScene]
     */
    protected open fun onPostEnterScene() {}

    /** Called when the [scene] is resized. */
    protected open fun resize(width: Int, height: Int) {}

    /**
     * Called when this [Node] is added to a [SceneGraph] after all pending [Node] changes are
     * committed. Called from top-to-bottom of tree.
     */
    protected open fun onAddedToScene() {}

    /** Called when this [Node] is removed from a [SceneGraph]. Called bottom-to-top of tree. */
    protected open fun onRemovedFromScene() {}

    /** Called when this [Node] is removed from a [SceneGraph]. Called bottom-to-top of tree. */
    protected open fun onRemovingFromScene() {}

    /**
     * Called when a [Node] is added as a child to this node.
     *
     * @param child - the child node being added
     */
    protected open fun onChildAdded(child: Node) {}

    /**
     * Called when a [Node] is removed as a child from this node.
     *
     * @param child - the child node being removed
     */
    protected open fun onChildRemoved(child: Node) {}

    /** Called each frame as long as the [Node] is [enabled]. */
    protected open fun preUpdate(dt: Duration) {}

    /** Called each frame as long as the [Node] is [enabled]. */
    protected open fun update(dt: Duration) {}

    /** Called each frame as long as the [Node] is [enabled]. */
    protected open fun postUpdate(dt: Duration) {}

    /**
     * Called at a fixed interval.
     *
     * @see SceneGraph.fixedTimesPerSecond
     * @see SceneGraph.fixedProgressionRatio
     */
    protected open fun fixedUpdate() {}

    /**
     * Called when there is an [InputEvent]. The input event propagates up through the node tree
     * until a node consumes it.
     */
    protected open fun input(event: InputEvent<*>) = Unit

    /**
     * Called when an [InputEvent] isn't consumed by [input] or the UI. The input event propagates
     * up through the node tree until a node consumes it.
     */
    protected open fun unhandledInput(event: InputEvent<*>) = Unit

    /** Called when [enabled] is set to `true`. */
    protected open fun onEnabled() = Unit

    /** Called when [enabled] is set to `false`. */
    protected open fun onDisabled() = Unit

    /** @see addChild */
    operator fun plusAssign(child: Node) {
        addChild(child)
    }

    /** @see removeChild */
    operator fun minusAssign(child: Node) {
        removeChild(child)
    }

    /** @return a tree string for all the child nodes under this [Node]. */
    fun treeString(): String {
        val builder = StringBuilder()
        internalToTreeString(builder, "", "")
        return builder.toString()
    }

    /**
     * @return a tree string for all child nodes under this [Node], that also includes
     *   [membersAndPropertiesString].
     */
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
                    node.internalToTreeString(
                        builder,
                        "$childrenPrefix├── ",
                        "$childrenPrefix│   ",
                        showProps
                    )
                } else {
                    node.internalToTreeString(
                        builder,
                        "$childrenPrefix└── ",
                        "$childrenPrefix    ",
                        showProps
                    )
                }
            }
        }
    }

    /**
     * Compares [Node] by depth and position at each depth.
     *
     * @return a negative value if depth is less than the other node depth or if depths are the same
     *   and position is less than the other nodes position. If the depth and position are the same
     *   (which should not happen) it will compare by ids. Otherwise it will return a positive
     *   value.
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

/** Add this node to the given [Node], making [parent] its new parent. */
fun <T : Node> T.addTo(parent: Node): T {
    parent(parent)
    return this
}

/** Add this node to the given [SceneGraph], making the [scene] root node its new parent. */
fun <T : Node> T.addTo(scene: SceneGraph<*>): T {
    parent(scene.root)
    return this
}

/** Search this nodes children until it finds a node of the given type. */
inline fun <reified T : Node> Node.findFirstNodeOfType() = nodes.findFirstNodeOfType(T::class)

/**
 * Create a new [RenderPassEncoder] for the current node [CanvasLayer].
 *
 * @param label an optional label for the render pass
 */
fun Node.pushRenderPassToCanvas(label: String? = null) =
    canvas?.pushRenderPass(label) ?: error("Node is not part of a scene!")

/**
 * Removes the last [RenderPassEncoder] that was last added with [pushRenderPassToCanvas] and ends
 * and releases it via [RenderPassEncoder.end] and [RenderPassEncoder.release]. If any render passes
 * are left in the list, [Node.canvasRenderPass] will be set to it.
 */
fun Node.popAndEndCanvasRenderPass() = canvas?.popAndEndRenderPass()
