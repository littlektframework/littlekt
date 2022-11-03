package com.lehaine.littlekt.graph.node.internal

import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.fastForEachReverse
import com.lehaine.littlekt.util.fastForEachWithIndex
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 1/1/2022
 */
class NodeList {

    val size get() = nodes.size

    @PublishedApi
    internal val nodes = mutableListOf<Node>()

    @PublishedApi
    internal val sortedNodes = mutableListOf<Node>()

    private var isNodeListUnsorted = false

    /**
     * Allow custom sorting when updating internal node lists.
     */
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

    internal fun add(node: Node) {
        addAt(node, size)
    }

    internal fun addAt(node: Node, index: Int) {
        if (nodes.contains(node)) {
            logger.warn { "You are trying to add an node (${node.name}) that you already added." }
        } else {
            nodes.add(index, node)
        }
    }

    internal fun remove(node: Node) {
        if (!nodes.contains(node)) {
            logger.warn { "You are trying to remove an node (${node.name}) that you already removed." }
        } else {
            nodes -= node
        }
    }

    internal fun removeAt(index: Int) {
        nodes.getOrNull(index)?.let(::remove)
        isNodeListUnsorted = true
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

    internal fun removeAllNodes() {
        isNodeListUnsorted = false

        updateLists()

        nodes.fastForEach {
            it.destroy()
        }
        nodes.clear()
        sortedNodes.clear()
    }

    fun contains(node: Node): Boolean {
        return nodes.contains(node)
    }

    /**
     * Iterate through the nodes in normal sorted order. To iterate the sorted list see [forEachSorted].
     */
    inline fun forEach(action: (Node) -> Unit) {
        nodes.fastForEach(action)
    }


    /**
     * Iterate through the sorted list. Mainly used for custom render sorting while keep the original update order. See [NodeList.sort].
     */
    inline fun forEachSorted(action: (Node) -> Unit) {
        sort?.let {
            sortedNodes.fastForEach(action)
        } ?: run {
            nodes.fastForEach(action)
        }
    }

    inline fun forEachIndexed(action: (index: Int, node: Node) -> Unit) {
        nodes.fastForEachWithIndex { index, value -> action(index, value) }
    }

    inline fun forEachReversed(action: (Node) -> Unit) {
        nodes.fastForEachReverse { action(it) }
    }

    internal fun preUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagatePreUpdate()
            }
        }
    }

    /**
     * Should only be called once a frame.
     */
    internal fun update() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagateUpdate()
            }
        }
        frameCount++
    }

    internal fun postUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagatePostUpdate()
            }
        }
    }

    internal fun fixedUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagateFixedUpdate()
            }
        }
    }

    internal fun updateLists() {
        if (isNodeListUnsorted || sort != null) {
            nodes.sort()
            sort?.let {
                sortedNodes.sortWith(it)
            } ?: run {
                sortedNodes.sort()
            }
            isNodeListUnsorted = false
        }
    }

    inline fun <reified T : Node> findFirstNodeOfType(): T? = findFirstNodeOfType(T::class)

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

    fun findNode(name: String): Node? {
        nodes.fastForEach {
            if (it.name == name) {
                return it
            }
        }

        return null
    }

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

    companion object {
        private val logger = Logger<NodeList>()
    }
}