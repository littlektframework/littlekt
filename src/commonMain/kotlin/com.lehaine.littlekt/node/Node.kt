package com.lehaine.littlekt.node

import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.input.Input
import kotlin.native.concurrent.ThreadLocal

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class NodeDslMarker

inline fun Node.node(callback: @NodeDslMarker Node.() -> Unit = {}) =
    Node().addTo(this).also(callback)


open class Node : Comparable<Node> {

    @ThreadLocal
    companion object {
        private var idGenerator = 0 // TODO - create real id generator
        internal const val CLEAN = 0
    }

    /**
     * Entity name. useful for doing scene-wide searches for an entity
     */
    var name: String = this::class.simpleName ?: "Node"

    /**
     * The scene this entity belongs to.
     */
    var scene: Scene? = null

    /**
     * Unique identifier for this Entity.
     */
    val id = idGenerator++

    /**
     * Use this however you want to. It can later be used to query the scene for all Entities with a specific tag
     */
    var tag: Int
        get() = _tag
        set(value) {
            tag(value)
        }

    /**
     * Specifies how often this entity's update method should be called. 1 means every frame, 2 is every other, etc
     */
    var updateInterval = 1

    /**
     * Enables/disables the Entity. When disabled colliders are removed from the Physics system and components methods will not be called.
     */
    var enabled: Boolean
        get() = _enabled
        set(value) {
            enabled(value)
        }

    /**
     * If destroy was called, this will be true until the next time Entity's are processed.
     */
    val isDestroyed get() = _isDestroyed

    private var _tag = 0
    private var _enabled = true
    private var _updateOrder = 0
    internal var _isDestroyed = false

    var parent: Node?
        get() = _parent
        set(value) {
            parent(value)
        }

    val childCount get() = _children.size

    protected var _parent: Node? = null
    protected var hierarchyDirty: Int = CLEAN

    internal val _children = mutableListOf<Node>()
    val children get() = _children.toList()

    /**
     * Sets the parent [Node] of this [Node].
     * @param parent this Nodes parent
     */
    open fun parent(parent: Node?): Node {
        if (_parent == parent) {
            return this
        }

        _parent?._children?.remove(this)
        parent?._children?.add(this)

        _parent = parent
        scene = _parent?.scene
        scene?.addNode(this)
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

    /**
     * Updates the current [Node] hierarchy if it is dirty.
     */
    open fun updateNode() {
        if (hierarchyDirty != CLEAN) {
            parent?.updateNode()
            hierarchyDirty = CLEAN
        }
    }

    /**
     * Can be used to query the [Scene] for all [Nodes] with a specific tag.
     */
    fun tag(value: Int): Node {
        if (_tag != value) {
            scene?.nodes?.removeFromTagList(this)
            _tag = tag
            scene?.nodes?.addToTagList(this)
        }
        return this
    }

    /**
     * Enables/disables the [Node]. When disabled colliders are removed from the Physics system and will not be called.
     */
    fun enabled(value: Boolean): Node {
        if (_enabled != value) {
            _enabled = value
        }
        return this
    }

    /**
     * Remove the [Node] from the [Scene] and destroys all children.
     */
    fun destroy() {
        _isDestroyed = true
        scene?.nodes?.remove(this)
        parent = null

        children.forEach {
            it.destroy()
        }
    }

    /**
     * Detaches the [Node] from the [Scene].
     * The following life cycle is called on the [Node]: [onRemovedFromScene]
     */
    fun detachFromScene() {
        scene?.nodes?.remove(this)

        children.forEach {
            it.detachFromScene()
        }
    }

    /**
     * Attaches a [Node] from the [Scene].
     * The following lifecycle method is called on the [Node]: [onRemovedFromScene]
     */
    fun attachToScene(newScene: Scene) {
        scene = newScene
        newScene.nodes.add(this)

        children.forEach {
            it.attachToScene(newScene)
        }
    }


    /**
     * Called when this [Node] is added to a [Scene] after all pending [Node] changes are committed.
     */
    open fun onAddedToScene() {}

    /**
     * Called when this [Node] is removed from a [Scene].
     */
    open fun onRemovedFromScene() {}

    /**
     * Called each frame as long as the [Node] is [enabled].
     */
    open fun update(input: Input) {
    }

    /**
     * Called if `debugRenderEnabled` is `true` by the default renderers. Custom renderers can choose to call it or not.
     * @param batch the batch
     */
//    open fun debugRender(batch: Batch) {
//    }

    internal open fun dirty(dirtyFlag: Int) {
        if ((hierarchyDirty and dirtyFlag) == 0) {
            hierarchyDirty = hierarchyDirty or dirtyFlag

            _children.forEach {
                it.dirty(dirtyFlag)
            }
            onTransformChanged()
        }
    }

    open fun onTransformChanged() {}

    /**
     * @return a tree string for all the child nodes under this [Node].
     */
    fun treeString(): String {
        val builder = StringBuilder()
        internalToTreeString(builder, "", "")
        return builder.toString()
    }

    private fun internalToTreeString(builder: StringBuilder, prefix: String, childrenPrefix: String) {
        builder.run {
            append(prefix)
            append(name)
            if (name != this@Node::class.simpleName) {
                append(" (${this@Node::class.simpleName})")
            }
            appendLine()
            children.forEachIndexed { index, node ->
                if (index < children.size - 1) {
                    node.internalToTreeString(builder, "$childrenPrefix├── ", "$childrenPrefix│   ")
                } else {
                    node.internalToTreeString(builder, "$childrenPrefix└── ", "$childrenPrefix    ")
                }
            }
        }
    }

    override fun compareTo(other: Node): Int {
        return id.compareTo(other.id)
    }
}

fun <T : Node> T.addTo(parent: Node): T {
    parent(parent)
    return this
}