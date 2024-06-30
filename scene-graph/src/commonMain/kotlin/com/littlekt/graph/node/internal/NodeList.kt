package com.littlekt.graph.node.internal

import com.littlekt.graph.node.Node
import com.littlekt.log.Logger
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachReverse
import com.littlekt.util.datastructure.fastForEachWithIndex
import kotlin.reflect.KClass

/**
 * A "list" that tracked added [Node]s and sorted [Node]s.
 *
 * @author Colton Daily
 * @date 1/1/2022
 */
class NodeList {

    /** The total elements in the list. */
    val size: Int
        get() = nodes.size

    @PublishedApi internal val nodes = mutableListOf<Node>()

    @PublishedApi internal val sortedNodes = mutableListOf<Node>()

    private var isNodeListUnsorted = false

    /** Allow custom sorting when updating internal node lists. */
    var sort: Comparator<Node>? = null
        set(value) {
            field = value
            isNodeListUnsorted = true
            sortedNodes.clear()
            if (field != null) {
                sortedNodes.addAll(nodes)
            }
        }

    private var frameCount = 0

    /** @return true if no elements exist in the list */
    fun isEmpty() = nodes.isEmpty()

    /** @return true if elements do exist in the list */
    fun isNotEmpty() = nodes.isNotEmpty()

    internal fun add(node: Node) {
        addAt(node, size)
    }

    internal fun addAt(node: Node, index: Int) {
        if (nodes.contains(node)) {
            logger.warn { "You are trying to add an node (${node.name}) that you already added." }
        } else {
            nodes.add(index, node)
            sort?.run { sortedNodes.add(node) }
            isNodeListUnsorted = true
        }
    }

    internal fun remove(node: Node) {
        if (!nodes.contains(node)) {
            logger.warn {
                "You are trying to remove an node (${node.name}) that you already removed."
            }
        } else {
            nodes.remove(node)
            sort?.run { sortedNodes.remove(node) }
        }
    }

    internal fun sendToTop(node: Node) {
        moveTo(node, 0)
    }

    internal fun sendToBottom(node: Node) {
        moveTo(node, size - 1)
    }

    internal fun swap(node: Node, node2: Node) {
        val idx1 = nodes.indexOf(node)
        val idx2 = nodes.indexOf(node2)
        nodes[idx1] = node2
        nodes[idx2] = node
        isNodeListUnsorted = true
    }

    internal fun moveTo(node: Node, index: Int): Boolean {
        if (nodes.contains(node)) {
            nodes.remove(node)
            nodes.add(index, node)
            isNodeListUnsorted = true
            return true
        }
        return false
    }

    operator fun get(idx: Int): Node {
        return nodes[idx]
    }

    internal fun removeAndDestroyAllNodes() {
        isNodeListUnsorted = false

        updateLists()

        while (nodes.isNotEmpty()) {
            nodes[0].destroy()
        }
        nodes.clear()
        sortedNodes.clear()
    }

    /** @return true if the given [node] exists the list. */
    fun contains(node: Node): Boolean {
        return nodes.contains(node)
    }

    /**
     * Iterate through the nodes in normal sorted order. To iterate the sorted list see
     * [forEachSorted].
     */
    inline fun forEach(action: (Node) -> Unit) {
        nodes.fastForEach(action)
    }

    /**
     * Iterate through the sorted list. Mainly used for custom render sorting while keep the
     * original update order. See [NodeList.sort].
     */
    inline fun forEachSorted(action: (Node) -> Unit) {
        sort?.let { sortedNodes.fastForEach(action) } ?: run { nodes.fastForEach(action) }
    }

    /** Iterate through the nodes in normal sorted order, with an index. */
    inline fun forEachIndexed(action: (index: Int, node: Node) -> Unit) {
        nodes.fastForEachWithIndex { index, value -> action(index, value) }
    }

    /** Iterate through the nodes in reverse-normal sorted order, with an index. */
    inline fun forEachReversed(action: (Node) -> Unit) {
        nodes.fastForEachReverse { action(it) }
    }

    internal fun preUpdate() {
        nodes.fastForEach {
            if (it.canUpdate) {
                it.propagatePreUpdate()
            }
        }
    }

    /** Should only be called once a frame. */
    internal fun update() {
        nodes.fastForEach {
            if (it.canUpdate) {
                it.propagateUpdate()
            }
        }
        frameCount++
    }

    internal fun postUpdate() {
        nodes.fastForEach {
            if (it.canUpdate) {
                it.propagatePostUpdate()
            }
        }
    }

    internal fun fixedUpdate() {
        nodes.fastForEach {
            if (it.canUpdate) {
                it.propagateFixedUpdate()
            }
        }
    }

    internal fun updateLists() {
        if (isNodeListUnsorted || sort != null) {
            nodes.sort()
            sort?.let { sortedNodes.sortWith(it) } ?: run { sortedNodes.sort() }
            isNodeListUnsorted = false
        }
    }

    /** @return a list containing all elements that are instances of specified type parameter [T] */
    inline fun <reified T : Node> filterIsInstance(): List<T> = nodes.filterIsInstance<T>()

    /** @return a the first node of the given type [T]. */
    inline fun <reified T : Node> findFirstNodeOfType(): T? = findFirstNodeOfType(T::class)

    /** @return a the first node of the given type [T]. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Node> findFirstNodeOfType(type: KClass<T>): T? {
        nodes.fastForEach {
            if (type.isInstance(it)) {
                return it as T
            }
            return it.nodes.findFirstNodeOfType(type)
        }
        return null
    }

    /** Search through the list to find a node with the given [name]. */
    fun findNode(name: String): Node? {
        nodes.fastForEach {
            if (it.name == name) {
                return it
            }
        }

        return null
    }

    /** @return a list of nodes of the given type [T]. */
    inline fun <reified T : Node> nodesOfType(): List<T> {
        val list = mutableListOf<T>()
        nodes.fastForEach {
            if (it is T) {
                list.add(it)
            }
        }

        return list
    }

    override fun toString(): String {
        return "[$nodes]"
    }

    private val Node.canUpdate: Boolean
        get() =
            enabled &&
                !isDestroyed &&
                updateInterval > 0 &&
                (updateInterval == 1 || frameCount % updateInterval == 0)

    companion object {
        private val logger = Logger<NodeList>()
    }
}
